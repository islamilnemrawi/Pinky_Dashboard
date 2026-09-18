package com.pinky.dashboard.ui.screens.reports

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
import com.pinky.dashboard.domain.model.*
import com.pinky.dashboard.ui.components.PinkyStatCard
import com.pinky.dashboard.ui.theme.PinkPrimary

@Composable
fun ReportsScreen(
    orders: List<Order>,
    products: List<Product>,
    customers: List<Customer>,
    analytics: AnalyticsData,
    currentUser: AdminUser?,
    modifier: Modifier = Modifier
) {
    var selectedReportType by remember { mutableIntStateOf(0) } // 0 = Financial/Profit, 1 = Products/Inventory, 2 = Shipping
    var showExportToast by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = {
            if (showExportToast) {
                Snackbar(
                    action = { TextButton(onClick = { showExportToast = false }) { Text("إغلاق", color = PinkPrimary) } },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("جاري إنشاء وتحميل تقرير المتجر بصيغة CSV و PDF...")
                }
            }
        },
        modifier = modifier.testTag("reports_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(PinkPrimary)
                            )
                            Text(
                                text = "التقارير المالية والتشغيلية",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تحليلات الأرباح، أداء الشحن، حركة المخزون، وحساب هوامش الربح التقديرية",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = { showExportToast = true },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = PinkPrimary.copy(alpha = 0.15f), contentColor = PinkPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير CSV / PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Report Sub-Tabs
            TabRow(
                selectedTabIndex = selectedReportType,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PinkPrimary
            ) {
                Tab(
                    selected = selectedReportType == 0,
                    onClick = { selectedReportType = 0 },
                    text = { Text("المبيعات والأرباح", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Outlined.TrendingUp, contentDescription = null) }
                )
                Tab(
                    selected = selectedReportType == 1,
                    onClick = { selectedReportType = 1 },
                    text = { Text("أداء المنتجات والمخزون", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Outlined.Inventory2, contentDescription = null) }
                )
                Tab(
                    selected = selectedReportType == 2,
                    onClick = { selectedReportType = 2 },
                    text = { Text("كفاءة الشحن والتوصيل", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Outlined.LocalShipping, contentDescription = null) }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedReportType == 0) {
                    // Financial & Profit Report
                    if (currentUser?.canViewProfit == true) {
                        item {
                            val totalProfit = analytics.totalProfit ?: 0.0
                            val margin = if (analytics.totalSales > 0) (totalProfit / analytics.totalSales * 100).toInt() else 0

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                PinkyStatCard(
                                    title = "صافي الأرباح المقدرة",
                                    value = Formatters.formatCurrencyEgp(totalProfit),
                                    subtitle = "المعادلة: (المبيعات - تكلفة الجملة)",
                                    icon = Icons.Outlined.MonetizationOn,
                                    iconTint = Color(0xFF34D399),
                                    modifier = Modifier.weight(1f)
                                )

                                PinkyStatCard(
                                    title = "متوسط هامش الربح",
                                    value = "$margin%",
                                    subtitle = "من إجمالي حجم المبيعات",
                                    icon = Icons.Outlined.Percent,
                                    iconTint = PinkPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("تفاصيل ربحية الطلبات الأخيرة", fontWeight = FontWeight.Bold)
                                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                                    orders.take(5).forEach { order ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("#${order.orderNumber} - ${order.customerName}", fontWeight = FontWeight.SemiBold)
                                                Text(order.governorate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("القيمة: ${Formatters.formatCurrencyEgp(order.totalAmount)}", fontWeight = FontWeight.Bold)
                                                Text("الربح المقدر: ${Formatters.formatCurrencyEgp(order.totalAmount * 0.38)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF34D399))
                                            }
                                        }
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(48.dp))
                                    Text("تقارير الأرباح وهوامش التكلفة محجوبة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        text = "حسابات الأرباح محصورة لحساب المالك (Owner) والمدير المالي المصرح له.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedReportType == 1) {
                    // Products & Inventory Report
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("ملخص حركة المخزون بالأصناف", fontWeight = FontWeight.Bold)

                                products.forEach { p ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(p.name, fontWeight = FontWeight.SemiBold)
                                            Text("القسم: ${p.categoryName} • السعر: ${Formatters.formatCurrencyEgp(p.price)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (p.stock > 10) Color(0xFF34D399).copy(alpha = 0.15f) else PinkPrimary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "${p.stock} قطعة",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (p.stock > 10) Color(0xFF34D399) else PinkPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Shipping Performance Report
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("أداء شركات الشحن ومعدل التسليم", fontWeight = FontWeight.Bold)

                                val shippedOrders = orders.filter { it.status == OrderStatus.SHIPPED || it.status == OrderStatus.DELIVERED }
                                val deliveredOrders = orders.filter { it.status == OrderStatus.DELIVERED }
                                val deliveryRate = if (orders.isNotEmpty()) (deliveredOrders.size * 100 / orders.size) else 100

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("نسبة التسليم الناجح (Delivery Success Rate):")
                                    Text("$deliveryRate%", fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("إجمالي الشحنات الخارجة:")
                                    Text("${shippedOrders.size} شحنة", fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("متوسط زمن التسليم بالمحافظات:")
                                    Text("2 - 3 أيام عمل", fontWeight = FontWeight.Bold, color = PinkPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
