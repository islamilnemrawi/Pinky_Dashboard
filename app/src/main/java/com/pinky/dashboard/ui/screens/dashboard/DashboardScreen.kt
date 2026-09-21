package com.pinky.dashboard.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinky.dashboard.core.util.Formatters
import com.pinky.dashboard.domain.model.*
import com.pinky.dashboard.ui.components.*
import com.pinky.dashboard.ui.navigation.DashboardSection
import com.pinky.dashboard.ui.theme.PinkPrimary
import java.util.Calendar

@Composable
fun DashboardScreen(
    orders: List<Order>,
    products: List<Product>,
    customers: List<Customer>,
    analytics: AnalyticsData,
    currentUser: AdminUser?,
    onNavigateToSection: (DashboardSection) -> Unit,
    onSelectOrder: (Order) -> Unit,
    onQuickAddSampleData: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Calculate Today's Stats dynamically
    val todayStart = remember(orders) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    val todayOrders = remember(orders, todayStart) {
        orders.filter { it.createdAt >= todayStart && it.status != OrderStatus.CANCELLED }
    }
    
    val todaySalesAmount = remember(todayOrders) {
        todayOrders.sumOf { it.totalAmount }
    }
    
    val todayOrdersCount = todayOrders.size
    val totalCustomersCount = customers.size
    
    val lowStockProducts = products.filter { it.stock <= 10 }
    val pendingOrders = orders.filter { it.status == OrderStatus.NEW }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting and Welcome Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "مرحباً بك، ${currentUser?.name ?: "مدير بينكي"}",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "إليك ملخص أداء متجر بينكي اليوم والمؤشرات الرئيسية.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 4 KPI Indicators Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PinkyStatCard(
                        title = "مبيعات اليوم",
                        value = Formatters.formatCurrencyEgp(todaySalesAmount),
                        icon = Icons.Outlined.Payments,
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_sales"
                    )

                    PinkyStatCard(
                        title = "طلبات اليوم",
                        value = "$todayOrdersCount أوردر",
                        icon = Icons.Outlined.ShoppingBag,
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_orders"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PinkyStatCard(
                        title = "إجمالي العملاء",
                        value = "$totalCustomersCount عميلة",
                        icon = Icons.Outlined.People,
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_customers"
                    )

                    if (currentUser?.canViewProfit == true) {
                        PinkyStatCard(
                            title = "صافي الربح",
                            value = Formatters.formatCurrencyEgp(analytics.totalProfit ?: 0.0),
                            icon = Icons.Outlined.TrendingUp,
                            iconTint = Color(0xFF34D399),
                            modifier = Modifier.weight(1f),
                            testTag = "kpi_profit"
                        )
                    } else {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "صافي الربح",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                ProtectedFieldPlaceholder("صافي الربح")
                            }
                        }
                    }
                }
            }
        }

        // Pinky Smart Insights (Compact & organized)
        if (lowStockProducts.isNotEmpty() || pendingOrders.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "تنبيهات ورؤى ذكية (Insights)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (lowStockProducts.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToSection(DashboardSection.PRODUCTS) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "مخزون منخفض: ${lowStockProducts.size} منتجات قاربت على النفاد",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${lowStockProducts.first().name} (متبقي ${lowStockProducts.first().stock} قطع)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text("عرض", color = PinkPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (pendingOrders.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToSection(DashboardSection.ORDERS) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "طلبات بانتظار التأكيد: ${pendingOrders.size} أوردر جديد",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text("تأكيد", color = PinkPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 4 Quick Actions Only
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "الإجراءات السريعة",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard("إضافة منتج", Icons.Outlined.AddBox, Modifier.weight(1f)) {
                        onNavigateToSection(DashboardSection.PRODUCTS)
                    }
                    QuickActionCard("إضافة عرض", Icons.Outlined.LocalOffer, Modifier.weight(1f)) {
                        onNavigateToSection(DashboardSection.OFFERS_COUPONS)
                    }
                    QuickActionCard("الطلبات", Icons.Outlined.ShoppingBag, Modifier.weight(1f)) {
                        onNavigateToSection(DashboardSection.ORDERS)
                    }
                    QuickActionCard("الشحن", Icons.Outlined.LocalShipping, Modifier.weight(1f)) {
                        onNavigateToSection(DashboardSection.SHIPPING)
                    }
                }
            }
        }

        // Recent Orders Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "أحدث الطلبات",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = { onNavigateToSection(DashboardSection.ORDERS) }) {
                    Text("كل الطلبات (${orders.size})", color = PinkPrimary, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // Recent Orders List (takes first 3 to 5)
        if (orders.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "لا توجد طلبات حقيقية",
                    message = "الطلبات الحقيقية التي تتم عبر متجر بينكي ستظهر هنا تلقائياً.",
                    icon = Icons.Outlined.ReceiptLong
                )
            }
        } else {
            items(orders.take(5), key = { it.id }) { order ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectOrder(order) }
                        .testTag("recent_order_item_${order.orderNumber}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "#${order.orderNumber}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PinkPrimary
                                )
                                Text(
                                    text = "• ${Formatters.formatRelativeTime(order.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OrderStatusBadge(status = order.status)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${order.customerName} - ${order.governorate}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = order.items.joinToString("، ") { "${it.productName} (${it.quantity})" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = order.paymentMethod.arabicLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = Formatters.formatCurrencyEgp(order.totalAmount),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(76.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
