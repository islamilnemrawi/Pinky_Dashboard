package com.pinky.dashboard.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinky.dashboard.core.util.Formatters
import com.pinky.dashboard.domain.model.AdminUser
import com.pinky.dashboard.domain.model.AnalyticsData
import com.pinky.dashboard.domain.model.OrderStatus
import com.pinky.dashboard.ui.components.OrderStatusBadge
import com.pinky.dashboard.ui.components.PinkyStatCard
import com.pinky.dashboard.ui.components.ProtectedFieldPlaceholder
import com.pinky.dashboard.ui.components.SalesBarChartCard
import com.pinky.dashboard.ui.theme.PinkPrimary

@Composable
fun AnalyticsScreen(
    analytics: AnalyticsData,
    currentUser: AdminUser?,
    selectedTimeframeDays: Int,
    onTimeframeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeframes = listOf(
        1 to "اليوم",
        7 to "آخر 7 أيام",
        30 to "آخر شهر",
        90 to "آخر 3 أشهر"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Timeframe selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    timeframes.forEach { (days, label) ->
                        val isSelected = selectedTimeframeDays == days
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTimeframeChange(days) },
                            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PinkPrimary,
                                selectedLabelColor = Color(0xFF381528)
                            )
                        )
                    }
                }
            }
        }

        // Summary KPI Cards
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PinkyStatCard(
                    title = "إجمالي المبيعات",
                    value = Formatters.formatCurrencyEgp(analytics.totalSales),
                    subtitle = "متوسط الطلب: ${Formatters.formatCurrencyEgp(analytics.averageOrderValue)}",
                    icon = Icons.Outlined.Payments,
                    modifier = Modifier.weight(1f),
                    testTag = "analytics_total_sales"
                )

                PinkyStatCard(
                    title = "إجمالي الطلبات",
                    value = "${analytics.totalOrders}",
                    subtitle = "المنتجات المباعة: ${analytics.totalProductsSold}",
                    icon = Icons.Outlined.ShoppingBag,
                    modifier = Modifier.weight(1f),
                    testTag = "analytics_total_orders"
                )
            }
        }

        // Profit Metric (Protected)
        item {
            if (currentUser?.canViewProfit == true) {
                PinkyStatCard(
                    title = "صافي الأرباح المقدرة للفترة",
                    value = Formatters.formatCurrencyEgp(analytics.totalProfit ?: 0.0),
                    subtitle = "محسوب بالمعادلة: (سعر البيع - سعر التكلفة بالجملة)",
                    icon = Icons.Outlined.TrendingUp,
                    iconTint = Color(0xFF34D399),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProtectedFieldPlaceholder(fieldName = "أرباح المتجر")
                        Text(
                            text = "صلاحية استعراض الأرباح مقتصرة على المالك والمدير.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Sales Bar Chart
        item {
            SalesBarChartCard(
                metrics = analytics.dailyMetrics,
                title = "توزيع المبيعات خلال الفترة المحددة",
                subtitle = "مستخرجة من قيم الطلبات المسجلة فعلياً"
            )
        }

        // Top Selling Products
        item {
            Text(
                text = "المنتجات الأكثر مبيعاً ⭐",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (analytics.topSellingProducts.isEmpty()) {
            item {
                Text(
                    text = "لا توجد مبيعات مسجلة في هذه الفترة لتحديد المنتجات الأكثر طلباً.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(analytics.topSellingProducts) { top ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(top.productName, fontWeight = FontWeight.Bold)
                            Text(
                                text = "تم بيع: ${top.unitsSold} قطعة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = Formatters.formatCurrencyEgp(top.totalRevenue),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PinkPrimary
                        )
                    }
                }
            }
        }

        // Orders by Status Breakdown
        item {
            Text(
                text = "توزيع الطلبات حسب الحالة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OrderStatus.entries.forEach { status ->
                        val count = analytics.ordersByStatus[status] ?: 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OrderStatusBadge(status = status)
                            Text(
                                text = "$count طلب",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        }

        // Sales by Governorate Breakdown
        item {
            Text(
                text = "توزيع المبيعات حسب المحافظات المصرية 🇪🇬",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (analytics.salesByGovernorate.isEmpty()) {
                        Text(
                            text = "لا توجد بيانات كافية للمحافظات في هذه الفترة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        analytics.salesByGovernorate.entries.sortedByDescending { it.value }.forEach { (gov, sales) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(gov, fontWeight = FontWeight.Medium)
                                Text(
                                    text = Formatters.formatCurrencyEgp(sales),
                                    fontWeight = FontWeight.Bold,
                                    color = PinkPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
