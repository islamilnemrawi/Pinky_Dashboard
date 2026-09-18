package com.pinky.dashboard.ui.screens.products

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.pinky.dashboard.domain.model.AdminUser
import com.pinky.dashboard.domain.model.Category
import com.pinky.dashboard.domain.model.Product
import com.pinky.dashboard.domain.model.ProductVariant
import com.pinky.dashboard.ui.components.ProtectedFieldPlaceholder
import com.pinky.dashboard.ui.theme.PinkPrimary
import com.pinky.dashboard.ui.theme.StatusCancelled

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditDialog(
    initialProduct: Product?,
    categories: List<Category>,
    currentUser: AdminUser?,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val isNew = initialProduct == null

    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var description by remember { mutableStateOf(initialProduct?.description ?: "") }
    var imageUrl by remember { mutableStateOf(initialProduct?.imageUrl ?: "") }
    var selectedCategoryId by remember { mutableStateOf(initialProduct?.categoryId ?: (categories.firstOrNull()?.id ?: "")) }
    var priceText by remember { mutableStateOf(initialProduct?.price?.toString() ?: "") }
    var oldPriceText by remember { mutableStateOf(initialProduct?.oldPrice?.toString() ?: "") }
    var discountText by remember { mutableStateOf(initialProduct?.discountPercent?.toString() ?: "") }
    var wholesalePriceText by remember { mutableStateOf(initialProduct?.wholesalePrice?.toString() ?: "") }
    var internalCode by remember { mutableStateOf(initialProduct?.internalCode ?: "") }
    var stockText by remember { mutableStateOf(initialProduct?.stock?.toString() ?: "10") }
    var colorsText by remember { mutableStateOf(initialProduct?.colors?.joinToString("، ") ?: "") }
    var sizesText by remember { mutableStateOf(initialProduct?.sizes?.joinToString("، ") ?: "") }
    var weightText by remember { mutableStateOf("") } // Weight field placeholder state for future schema link
    var isActive by remember { mutableStateOf(initialProduct?.isActive ?: true) }
    var isFeatured by remember { mutableStateOf(initialProduct?.isFeatured ?: false) }

    // Media Picker States
    var selectedMainImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedGalleryUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Photo pickers
    val mainImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedMainImageUri = uri
            // Mock Supabase/R2 resulting URL architecture for preview/save flow
            imageUrl = "https://pinky-store.supabase.co/storage/v1/object/public/pinky-products/${uri.lastPathSegment}.jpg"
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedGalleryUris = uris
        }
    }

    // Variants list state
    var variantsList by remember { mutableStateOf(initialProduct?.variants ?: emptyList()) }
    var showVariantForm by remember { mutableStateOf(false) }
    var tempVarName by remember { mutableStateOf("") }
    var tempVarColor by remember { mutableStateOf("") }
    var tempVarSize by remember { mutableStateOf("") }
    var tempVarPriceText by remember { mutableStateOf("") }
    var tempVarStockText by remember { mutableStateOf("10") }
    var tempVarSku by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.95f)
                .testTag("product_edit_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isNew) "إضافة منتج جديد لمتجر بينكي" else "تعديل المنتج: ${initialProduct?.name}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = PinkPrimary
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "إلغاء")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Form inputs
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Name
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("اسم المنتج *") },
                            placeholder = { Text("مثال: عباية استقبال مطرزة بالورود") },
                            modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Description
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("وصف المنتج") },
                            placeholder = { Text("تفاصيل الخامة والتصميم والألوان الزاهية...") },
                            modifier = Modifier.fillMaxWidth().testTag("product_desc_input"),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 3
                        )
                    }

                    // Category Selection & Weight
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Category Dropdown
                            var categoryExpanded by remember { mutableStateOf(false) }
                            val currentCategoryName = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "اختر القسم"

                            Box(modifier = Modifier.weight(1.3f)) {
                                OutlinedTextField(
                                    value = currentCategoryName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("القسم") },
                                    trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { categoryExpanded = true },
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                DropdownMenu(
                                    expanded = categoryExpanded,
                                    onDismissRequest = { categoryExpanded = false }
                                ) {
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat.name) },
                                            onClick = {
                                                selectedCategoryId = cat.id
                                                categoryExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Weight
                            OutlinedTextField(
                                value = weightText,
                                onValueChange = { weightText = it },
                                label = { Text("الوزن (جرام)") },
                                placeholder = { Text("مثال: 350") },
                                modifier = Modifier.weight(0.7f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Pricing Row (Sale, Old, Discount)
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = priceText,
                                onValueChange = { priceText = it },
                                label = { Text("سعر البيع *") },
                                placeholder = { Text("850") },
                                modifier = Modifier.weight(1f).testTag("product_price_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = oldPriceText,
                                onValueChange = { oldPriceText = it },
                                label = { Text("السعر القديم") },
                                placeholder = { Text("1100") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = discountText,
                                onValueChange = { discountText = it },
                                label = { Text("الخصم %") },
                                placeholder = { Text("20") },
                                modifier = Modifier.weight(0.8f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Protected Fields Block (Wholesale Price, SKU/Internal Code)
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "بيانات إدارية محمية بالصلاحيات 🔒",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PinkPrimary
                                )

                                if (currentUser?.canViewWholesale == true) {
                                    OutlinedTextField(
                                        value = wholesalePriceText,
                                        onValueChange = { wholesalePriceText = it },
                                        label = { Text("سعر الجملة / التكلفة (ج.م)") },
                                        placeholder = { Text("مثال: 450") },
                                        modifier = Modifier.fillMaxWidth().testTag("product_wholesale_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                } else {
                                    ProtectedFieldPlaceholder("سعر الجملة")
                                }

                                if (currentUser?.canViewInternalCode == true) {
                                    OutlinedTextField(
                                        value = internalCode,
                                        onValueChange = { internalCode = it },
                                        label = { Text("الكود الداخلي / SKU") },
                                        placeholder = { Text("مثال: PNK-ABY-102") },
                                        modifier = Modifier.fillMaxWidth().testTag("product_sku_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                } else {
                                    ProtectedFieldPlaceholder("الكود الداخلي SKU")
                                }
                            }
                        }
                    }

                    // Stock, Colors, Sizes
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = stockText,
                                onValueChange = { stockText = it },
                                label = { Text("الكمية بالمخزون *") },
                                modifier = Modifier.weight(1f).testTag("product_stock_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = colorsText,
                                onValueChange = { colorsText = it },
                                label = { Text("الألوان (مفصولة بـ ،)") },
                                placeholder = { Text("وردي، أسود، كحلي") },
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = sizesText,
                            onValueChange = { sizesText = it },
                            label = { Text("المقاسات المتاحة (مفصولة بـ ،)") },
                            placeholder = { Text("M، L، XL، XXL") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Image picker & Gallery picker (R2 Ready Architecture)
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "صور المنتج والوسائط سريعة التوصيل سحابياً",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PinkPrimary
                                )

                                // Main Image Picker Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { mainImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("رفع صورة رئيسية من الهاتف", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    // Main Image preview
                                    if (selectedMainImageUri != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, PinkPrimary, RoundedCornerShape(8.dp))
                                        ) {
                                            AsyncImage(
                                                model = selectedMainImageUri,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    } else if (imageUrl.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                        ) {
                                            AsyncImage(
                                                model = imageUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }

                                if (selectedMainImageUri != null) {
                                    Text(
                                        text = "✔️ جاهز للرفع السحابي التلقائي إلى مساحة Cloudflare R2",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF34D399)
                                    )
                                }

                                // Gallery Picker Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Outlined.Collections, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("اختيار معرض صور المنتج (Gallery)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    }

                                    if (selectedGalleryUris.isNotEmpty()) {
                                        Text(
                                            text = "(${selectedGalleryUris.size} صور)",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PinkPrimary
                                        )
                                    }
                                }

                                // Display selected gallery preview
                                if (selectedGalleryUris.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(selectedGalleryUris) { uri ->
                                            Box(
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                            ) {
                                                AsyncImage(
                                                    model = uri,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Product Variants management
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "الموديلات والبدائل (Variants) للطلب الدقيق",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PinkPrimary
                                    )

                                    IconButton(onClick = { showVariantForm = !showVariantForm }) {
                                        Icon(
                                            if (showVariantForm) Icons.Outlined.RemoveCircleOutline else Icons.Outlined.AddCircleOutline,
                                            contentDescription = "إضافة موديل",
                                            tint = PinkPrimary
                                        )
                                    }
                                }

                                // Variant quick input form
                                AnimatedVisibility(visible = showVariantForm) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = tempVarName,
                                            onValueChange = { tempVarName = it },
                                            label = { Text("اسم البديل (مثال: أحمر / XL)") },
                                            shape = RoundedCornerShape(10.dp)
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = tempVarPriceText,
                                                onValueChange = { tempVarPriceText = it },
                                                label = { Text("سعر إضافي") },
                                                placeholder = { Text("0") },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            )

                                            OutlinedTextField(
                                                value = tempVarStockText,
                                                onValueChange = { tempVarStockText = it },
                                                label = { Text("المخزون") },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (tempVarName.isNotBlank()) {
                                                    val additionalPrice = tempVarPriceText.toDoubleOrNull() ?: 0.0
                                                    val stock = tempVarStockText.toIntOrNull() ?: 10
                                                    val newVar = ProductVariant(
                                                        id = "var_${System.currentTimeMillis()}",
                                                        name = tempVarName,
                                                        color = tempVarColor.ifBlank { null },
                                                        size = tempVarSize.ifBlank { null },
                                                        additionalPrice = additionalPrice,
                                                        stock = stock,
                                                        sku = tempVarSku.ifBlank { null }
                                                    )
                                                    variantsList = variantsList + newVar
                                                    // clear form
                                                    tempVarName = ""
                                                    tempVarColor = ""
                                                    tempVarSize = ""
                                                    tempVarPriceText = ""
                                                    tempVarStockText = "10"
                                                    tempVarSku = ""
                                                    showVariantForm = false
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528))
                                        ) {
                                            Text("إضافة البديل للجدول", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // List existing variants
                                if (variantsList.isEmpty()) {
                                    Text(
                                        "لم يتم إضافة أي موديلات متفرعة لهذا المنتج حالياً.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        variantsList.forEachIndexed { idx, vr ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(vr.name, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        "مخزون: ${vr.stock} • سعر إضافي: +${vr.additionalPrice} ج.م",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { variantsList = variantsList.filterIndexed { i, _ -> i != idx } },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Outlined.Delete, contentDescription = "حذف بديل", tint = StatusCancelled)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Switch Controls (Active, Featured)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("المنتج نشط ومتاح للشراء في الموقع", fontWeight = FontWeight.SemiBold)
                            Switch(checked = isActive, onCheckedChange = { isActive = it })
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("عرض كمنتج مميز بالصفحة الرئيسية ⭐", fontWeight = FontWeight.SemiBold)
                            Switch(checked = isFeatured, onCheckedChange = { isFeatured = it })
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Footer Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val price = priceText.toDoubleOrNull() ?: 0.0
                            val oldPrice = oldPriceText.toDoubleOrNull()
                            val wholesalePrice = wholesalePriceText.toDoubleOrNull()
                            val stock = stockText.toIntOrNull() ?: 0
                            val discount = discountText.toIntOrNull()
                            val cat = categories.firstOrNull { it.id == selectedCategoryId }

                            val updated = Product(
                                id = initialProduct?.id ?: "prod_${System.currentTimeMillis()}",
                                name = name.ifBlank { "منتج بينكي جديد" },
                                description = description,
                                imageUrl = imageUrl,
                                gallery = selectedGalleryUris.map { it.toString() }.ifEmpty { initialProduct?.gallery ?: emptyList() },
                                categoryId = selectedCategoryId,
                                categoryName = cat?.name ?: "عام",
                                price = price,
                                oldPrice = oldPrice,
                                discountPercent = discount,
                                wholesalePrice = wholesalePrice,
                                internalCode = internalCode.ifBlank { null },
                                stock = stock,
                                colors = colorsText.split("،", ",").map { it.trim() }.filter { it.isNotEmpty() },
                                sizes = sizesText.split("،", ",").map { it.trim() }.filter { it.isNotEmpty() },
                                variants = variantsList,
                                isActive = isActive,
                                isFeatured = isFeatured,
                                createdAt = initialProduct?.createdAt ?: System.currentTimeMillis()
                            )
                            onSave(updated)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528)),
                        modifier = Modifier.weight(1f).testTag("save_product_btn")
                    ) {
                        Text(if (isNew) "إضافة المنتج" else "حفظ التعديلات", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}
