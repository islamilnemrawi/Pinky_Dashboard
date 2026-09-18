package com.pinky.dashboard.ui.screens.products

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import java.io.InputStream
import java.util.UUID
import com.pinky.dashboard.core.util.Formatters
import com.pinky.dashboard.domain.model.AdminUser
import com.pinky.dashboard.domain.model.Category
import com.pinky.dashboard.domain.model.Product
import com.pinky.dashboard.domain.model.ProductVariant
import com.pinky.dashboard.ui.components.ConfirmDestructiveDialog
import com.pinky.dashboard.ui.components.EmptyStateCard
import com.pinky.dashboard.ui.components.ProtectedFieldPlaceholder
import com.pinky.dashboard.ui.theme.PinkPrimary
import com.pinky.dashboard.ui.theme.StatusCancelled

@Composable
fun ProductsScreen(
    products: List<Product>,
    categories: List<Category>,
    currentUser: AdminUser?,
    onSaveProduct: (Product) -> Unit,
    onSaveCategory: (Category) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onToggleActive: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    // Multi-select for Bulk actions
    var selectedProductIds by remember { mutableStateOf(setOf<String>()) }
    var isSelectionMode by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, searchQuery, selectedCategoryId) {
        products.filter { product ->
            val matchesQuery = searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                (currentUser?.canViewInternalCode == true && product.internalCode?.contains(searchQuery, ignoreCase = true) == true)
            val matchesCategory = selectedCategoryId == null || product.categoryId == selectedCategoryId
            matchesQuery && matchesCategory
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingProduct = null
                    showAddEditDialog = true
                },
                containerColor = PinkPrimary,
                contentColor = Color(0xFF381528),
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("إضافة منتج جديد", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_product_fab")
            )
        },
        modifier = modifier.testTag("products_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header: Search, Bulk Mode, Import Excel
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("products_search_input"),
                        placeholder = { Text("بحث باسم المنتج أو الكود...") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = PinkPrimary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Import CSV/Excel button
                    IconButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.testTag("import_excel_btn")
                    ) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = "استيراد ملف Excel/CSV", tint = PinkPrimary)
                    }

                    // Toggle Bulk Selection Mode
                    FilledTonalIconToggleButton(
                        checked = isSelectionMode,
                        onCheckedChange = {
                            isSelectionMode = it
                            if (!it) selectedProductIds = emptySet()
                        }
                    ) {
                        Icon(Icons.Outlined.Checklist, contentDescription = "تحديد متعدد")
                    }
                }

                // Category Filter Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                        label = { Text("كل الأقسام (${products.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PinkPrimary,
                            selectedLabelColor = Color(0xFF381528)
                        )
                    )

                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PinkPrimary,
                                selectedLabelColor = Color(0xFF381528)
                            )
                        )
                    }
                }

                // Bulk Actions Bar (when in selection mode)
                if (isSelectionMode && selectedProductIds.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = PinkPrimary.copy(alpha = 0.12f)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تم تحديد ${selectedProductIds.size} منتج",
                                fontWeight = FontWeight.Bold,
                                color = PinkPrimary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        selectedProductIds.forEach { onToggleActive(it, true) }
                                        selectedProductIds = emptySet()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399), contentColor = Color.Black)
                                ) {
                                    Text("تفعيل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        selectedProductIds.forEach { onToggleActive(it, false) }
                                        selectedProductIds = emptySet()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusCancelled, contentColor = Color.White)
                                ) {
                                    Text("تعطيل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        selectedProductIds.forEach { onDeleteProduct(it) }
                                        selectedProductIds = emptySet()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("حذف", fontSize = 11.sp, color = StatusCancelled)
                                }
                            }
                        }
                    }
                }
            }

            // Products List
            if (filteredProducts.isEmpty()) {
                EmptyStateCard(
                    title = "لا توجد منتجات مسجلة",
                    message = if (searchQuery.isNotBlank()) "لا توجد نتائج مطابقة للبحث" else "ابدأ بإضافة أول منتج في متجر بينكي",
                    icon = Icons.Outlined.Inventory,
                    actionButtonText = "إضافة منتج الآن",
                    onActionClick = {
                        editingProduct = null
                        showAddEditDialog = true
                    },
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("products_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val isSelected = selectedProductIds.contains(product.id)
                        ProductCardItem(
                            product = product,
                            currentUser = currentUser,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onSelect = {
                                selectedProductIds = if (isSelected) selectedProductIds - product.id else selectedProductIds + product.id
                            },
                            onEdit = {
                                editingProduct = product
                                showAddEditDialog = true
                            },
                            onDuplicate = {
                                val duplicated = product.copy(
                                    id = "prod_${System.currentTimeMillis()}",
                                    name = "${product.name} (نسخة)",
                                    createdAt = System.currentTimeMillis()
                                )
                                onSaveProduct(duplicated)
                            },
                            onDelete = { productToDelete = product },
                            onToggleActive = { isActive -> onToggleActive(product.id, isActive) }
                        )
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        ProductEditDialog(
            initialProduct = editingProduct,
            categories = categories,
            currentUser = currentUser,
            onDismiss = { showAddEditDialog = false },
            onSave = { savedProduct ->
                onSaveProduct(savedProduct)
                showAddEditDialog = false
            }
        )
    }

    if (showImportDialog) {
        ProductImportDialog(
            products = products,
            categories = categories,
            onSaveProduct = onSaveProduct,
            onSaveCategory = onSaveCategory,
            onDismiss = { showImportDialog = false }
        )
    }

    productToDelete?.let { product ->
        ConfirmDestructiveDialog(
            title = "حذف المنتج نهائياً؟",
            message = "هل أنت متأكد من حذف \"${product.name}\"؟ لا يمكن التراجع عن هذا الإجراء.",
            confirmButtonText = "نعم، حذف المنتج",
            onConfirm = {
                onDeleteProduct(product.id)
                productToDelete = null
            },
            onDismiss = { productToDelete = null }
        )
    }
}

@Composable
fun ProductCardItem(
    product: Product,
    currentUser: AdminUser?,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PinkPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PinkPrimary)) else CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Checkbox (if selection mode), Category, Active Switch, Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isSelectionMode) {
                        Checkbox(checked = isSelected, onCheckedChange = { onSelect() })
                    }

                    Text(
                        text = product.categoryName.ifEmpty { "قسم عام" },
                        style = MaterialTheme.typography.labelSmall,
                        color = PinkPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (product.isFeatured) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PinkPrimary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "مميز ⭐",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = PinkPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Switch(
                        checked = product.isActive,
                        onCheckedChange = onToggleActive,
                        modifier = Modifier.testTag("product_active_switch_${product.id}")
                    )

                    IconButton(onClick = onDuplicate, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "تكرار المنتج", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Delete, contentDescription = "حذف", tint = StatusCancelled)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name & Description
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (product.description.isNotEmpty()) {
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stock & Price row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prices
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = Formatters.formatCurrencyEgp(product.price),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PinkPrimary
                    )
                    if (product.oldPrice != null && product.oldPrice > product.price) {
                        Text(
                            text = Formatters.formatCurrencyEgp(product.oldPrice),
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Stock Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (product.stock > 5) MaterialTheme.colorScheme.surfaceVariant else StatusCancelled.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (product.stock > 0) "المخزون: ${product.stock} قطعة" else "نفذ المخزون!",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (product.stock > 5) MaterialTheme.colorScheme.onSurfaceVariant else StatusCancelled,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Protected fields row (Wholesale price & Internal SKU code)
            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wholesale Price (Protected)
                if (currentUser?.canViewWholesale == true) {
                    Text(
                        text = "سعر الجملة/التكلفة: ${product.wholesalePrice?.let { Formatters.formatCurrencyEgp(it) } ?: "غير محدد"}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF34D399)
                    )
                } else {
                    ProtectedFieldPlaceholder(fieldName = "سعر الجملة")
                }

                // Internal Code / SKU (Protected)
                if (currentUser?.canViewInternalCode == true) {
                    Text(
                        text = "كود SKU: ${product.internalCode ?: "—"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ProtectedFieldPlaceholder(fieldName = "كود المنتج")
                }
            }
        }
    }
}

