package com.pinky.dashboard.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import com.pinky.dashboard.data.remote.SupabaseClient
import com.pinky.dashboard.domain.model.Category
import com.pinky.dashboard.ui.components.ConfirmDestructiveDialog
import com.pinky.dashboard.ui.components.EmptyStateCard
import com.pinky.dashboard.ui.theme.PinkPrimary
import com.pinky.dashboard.ui.theme.StatusCancelled
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull

@Composable
fun CategoriesScreen(
    categories: List<Category>,
    onSaveCategory: (Category) -> Unit,
    onDeleteCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingCategory = null
                    showDialog = true
                },
                containerColor = PinkPrimary,
                contentColor = Color(0xFF381528),
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("إضافة قسم جديد", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_category_fab")
            )
        },
        modifier = modifier.testTag("categories_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "أقسام وتصنيفات متجر بينكي",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "إجمالي الأقسام: ${categories.size} (النشطة: ${categories.count { it.isActive }})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PinkPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Category, contentDescription = null, tint = PinkPrimary)
                    }
                }
            }

            if (categories.isEmpty()) {
                EmptyStateCard(
                    title = "لا توجد أقسام",
                    message = "قم بإنشاء الأقسام الرئيسية للمتجر (فساتين، إكسسوارات، عبايات، إلخ)",
                    icon = Icons.Outlined.Category,
                    actionButtonText = "إضافة قسم الآن",
                    onActionClick = {
                        editingCategory = null
                        showDialog = true
                    },
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("categories_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("category_card_${cat.id}"),
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${cat.sortOrder}",
                                            fontWeight = FontWeight.Bold,
                                            color = PinkPrimary
                                        )
                                    }

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = cat.name,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (!cat.isActive) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = StatusCancelled.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "معطل",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = StatusCancelled,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "ترتيب العرض: ${cat.sortOrder} • المنتجات: ${cat.productsCount}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            editingCategory = cat
                                            showDialog = true
                                        }
                                    ) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    IconButton(onClick = { categoryToDelete = cat }) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "حذف", tint = StatusCancelled)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        CategoryEditDialog(
            category = editingCategory,
            onDismiss = { showDialog = false },
            onSave = {
                onSaveCategory(it)
                showDialog = false
            }
        )
    }

    categoryToDelete?.let { cat ->
        ConfirmDestructiveDialog(
            title = "حذف القسم؟",
            message = "هل أنت متأكد من حذف قسم \"${cat.name}\"؟",
            confirmButtonText = "نعم، حذف",
            onConfirm = {
                onDeleteCategory(cat.id)
                categoryToDelete = null
            },
            onDismiss = { categoryToDelete = null }
        )
    }
}

@Composable
fun CategoryEditDialog(
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var imageUrl by remember { mutableStateOf(category?.imageUrl ?: "") }
    var sortOrderText by remember { mutableStateOf(category?.sortOrder?.toString() ?: "1") }
    var isActive by remember { mutableStateOf(category?.isActive ?: true) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var uploadSuccess by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            isUploading = true
            uploadError = null
            uploadSuccess = false

            coroutineScope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val mediaType = "image/jpeg".toMediaTypeOrNull()
                        val requestBody = okhttp3.RequestBody.create(mediaType, bytes)

                        // Unique, safe, automatic path
                        val fileName = "categories/${System.currentTimeMillis()}_${uri.lastPathSegment ?: "image"}.jpg"
                        val bucket = "pinky-products"

                        val response = SupabaseClient.service.uploadStorageFile(
                            bucket = bucket,
                            path = fileName,
                            file = requestBody,
                            contentType = "image/jpeg"
                        )

                        if (response.isSuccessful) {
                            // Automatically compute the public URL
                            val publicUrl = "${SupabaseClient.supabaseUrl}/storage/v1/object/public/$bucket/$fileName"
                            imageUrl = publicUrl
                            uploadSuccess = true
                        } else {
                            val errBody = response.errorBody()?.string() ?: ""
                            android.util.Log.e("SupabaseDiagnostic", "Image upload failed: HTTP ${response.code()} | $errBody")
                            uploadError = "فشل الرفع السحابي: ${response.code()} \nتأكد من اتصالك بالإنترنت وصلاحيات خادم التخزين."
                        }
                    } else {
                        uploadError = "خطأ: لم نتمكن من قراءة ملف الصورة المحدد."
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseDiagnostic", "Image upload exception", e)
                    uploadError = "عذرًا، حدث خطأ غير متوقع: ${e.localizedMessage}"
                } finally {
                    isUploading = false
                }
            }
        }
    }

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
                    text = if (category == null) "إضافة قسم جديد" else "تعديل القسم",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = PinkPrimary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم القسم *") },
                    placeholder = { Text("مثال: فساتين سهرة، عبايات، حقائب...") },
                    modifier = Modifier.fillMaxWidth().testTag("category_name_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = sortOrderText,
                    onValueChange = { sortOrderText = it },
                    label = { Text("ترتيب العرض (رقم)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // IMAGE UPLOAD CARD
                Text("صورة القسم *", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 1.dp,
                            color = if (uploadError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = !isUploading) {
                            imageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(color = PinkPrimary, modifier = Modifier.size(28.dp))
                            Text("جاري رفع الصورة للمتجر...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (selectedImageUri != null || imageUrl.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = selectedImageUri ?: imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Edit overlay banner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text("تغيير الصورة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudUpload,
                                contentDescription = null,
                                tint = PinkPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text("اضغط لاختيار صورة من معرض الهاتف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("صيغة JPG أو PNG", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (uploadError != null) {
                    Text(
                        text = uploadError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (uploadSuccess) {
                    Text(
                        text = "✔️ تم رفع الصورة بنجاح إلى سحابة متجر Pinky!",
                        color = Color(0xFF10B981),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("القسم نشط ويظهر في المتجر", fontWeight = FontWeight.SemiBold)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (isUploading) return@Button
                            val updated = Category(
                                id = category?.id ?: "cat_${System.currentTimeMillis()}",
                                name = name.ifBlank { "قسم جديد" },
                                imageUrl = imageUrl,
                                sortOrder = sortOrderText.toIntOrNull() ?: 1,
                                isActive = isActive,
                                productsCount = category?.productsCount ?: 0
                            )
                            onSave(updated)
                        },
                        enabled = name.isNotBlank() && !isUploading,
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("save_category_btn")
                    ) {
                        Text("حفظ القسم", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}
