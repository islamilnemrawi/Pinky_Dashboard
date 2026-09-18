package com.pinky.dashboard.ui.screens.payments

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
import androidx.compose.ui.window.Dialog
import com.pinky.dashboard.domain.model.PaymentMethodConfig
import com.pinky.dashboard.ui.theme.PinkPrimary

@Composable
fun PaymentsScreen(
    paymentMethods: List<PaymentMethodConfig>,
    onSavePaymentMethod: (PaymentMethodConfig) -> Unit,
    onToggleActive: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingMethod by remember { mutableStateOf<PaymentMethodConfig?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("payments_screen")
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
                            text = "إعدادات وتفعيل طرق الدفع",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "التحكم في وسائل السداد المتاحة لعملاء متجر بينكي وتفاصيل المحافظ والحسابات البنكية",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(paymentMethods, key = { it.key }) { config ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_method_${config.key}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PinkPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (config.key) {
                                            "cod" -> Icons.Outlined.LocalAtm
                                            "vodafone_cash" -> Icons.Outlined.PhoneAndroid
                                            else -> Icons.Outlined.CreditCard
                                        },
                                        contentDescription = null,
                                        tint = PinkPrimary
                                    )
                                }

                                Column {
                                    Text(
                                        text = config.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (config.isEnabled) "مفعلة للمشتريات" else "معطلة حالياً",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (config.isEnabled) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Switch(
                                    checked = config.isEnabled,
                                    onCheckedChange = { onToggleActive(config.key, it) }
                                )

                                IconButton(onClick = { editingMethod = config }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        Text(
                            text = config.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (config.feeOrDiscountText.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PinkPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = config.feeOrDiscountText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PinkPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingMethod?.let { config ->
        PaymentMethodEditDialog(
            config = config,
            onDismiss = { editingMethod = null },
            onSave = { updated ->
                onSavePaymentMethod(updated)
                editingMethod = null
            }
        )
    }
}

@Composable
fun PaymentMethodEditDialog(
    config: PaymentMethodConfig,
    onDismiss: () -> Unit,
    onSave: (PaymentMethodConfig) -> Unit
) {
    var name by remember { mutableStateOf(config.name) }
    var description by remember { mutableStateOf(config.description) }
    var feeOrDiscount by remember { mutableStateOf(config.feeOrDiscountText) }
    var isEnabled by remember { mutableStateOf(config.isEnabled) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "تعديل وسيلة الدفع: ${config.name}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = PinkPrimary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الظاهر في المتجر") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("بيانات وتفاصيل التحويل / الحساب للعميل") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = feeOrDiscount,
                    onValueChange = { feeOrDiscount = it },
                    label = { Text("نص الخصم أو الرسوم الإضافية") },
                    placeholder = { Text("مثال: خصم 5% عند الدفع بإنستاباي") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تفعيل وسيلة الدفع في المتجر", fontWeight = FontWeight.SemiBold)
                    Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onSave(
                                config.copy(
                                    name = name,
                                    description = description,
                                    feeOrDiscountText = feeOrDiscount,
                                    isEnabled = isEnabled
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("حفظ التعديلات", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}
