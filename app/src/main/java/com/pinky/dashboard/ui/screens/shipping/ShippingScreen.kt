package com.pinky.dashboard.ui.screens.shipping

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pinky.dashboard.core.util.Formatters
import com.pinky.dashboard.domain.model.ShippingCenter
import com.pinky.dashboard.domain.model.ShippingGovernorate
import com.pinky.dashboard.ui.theme.PinkPrimary
import com.pinky.dashboard.ui.theme.StatusCancelled

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ShippingScreen(
    governorates: List<ShippingGovernorate>,
    shippingCenters: List<ShippingCenter>,
    onSaveGovernorate: (ShippingGovernorate) -> Unit,
    onSaveCenter: (ShippingCenter) -> Unit,
    onDeleteCenter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedGov by remember { mutableStateOf<ShippingGovernorate?>(null) }
    var editingGov by remember { mutableStateOf<ShippingGovernorate?>(null) }
    var editingCenter by remember { mutableStateOf<ShippingCenter?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Ensure the selectedGov remains synced when external list updates
    val activeGov = selectedGov?.let { current -> governorates.find { it.id == current.id } ?: current }

    AnimatedContent(
        targetState = activeGov,
        label = "shipping_navigation"
    ) { currentGov ->
        if (currentGov == null) {
            // Screen 1: List of all 27 Governorates
            Scaffold(
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = {
                            editingGov = ShippingGovernorate(
                                id = "gov_${System.currentTimeMillis()}",
                                name = "",
                                deliveryPrice = 65.0,
                                estimatedDays = "2-3 أيام",
                                isActive = true
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        icon = { Icon(Icons.Outlined.AddLocationAlt, contentDescription = null, modifier = Modifier.size(24.dp)) },
                        text = { Text("إضافة محافظة جديدة", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                        modifier = Modifier.testTag("add_shipping_fab")
                    )
                },
                modifier = modifier.testTag("shipping_screen")
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Header Information
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "إعدادات تسعير الشحن والتوصيل",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "تعديل وإضافة أسعار شحن المحافظات ومندوبين التوصيل لكل المراكز الفرعية التابعة لها.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Search Input
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ابحث عن محافظة...", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            )
                        )
                    }

                    val filteredGovs = governorates.filter {
                        it.name.contains(searchQuery, ignoreCase = true)
                    }

                    // Governorates List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("governorates_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredGovs, key = { it.id }) { gov ->
                            val govCenters = shippingCenters.filter { it.governorateId == gov.id }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedGov = gov }
                                    .testTag("gov_card_${gov.id}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(18.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = gov.name,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            // Badge showing centers count
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = "${govCenters.size} مراكز",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }

                                            if (!gov.isActive) {
                                                Surface(shape = RoundedCornerShape(4.dp), color = StatusCancelled.copy(alpha = 0.15f)) {
                                                    Text(
                                                        text = "موقف مؤقتاً",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = StatusCancelled,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "مدة الشحن: ${gov.estimatedDays} • السعر العام: ${Formatters.formatCurrencyEgp(gov.deliveryPrice)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = { editingGov = gov },
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Edit,
                                                contentDescription = "تعديل المحافظة",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Outlined.ArrowForwardIos,
                                            contentDescription = "عرض تفاصيل المراكز",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Screen 2: Dedicated independent Governorate Details & Centers management
            val govCenters = shippingCenters.filter { it.governorateId == currentGov.id }
            var bulkPriceText by remember { mutableStateOf("") }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "محافظة ${currentGov.name}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { selectedGov = null }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "رجوع للمحافظات",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { editingGov = currentGov }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "تعديل بيانات المحافظة", modifier = Modifier.size(24.dp))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = {
                            editingCenter = ShippingCenter(
                                id = "cnt_${System.currentTimeMillis()}",
                                governorateId = currentGov.id,
                                name = "",
                                deliveryPrice = currentGov.deliveryPrice,
                                contactNumber = "",
                                isActive = true
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        icon = { Icon(Icons.Outlined.AddLocation, contentDescription = null, modifier = Modifier.size(24.dp)) },
                        text = { Text("إضافة مركز جديد", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Governorate summary block & Bulk price tool
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "التحكم في أسعار شحن مراكز ${currentGov.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "السعر العام: ${Formatters.formatCurrencyEgp(currentGov.deliveryPrice)}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(14.dp))

                            // Bulk Price Application
                            Text(
                                text = "تطبيق سعر شحن موحد على جميع مراكز المحافظة:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = bulkPriceText,
                                    onValueChange = { bulkPriceText = it },
                                    placeholder = { Text("أدخل السعر الجديد...", fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                Button(
                                    onClick = {
                                        val price = bulkPriceText.toDoubleOrNull()
                                        if (price != null) {
                                            govCenters.forEach { center ->
                                                onSaveCenter(center.copy(deliveryPrice = price))
                                            }
                                            bulkPriceText = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("تطبيق السعر", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Centers List
                    Text(
                        text = "المراكز المسجلة (${govCenters.size}):",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )

                    if (govCenters.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.LocationOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Text(
                                    text = "لا توجد مراكز مضافة لهذه المحافظة.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "سيتم تطبيق السعر العام تلقائياً للعميلات.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(govCenters, key = { it.id }) { center ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = center.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )

                                                if (!center.isActive) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = StatusCancelled.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = "غير نشط",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = StatusCancelled,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            if (!center.contactNumber.isNullOrEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "المندوب/رقم الشحنة: ${center.contactNumber}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = Formatters.formatCurrencyEgp(center.deliveryPrice),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 15.sp
                                            )

                                            // Toggle Active state directly
                                            Switch(
                                                checked = center.isActive,
                                                onCheckedChange = { isChecked ->
                                                    onSaveCenter(center.copy(isActive = isChecked))
                                                },
                                                modifier = Modifier.scale(0.8f)
                                            )

                                            IconButton(
                                                onClick = { editingCenter = center },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Edit,
                                                    contentDescription = "تعديل",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { onDeleteCenter(center.id) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = "حذف",
                                                    tint = StatusCancelled,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingGov?.let { gov ->
        GovernorateEditDialog(
            gov = gov,
            onDismiss = { editingGov = null },
            onSave = {
                onSaveGovernorate(it)
                editingGov = null
            }
        )
    }

    editingCenter?.let { center ->
        CenterEditDialog(
            center = center,
            onDismiss = { editingCenter = null },
            onSave = {
                onSaveCenter(it)
                editingCenter = null
            }
        )
    }
}

@Composable
fun GovernorateEditDialog(
    gov: ShippingGovernorate,
    onDismiss: () -> Unit,
    onSave: (ShippingGovernorate) -> Unit
) {
    var name by remember { mutableStateOf(gov.name) }
    var priceText by remember { mutableStateOf(gov.deliveryPrice.toString()) }
    var daysText by remember { mutableStateOf(gov.estimatedDays) }
    var isActive by remember { mutableStateOf(gov.isActive) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(0.95f),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (gov.name.isBlank()) "إضافة محافظة جديدة" else "تعديل محافظة ${gov.name}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المحافظة") },
                    placeholder = { Text("مثال: الدقهلية") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("سعر الشحن الأساسي (ج.م)") },
                    placeholder = { Text("65") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it },
                    label = { Text("مدة التوصيل التقديرية") },
                    placeholder = { Text("2-3 أيام") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تفعيل الشحن للمحافظة", fontWeight = FontWeight.SemiBold)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val price = priceText.toDoubleOrNull() ?: 60.0
                            onSave(gov.copy(name = name, deliveryPrice = price, estimatedDays = daysText, isActive = isActive))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("حفظ البيانات", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}

@Composable
fun CenterEditDialog(
    center: ShippingCenter,
    onDismiss: () -> Unit,
    onSave: (ShippingCenter) -> Unit
) {
    var name by remember { mutableStateOf(center.name) }
    var priceText by remember { mutableStateOf(center.deliveryPrice.toString()) }
    var contact by remember { mutableStateOf(center.contactNumber ?: "") }
    var isActive by remember { mutableStateOf(center.isActive) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(0.95f),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (center.name.isBlank()) "إضافة مركز / منطقة" else "تعديل مركز ${center.name}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المركز أو المدينة الفرعية") },
                    placeholder = { Text("مثال: ميت غمر أو السنبلاوين") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("سعر التوصيل للمركز (ج.م)") },
                    placeholder = { Text("55") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("رقم هاتف المندوب المختص") },
                    placeholder = { Text("مثال: 01012345678") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("حالة المركز نشط", fontWeight = FontWeight.SemiBold)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val price = priceText.toDoubleOrNull() ?: center.deliveryPrice
                            onSave(center.copy(name = name, deliveryPrice = price, contactNumber = contact.ifBlank { null }, isActive = isActive))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("حفظ المركز", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}