@Composable
fun ProductImportDialog(
    products: List<Product>,
    categories: List<Category>,
    onSaveProduct: (Product) -> Unit,
    onSaveCategory: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf<String?>(null) }
    var parsedProducts by remember { mutableStateOf<List<ImportProductPreview>>(emptyList()) }
    
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var progressText by remember { mutableStateOf("") }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successSummary by remember { mutableStateOf<String?>(null) }
    
    // Launch system file picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                selectedUri = uri
                fileName = getFileName(context, uri)
                errorMessage = null
                successSummary = null
                
                // Parse file in background
                coroutineScope.launch {
                    isLoading = true
                    progressText = "جاري قراءة وتحليل الملف..."
                    withContext(Dispatchers.IO) {
                        handleFileSelected(
                            context = context,
                            uri = uri,
                            existingProducts = products,
                            onParsed = { previews ->
                                parsedProducts = previews
                                isLoading = false
                            },
                            onError = { error ->
                                errorMessage = error
                                isLoading = false
                            }
                        )
                    }
                }
            }
        }
    )

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .testTag("product_import_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "استيراد المنتجات (Excel / CSV)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss, enabled = !isLoading) {
                        Icon(Icons.Outlined.Close, contentDescription = "إغلاق")
                    }
                }

                if (successSummary == null) {
                    Text(
                        text = "قم برفع ملف Excel (.xlsx / .xls) أو CSV يحتوي على أعمدة المنتجات مثل: الاسم، السعر، المخزون، الكود، والقسم لاستيرادها تلقائياً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // File Selector Drop Area (responds to click and touch)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                color = if (selectedUri != null) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = !isLoading) {
                                filePickerLauncher.launch("*/*")
                            },
                        color = if (selectedUri != null) Color(0xFF10B981).copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (fileName != null) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = fileName!!,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "انقر لتغيير الملف المختار",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.CloudUpload,
                                    contentDescription = null,
                                    tint = PinkPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "انقر لاختيار ملف .xlsx أو .csv من جهازك",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "يقبل الملفات باللغة العربية والإنجليزية",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Progress or Error status
                    if (isLoading) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = PinkPrimary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                text = progressText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    errorMessage?.let { error ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Previews and validation results
                    if (parsedProducts.isNotEmpty() && !isLoading) {
                        val validCount = parsedProducts.count { it.isValid }
                        val invalidCount = parsedProducts.count { !it.isValid }
                        val duplicatesCount = parsedProducts.count { it.isValid && it.isDuplicateUpdate }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "معاينة البيانات المستخرجة (${parsedProducts.size} صفوف):",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "صالحة: $validCount | تالفة: $invalidCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (invalidCount > 0) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                                )
                                if (duplicatesCount > 0) {
                                    Text(
                                        text = "سيتم تحديث $duplicatesCount منتجات مكررة",
                                        fontSize = 10.sp,
                                        color = Color(0xFFF59E0B),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Preview List
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(parsedProducts) { p ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (p.isValid) {
                                        if (p.isDuplicateUpdate) Color(0xFFF59E0B).copy(alpha = 0.05f)
                                        else Color(0xFF10B981).copy(alpha = 0.05f)
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        width = 0.5.dp,
                                        color = if (p.isValid) {
                                            if (p.isDuplicateUpdate) Color(0xFFF59E0B).copy(alpha = 0.3f)
                                            else Color(0xFF10B981).copy(alpha = 0.3f)
                                        } else {
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                        }
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = p.name.ifEmpty { "اسم غير محدد" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (p.isValid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = "${p.price} ج.م",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = PinkPrimary
                                            )
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text("القسم: ${p.categoryName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (p.internalCode != null) {
                                                Text("الكود: ${p.internalCode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text("المخزون: ${p.stock}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        if (p.isDuplicateUpdate && p.isValid) {
                                            Text(
                                                text = "⚠️ مكرر: سيتم تحديث بيانات المنتج الحالي بدلاً من تكراره",
                                                fontSize = 10.sp,
                                                color = Color(0xFFD97706),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (!p.isValid) {
                                            Text(
                                                text = "❌ خطأ: ${p.errors.joinToString(" | ")}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (!isLoading) {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss, enabled = !isLoading) {
                            Text("إلغاء")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val validOnes = parsedProducts.filter { it.isValid }
                                if (validOnes.isNotEmpty()) {
                                    coroutineScope.launch {
                                        isLoading = true
                                        var successCount = 0
                                        var failCount = 0
                                        val total = validOnes.size
                                        
                                        val mutableCategories = categories.toMutableList()
                                        
                                        for (index in validOnes.indices) {
                                            val preview = validOnes[index]
                                            progress = (index.toFloat() / total)
                                            progressText = "جاري استيراد المنتجات (${index + 1}/$total): ${preview.name}"
                                            
                                            try {
                                                // Find or create category on the fly
                                                var catId = ""
                                                var catName = preview.categoryName
                                                val existingCat = mutableCategories.find { it.name.equals(catName, ignoreCase = true) }
                                                if (existingCat != null) {
                                                    catId = existingCat.id
                                                    catName = existingCat.name
                                                } else {
                                                    catId = UUID.randomUUID().toString()
                                                    val newCat = Category(
                                                        id = catId,
                                                        name = catName,
                                                        imageUrl = "",
                                                        sortOrder = 0,
                                                        isActive = true
                                                    )
                                                    onSaveCategory(newCat)
                                                    mutableCategories.add(newCat)
                                                }
                                                
                                                val pId = preview.originalProductId ?: UUID.randomUUID().toString()
                                                val product = Product(
                                                    id = pId,
                                                    name = preview.name,
                                                    description = preview.description,
                                                    imageUrl = preview.imageUrl,
                                                    gallery = emptyList(),
                                                    categoryId = catId,
                                                    categoryName = catName,
                                                    price = preview.price,
                                                    oldPrice = preview.oldPrice,
                                                    discountPercent = if (preview.oldPrice != null && preview.oldPrice > preview.price) {
                                                        (((preview.oldPrice - preview.price) / preview.oldPrice) * 100).toInt()
                                                    } else null,
                                                    wholesalePrice = preview.wholesalePrice,
                                                    internalCode = preview.internalCode,
                                                    stock = preview.stock,
                                                    colors = preview.colors,
                                                    sizes = preview.sizes,
                                                    variants = emptyList(),
                                                    isActive = true,
                                                    isFeatured = false,
                                                    createdAt = System.currentTimeMillis()
                                                )
                                                onSaveProduct(product)
                                                successCount++
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                failCount++
                                            }
                                            kotlinx.coroutines.delay(80)
                                        }
                                        
                                        isLoading = false
                                        successSummary = "تم استيراد المنتجات بنجاح! تم استيراد $successCount منتجات صالحة وتحديثها على السيرفر، وفشل $failCount منتج."
                                    }
                                }
                            },
                            enabled = parsedProducts.any { it.isValid } && !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("بدء الاستيراد", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Success Screen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "اكتمل الاستيراد بنجاح!",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = successSummary!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إتمام", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Unified parser helpers
data class ImportProductPreview(
    val name: String,
    val price: Double,
    val oldPrice: Double?,
    val wholesalePrice: Double?,
    val internalCode: String?,
    val stock: Int,
    val categoryName: String,
    val description: String,
    val imageUrl: String,
    val colors: List<String>,
    val sizes: List<String>,
    val isValid: Boolean,
    val errors: List<String>,
    val isDuplicateUpdate: Boolean,
    val originalProductId: String?
)

fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "ملف غير معروف"
}

fun getCellValueAsString(cell: org.apache.poi.ss.usermodel.Cell?): String {
    if (cell == null) return ""
    return when (cell.cellType) {
        CellType.STRING -> cell.stringCellValue.trim()
        CellType.NUMERIC -> {
            val d = cell.numericCellValue
            if (d == d.toLong().toDouble()) {
                d.toLong().toString()
            } else {
                d.toString()
            }
        }
        CellType.BOOLEAN -> cell.booleanCellValue.toString()
        CellType.FORMULA -> {
            try {
                cell.stringCellValue.trim()
            } catch (e: Exception) {
                try {
                    cell.numericCellValue.toString()
                } catch (e2: Exception) {
                    ""
                }
            }
        }
        else -> ""
    }
}

fun parseExcelStream(inputStream: InputStream): List<List<String>> {
    val result = mutableListOf<List<String>>()
    val workbook = WorkbookFactory.create(inputStream)
    val sheet = workbook.getSheetAt(0)
    for (r in 0..sheet.lastRowNum) {
        val row = sheet.getRow(r) ?: continue
        val rowList = mutableListOf<String>()
        for (c in 0 until row.lastCellNum) {
            val cell = row.getCell(c)
            rowList.add(getCellValueAsString(cell))
        }
        result.add(rowList)
    }
    workbook.close()
    return result
}

fun parseCsvStream(inputStream: InputStream): List<List<String>> {
    val result = mutableListOf<List<String>>()
    val reader = inputStream.bufferedReader()
    reader.useLines { lines ->
        for (line in lines) {
            if (line.isBlank()) continue
            val row = parseCsvLine(line)
            result.add(row)
        }
    }
    return result
}

fun parseCsvLine(line: String): List<String> {
    val tokens = mutableListOf<String>()
    var sb = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        if (c == '\"') {
            inQuotes = !inQuotes
        } else if (c == ',' && !inQuotes) {
            tokens.add(sb.toString().trim())
            sb = StringBuilder()
        } else {
            sb.append(c)
        }
        i++
    }
    tokens.add(sb.toString().trim())
    return tokens
}

fun handleFileSelected(
    context: Context,
    uri: Uri,
    existingProducts: List<Product>,
    onParsed: (List<ImportProductPreview>) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            onError("تعذر فتح الملف المختار.")
            return
        }
        val name = getFileName(context, uri).lowercase()
        val rawRows = if (name.endsWith(".csv")) {
            parseCsvStream(inputStream)
        } else {
            parseExcelStream(inputStream)
        }
        
        if (rawRows.isEmpty()) {
            onError("الملف فارغ أو غير صالح.")
            return
        }
        
        // Match header columns
        val headerRow = rawRows[0]
        var nameCol = -1
        var priceCol = -1
        var oldPriceCol = -1
        var wholesaleCol = -1
        var codeCol = -1
        var stockCol = -1
        var catCol = -1
        var descCol = -1
        var imgCol = -1
        var colorsCol = -1
        var sizesCol = -1
        
        for (col in headerRow.indices) {
            val header = headerRow[col].lowercase().trim()
            if (header.isEmpty()) continue
            when {
                header.contains("اسم") || header == "name" || header == "title" -> nameCol = col
                header.contains("سعر البيع") || header == "السعر" || header == "price" -> priceCol = col
                header.contains("القديم") || header.contains("قبل الخصم") || header == "old_price" || header == "old price" -> oldPriceCol = col
                header.contains("الجملة") || header == "wholesale" || header == "wholesale price" || header == "wholesale_price" -> wholesaleCol = col
                header.contains("كود") || header == "sku" || header == "code" || header == "internal_code" || header == "internal code" -> codeCol = col
                header.contains("كمية") || header.contains("مخزون") || header == "stock" || header == "quantity" || header == "qty" -> stockCol = col
                header.contains("قسم") || header.contains("تصنيف") || header == "category" -> catCol = col
                header.contains("وصف") || header == "description" -> descCol = col
                header.contains("صورة") || header == "image" || header == "image_url" || header == "image url" -> imgCol = col
                header.contains("ألوان") || header.contains("الوان") || header == "colors" -> colorsCol = col
                header.contains("مقاس") || header == "sizes" -> sizesCol = col
            }
        }
        
        val previews = mutableListOf<ImportProductPreview>()
        
        if (nameCol == -1 || priceCol == -1) {
            var missing = ""
            if (nameCol == -1) missing += " (اسم المنتج)"
            if (priceCol == -1) missing += " (السعر)"
            onError("الملف المختار يفتقد لأعمدة أساسية مطلوبة:$missing. يرجى مراجعة عناوين الأعمدة.")
            return
        }
        
        for (i in 1 until rawRows.size) {
            val row = rawRows[i]
            if (row.isEmpty() || row.all { it.isBlank() }) continue
            
            val errors = mutableListOf<String>()
            
            val pName = if (nameCol < row.size) row[nameCol].trim() else ""
            if (pName.isEmpty()) {
                errors.add("اسم المنتج فارغ")
            }
            
            val priceStr = if (priceCol < row.size) row[priceCol].trim() else ""
            val price = priceStr.toDoubleOrNull()
            if (price == null || price < 0) {
                errors.add("السعر غير صالح: $priceStr")
            }
            
            val oldPriceStr = if (oldPriceCol != -1 && oldPriceCol < row.size) row[oldPriceCol].trim() else ""
            val oldPrice = if (oldPriceStr.isNotEmpty()) oldPriceStr.toDoubleOrNull() else null
            
            val wholesaleStr = if (wholesaleCol != -1 && wholesaleCol < row.size) row[wholesaleCol].trim() else ""
            val wholesale = if (wholesaleStr.isNotEmpty()) wholesaleStr.toDoubleOrNull() else null
            
            val code = if (codeCol != -1 && codeCol < row.size) row[codeCol].trim() else null
            
            val stockStr = if (stockCol != -1 && stockCol < row.size) row[stockCol].trim() else ""
            val stock = if (stockStr.isNotEmpty()) stockStr.toIntOrNull() ?: 0 else 0
            
            val category = if (catCol != -1 && catCol < row.size) row[catCol].trim() else "عام"
            val description = if (descCol != -1 && descCol < row.size) row[descCol].trim() else ""
            val imgUrl = if (imgCol != -1 && imgCol < row.size) row[imgCol].trim() else ""
            
            val colorsRaw = if (colorsCol != -1 && colorsCol < row.size) row[colorsCol].trim() else ""
            val colors = if (colorsRaw.isNotEmpty()) colorsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() } else emptyList()
            
            val sizesRaw = if (sizesCol != -1 && sizesCol < row.size) row[sizesCol].trim() else ""
            val sizes = if (sizesRaw.isNotEmpty()) sizesRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() } else emptyList()
            
            // Check for duplicate
            val isDuplicate: Boolean
            val originalId: String?
            
            val existingByCode = if (!code.isNullOrBlank()) {
                existingProducts.find { it.internalCode?.equals(code, ignoreCase = true) == true }
            } else null
            val existingByName = existingProducts.find { it.name.equals(pName, ignoreCase = true) == true }
            val matched = existingByCode ?: existingByName
            
            if (matched != null) {
                isDuplicate = true
                originalId = matched.id
            } else {
                isDuplicate = false
                originalId = null
            }
            
            previews.add(
                ImportProductPreview(
                    name = pName,
                    price = price ?: 0.0,
                    oldPrice = oldPrice,
                    wholesalePrice = wholesale,
                    internalCode = code,
                    stock = stock,
                    categoryName = category.ifEmpty { "عام" },
                    description = description,
                    imageUrl = imgUrl,
                    colors = colors,
                    sizes = sizes,
                    isValid = errors.isEmpty(),
                    errors = errors,
                    isDuplicateUpdate = isDuplicate,
                    originalProductId = originalId
                )
            )
        }
        
        onParsed(previews)
    } catch (e: Exception) {
        e.printStackTrace()
        onError("فشل في معالجة وقراءة الملف المختار: ${e.message}")
    }
}
