package com.pinky.dashboard.ui.screens.orders

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pinky.dashboard.core.util.Formatters
import com.pinky.dashboard.domain.model.Order
import com.pinky.dashboard.domain.model.OrderStatus
import com.pinky.dashboard.ui.components.OrderStatusBadge
import com.pinky.dashboard.ui.theme.PinkPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailDialog(
    order: Order,
    onDismiss: () -> Unit,
    onStatusChange: (OrderStatus) -> Unit
) {
    val context = LocalContext.current
    var selectedStatus by remember { mutableStateOf(order.status) }
    var noteText by remember { mutableStateOf(order.notes) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("order_detail_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "تفاصيل الطلب #${order.orderNumber}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = PinkPrimary
                        )
                        Text(
                            text = Formatters.formatOrderDate(order.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_order_detail_btn")) {
                        Icon(Icons.Outlined.Close, contentDescription = "إغلاق")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Status change section
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "حالة الطلب الحالية:",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    OrderStatusBadge(status = order.status)
                                }

                                Text(
                                    text = "تغيير الحالة للمزامنة مع موقع بينكي والعميل:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                var dropdownExpanded by remember { mutableStateOf(false) }

                                ExposedDropdownMenuBox(
                                    expanded = dropdownExpanded,
                                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedStatus.arabicLabel,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                            .testTag("status_selector_field"),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false }
                                    ) {
                                        OrderStatus.entries.forEach { status ->
                                            DropdownMenuItem(
                                                text = { Text(status.arabicLabel) },
                                                onClick = {
                                                    selectedStatus = status
                                                    dropdownExpanded = false
                                                    onStatusChange(status)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Customer Information & Quick Contact
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "بيانات العميل والشحن",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(order.customerName, fontWeight = FontWeight.Bold)
                                        Text(order.customerPhone, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${order.governorate} - ${order.customerAddress}", style = MaterialTheme.typography.bodySmall)
                                        Text("مركز الشحن: ${order.shippingCenter}", style = MaterialTheme.typography.bodySmall, color = PinkPrimary)
                                    }

                                    // Direct Contact Actions
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Call Button
                                        IconButton(
                                            onClick = {
                                                val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = Uri.parse("tel:${order.customerPhone}")
                                                }
                                                context.startActivity(callIntent)
                                            },
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(PinkPrimary.copy(alpha = 0.15f))
                                                .testTag("call_customer_btn")
                                        ) {
                                            Icon(Icons.Outlined.Phone, contentDescription = "اتصال بالعميل", tint = PinkPrimary)
                                        }

                                        // WhatsApp Button
                                        IconButton(
                                            onClick = {
                                                val cleanPhone = order.customerPhone.replace("+", "").replace(" ", "")
                                                val formatted = if (cleanPhone.startsWith("01")) "2$cleanPhone" else cleanPhone
                                                val waIntent = Intent(Intent.ACTION_VIEW).apply {
                                                    data = Uri.parse("https://api.whatsapp.com/send?phone=$formatted&text=مرحباً ${order.customerName}، بخصوص طلبك رقم #${order.orderNumber} من متجر بينكي")
                                                }
                                                context.startActivity(waIntent)
                                            },
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF25D366).copy(alpha = 0.15f))
                                                .testTag("whatsapp_customer_btn")
                                        ) {
                                            Icon(Icons.Outlined.Chat, contentDescription = "واتساب", tint = Color(0xFF25D366))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Order Items Table (Multiple products in the same order)
                    item {
                        Text(
                            text = "منتجات الطلب (${order.items.size} أصناف)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    items(order.items, key = { it.id }) { item ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.productName, fontWeight = FontWeight.Bold)
                                    if (item.variantName != null || item.color != null || item.size != null) {
                                        Text(
                                            text = listOfNotNull(item.variantName, item.color, item.size).joinToString(" • "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${item.quantity} قطعة × ${Formatters.formatCurrencyEgp(item.unitPrice)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PinkPrimary
                                    )
                                }

                                Text(
                                    text = Formatters.formatCurrencyEgp(item.lineTotal),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Financial Summary
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("ملخص الحساب", fontWeight = FontWeight.Bold)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("المجموع الفرعي:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(Formatters.formatCurrencyEgp(order.subtotalAmount))
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("تكلفة الشحن (${order.governorate}):", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(Formatters.formatCurrencyEgp(order.shippingFee))
                                }

                                if (order.discountAmount > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("خصم كوبون / عرض:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("- ${Formatters.formatCurrencyEgp(order.discountAmount)}", color = Color(0xFF34D399))
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("الإجمالي الكلي المستحق:", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = Formatters.formatCurrencyEgp(order.totalAmount),
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PinkPrimary
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("طريقة الدفع:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${order.paymentMethod.arabicLabel} (${order.paymentStatus.arabicLabel})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Internal Notes
                    item {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("order_notes_input"),
                            label = { Text("ملاحظات داخلية لفريق العمل") },
                            placeholder = { Text("مثال: العميل طلب التوصيل بعد الساعة 5 مساءً...") },
                            shape = RoundedCornerShape(10.dp),
                            minLines = 2
                        )
                    }
                }

                // Footer Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onStatusChange(selectedStatus)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_order_status_btn")
                    ) {
                        Text("حفظ التغييرات والمزامنة", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("dismiss_order_btn")
                    ) {
                        Text("إغلاق")
                    }
                }
            }
        }
    }
}
