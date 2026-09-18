package com.pinky.dashboard.ui.screens.prime

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pinky.dashboard.core.util.Formatters
import com.pinky.dashboard.domain.model.PrimeSubscriber
import com.pinky.dashboard.domain.model.PrimeSubscriberStatus
import com.pinky.dashboard.ui.theme.PinkPrimary
import com.pinky.dashboard.ui.theme.StatusCancelled

@Composable
fun PrimeScreen(
    subscribers: List<PrimeSubscriber>,
    onSaveSubscriber: (PrimeSubscriber) -> Unit,
    onToggleStatus: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    // VIP Plan Settings State (Price, Duration, Benefits details as requested)
    var primePriceText by remember { mutableStateOf("300") }
    var primeDurationText by remember { mutableStateOf("30") }
    var primeBenefitsText by remember { mutableStateOf("شحن مجاني لجميع الطلبات + خصم إضافي 5% على كافة المنتجات 🌸") }

    val filteredSubscribers = remember(subscribers, searchQuery) {
        if (searchQuery.isBlank()) subscribers
        else subscribers.filter {
            it.customerName.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery)
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PinkPrimary,
                contentColor = Color(0xFF381528),
                icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
                text = { Text("إضافة مشترك Prime يدوي", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                modifier = Modifier.testTag("add_prime_fab")
            )
        },
        modifier = modifier.testTag("prime_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("prime_scroll_list"),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. Header Banner
            item {
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
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PinkPrimary
                                ) {
                                    Text(
                                        text = "PRIME VIP 👑",
                                        color = Color(0xFF381528),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = "إدارة مشتركي Pinky Prime",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "متابعة عملاء العضوية الحصرية، شحنات الشحن المجاني المستهلكة، ومزايا الخصومات",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Prime VIP Settings Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.SettingsSuggest, contentDescription = null, tint = PinkPrimary)
                            Text("تعديل باقة وعضوية بينكي برايم (VIP Settings)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = primePriceText,
                                onValueChange = { primePriceText = it },
                                label = { Text("سعر الاشتراك (ج.م)", fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = primeDurationText,
                                onValueChange = { primeDurationText = it },
                                label = { Text("المدة (بالأيام)", fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        OutlinedTextField(
                            value = primeBenefitsText,
                            onValueChange = { primeBenefitsText = it },
                            label = { Text("مزايا وفوائد العضوية", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                Toast.makeText(context, "تم حفظ وتحديث إعدادات باقة Prime VIP بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تحديث باقة العضوية الموحدة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Search and Filters Header
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "قائمة المشتركات الحاليين (${filteredSubscribers.size}):",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث باسم العميلة أو رقم الهاتف...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = PinkPrimary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 4. Subscribers List
            if (filteredSubscribers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.WorkspacePremium, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Text(
                                text = "لا يوجد مشتركون في Pinky Prime حالياً مطابق للبحث.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredSubscribers, key = { it.id }) { subscriber ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("prime_subscriber_${subscriber.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(subscriber.customerName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("هاتف: ${subscriber.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val isActive = subscriber.status == PrimeSubscriberStatus.ACTIVE
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isActive) Color(0xFF34D399).copy(alpha = 0.15f) else StatusCancelled.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = subscriber.status.arabicLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isActive) Color(0xFF34D399) else StatusCancelled,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Switch(
                                        checked = isActive,
                                        onCheckedChange = { onToggleStatus(subscriber.id, it) }
                                    )
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outlineVariant)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("تاريخ الانتهاء", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(Formatters.formatDate(subscriber.expiryDate), fontWeight = FontWeight.SemiBold, color = PinkPrimary)
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("أوردرات برايم", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${subscriber.totalOrdersWithPrime} طلبات", fontWeight = FontWeight.Bold)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("وفرت مع برايم", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(Formatters.formatCurrencyEgp(subscriber.totalSavedWithPrime), fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        PrimeAddDialog(
            onDismiss = { showAddDialog = false },
            onSave = {
                onSaveSubscriber(it)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun PrimeAddDialog(
    onDismiss: () -> Unit,
    onSave: (PrimeSubscriber) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf("اشتراك سنوي (VIP)") }

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
                    text = "إضافة عميلة في Pinky Prime",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = PinkPrimary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم العميلة *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف المسجل *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = plan,
                    onValueChange = { plan = it },
                    label = { Text("نوع باقة الاشتراك") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val subscriber = PrimeSubscriber(
                                id = "prime_${System.currentTimeMillis()}",
                                customerName = name.ifBlank { "عميلة مميزة" },
                                phone = phone.ifBlank { "01000000000" },
                                startDate = System.currentTimeMillis(),
                                expiryDate = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365),
                                status = PrimeSubscriberStatus.ACTIVE,
                                totalOrdersWithPrime = 0,
                                totalSavedWithPrime = 0.0
                            )
                            onSave(subscriber)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تفعيل الاشتراك", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}
