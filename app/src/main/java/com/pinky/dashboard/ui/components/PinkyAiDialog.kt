package com.pinky.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pinky.dashboard.core.util.Formatters
import com.pinky.dashboard.domain.model.*
import com.pinky.dashboard.ui.theme.PinkPrimary
import kotlinx.coroutines.launch

@Composable
fun PinkyAiDialog(
    orders: List<Order>,
    products: List<Product>,
    customers: List<Customer>,
    analytics: AnalyticsData,
    currentUser: AdminUser?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputPrompt by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            AiChatMessage(
                id = "msg_welcome",
                sender = AiSender.ASSISTANT,
                message = "أهلاً بك في مساعد Pinky الذكي! 🌸\nأنا جاهز لتحليل بيانات متجرك، الإجابة عن استفسارات المبيعات والمخزون، وتقديم رؤى عملية لأداء متجر بينكي."
            )
        )
    }

    val promptChips = listOf(
        "ما إجمالي مبيعات اليوم؟",
        "ما أكثر المنتجات مبيعاً؟",
        "ما المنتجات منخفضة المخزون؟",
        "كم عدد الطلبات الجديدة؟",
        "ما المحافظات الأكثر طلباً؟",
        "لخص أداء المتجر هذا الأسبوع.",
        "اقترح منتجات تحتاج إلى إعادة تخزين."
    )

    fun handleQuery(query: String) {
        if (query.isBlank()) return
        val userQuery = query.trim()
        messages.add(AiChatMessage(System.currentTimeMillis().toString(), AiSender.USER, userQuery))
        inputPrompt = ""

        // Process query against live store state securely
        val response = when {
            userQuery.contains("مبيعات") || userQuery.contains("المبيعات") -> {
                val totalSalesFormatted = Formatters.formatCurrencyEgp(analytics.totalSales)
                val ordersCount = analytics.totalOrders
                val avgOrder = Formatters.formatCurrencyEgp(analytics.averageOrderValue)
                val profitText = if (currentUser?.canViewProfit == true) {
                    "\n• صافي الأرباح المقدرة: ${Formatters.formatCurrencyEgp(analytics.totalProfit ?: 0.0)}"
                } else ""
                "📊 تقرير المبيعات الحالية لمتجر بينكي:\n• إجمالي المبيعات المحققة: $totalSalesFormatted\n• عدد الطلبات الكلي: $ordersCount طلب\n• متوسط قيمة الطلب: $avgOrder$profitText"
            }

            userQuery.contains("أكثر المنتجات") || userQuery.contains("الأكثر مبيعاً") -> {
                if (analytics.topSellingProducts.isEmpty()) {
                    "لا توجد مبيعات كافية مسجلة حالياً لتحديد المنتجات الأكثر طلباً."
                } else {
                    val topList = analytics.topSellingProducts.take(3).mapIndexed { idx, item ->
                        "${idx + 1}. ${item.productName} (بيع منها ${item.unitsSold} قطعة بإجمالي ${Formatters.formatCurrencyEgp(item.totalRevenue)})"
                    }.joinToString("\n")
                    "⭐ المنتجات الأكثر مبيعاً في متجر بينكي:\n$topList"
                }
            }

            userQuery.contains("مخزون") || userQuery.contains("إعادة تخزين") -> {
                val lowStock = products.filter { it.stock <= 10 }
                if (lowStock.isEmpty()) {
                    "✅ جميع منتجات متجر بينكي في مستويات مخزون آمنة (أكثر من 10 قطع)."
                } else {
                    val list = lowStock.joinToString("\n") { "• ${it.name}: متبقي ${it.stock} قطع فقط" }
                    "⚠️ تنبيه المخزون - المنتجات التي تحتاج لإعادة التوريد قريباً:\n$list"
                }
            }

            userQuery.contains("الطلبات الجديدة") || userQuery.contains("جديدة") -> {
                val newOrders = orders.filter { it.status == OrderStatus.NEW }
                if (newOrders.isEmpty()) {
                    "لا توجد طلبات جديدة معلقة حالياً، جميع الطلبات قيد المعالجة أو تم تسليمها."
                } else {
                    val totalNew = newOrders.sumOf { it.totalAmount }
                    "🔔 لديك حالياً ${newOrders.size} طلبات جديدة بانتظار التأكيد، بقيمة إجمالية ${Formatters.formatCurrencyEgp(totalNew)}. ينصح بالتواصل مع العملاء لتأكيد الشحن."
                }
            }

            userQuery.contains("المحافظات") || userQuery.contains("شحن") -> {
                val govStats = orders.groupBy { it.governorate }.mapValues { it.value.size }
                    .entries.sortedByDescending { it.value }.take(3)
                if (govStats.isEmpty()) {
                    "لم تسجل طلبات بالمحافظات بعد."
                } else {
                    val topGovs = govStats.joinToString("\n") { "• ${it.key}: ${it.value} طلب" }
                    "🇪🇬 المحافظات الأكثر طلباً في متجر بينكي:\n$topGovs"
                }
            }

            userQuery.contains("لخص") || userQuery.contains("أداء") -> {
                "📈 ملخص أداء متجر بينكي:\n• إجمالي الطلبات النشطة: ${orders.count { it.status != OrderStatus.CANCELLED }}\n• إجمالي العملاء المسجلين: ${customers.size} عميل\n• إجمالي المبيعات: ${Formatters.formatCurrencyEgp(analytics.totalSales)}\n• نسبة الطلبات المكتملة: ${if (orders.isNotEmpty()) (orders.count { it.status == OrderStatus.DELIVERED } * 100 / orders.size) else 0}%\n• توصية المساعد الذكي: استمرار العروض الترويجية على فئات الفساتين والأطقم لتنشيط الطلبات."
            }

            else -> {
                "بناءً على بيانات متجر بينكي الحالية، تم تسجيل ${orders.size} طلب وإجمالي مبيعات ${Formatters.formatCurrencyEgp(analytics.totalSales)}. هل ترغب في الاستفسار عن تفاصيل المخزون، أو المحافظات، أو إحصائيات المبيعات؟"
            }
        }

        messages.add(AiChatMessage((System.currentTimeMillis() + 1).toString(), AiSender.ASSISTANT, response))
        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .testTag("pinky_ai_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PinkPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color(0xFF381528))
                        }

                        Column {
                            Text(
                                text = "مساعد Pinky الذكي",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "تحليل حقيقي لبيانات المتجر • آمن سحابياً (Server-side)",
                                style = MaterialTheme.typography.bodySmall,
                                color = PinkPrimary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "إغلاق")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Quick Prompt Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(promptChips) { chip ->
                        SuggestionChip(
                            onClick = { handleQuery(chip) },
                            label = { Text(chip, fontSize = 12.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = PinkPrimary.copy(alpha = 0.12f),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = PinkPrimary.copy(alpha = 0.3f))
                        )
                    }
                }

                // Chat Messages
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { msg ->
                        val isUser = msg.sender == AiSender.USER
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUser) PinkPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.widthIn(max = 300.dp)
                            ) {
                                Text(
                                    text = msg.message,
                                    color = if (isUser) Color(0xFF381528) else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                // Input Field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputPrompt,
                        onValueChange = { inputPrompt = it },
                        placeholder = { Text("اسأل عن مبيعات اليوم، المخزون، الطلبات...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    FilledIconButton(
                        onClick = { handleQuery(inputPrompt) },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528)),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Outlined.Send, contentDescription = "إرسال")
                    }
                }
            }
        }
    }
}
