package com.pinky.dashboard.ui.screens.offers

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pinky.dashboard.core.util.Formatters
import com.pinky.dashboard.domain.model.*
import com.pinky.dashboard.ui.components.ConfirmDestructiveDialog
import com.pinky.dashboard.ui.components.EmptyStateCard
import com.pinky.dashboard.ui.theme.PinkPrimary
import com.pinky.dashboard.ui.theme.StatusCancelled

@Composable
fun OffersScreen(
    offers: List<Offer>,
    coupons: List<Coupon>,
    categories: List<Category>,
    onSaveOffer: (Offer) -> Unit,
    onDeleteOffer: (String) -> Unit,
    onSaveCoupon: (Coupon) -> Unit,
    onDeleteCoupon: (String) -> Unit,
    onToggleCouponActive: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Offers/Banners, 1 = Coupons

    var showOfferDialog by remember { mutableStateOf(false) }
    var editingOffer by remember { mutableStateOf<Offer?>(null) }
    var offerToDelete by remember { mutableStateOf<Offer?>(null) }

    var showCouponDialog by remember { mutableStateOf(false) }
    var editingCoupon by remember { mutableStateOf<Coupon?>(null) }
    var couponToDelete by remember { mutableStateOf<Coupon?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        editingOffer = null
                        showOfferDialog = true
                    } else {
                        editingCoupon = null
                        showCouponDialog = true
                    }
                },
                containerColor = PinkPrimary,
                contentColor = Color(0xFF381528),
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = {
                    Text(
                        if (selectedTab == 0) "إضافة بنر / عرض" else "إنشاء كوبون جديد",
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier.testTag("add_offer_coupon_fab")
            )
        },
        modifier = modifier.testTag("offers_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PinkPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("العروض والبنرات (${offers.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Outlined.LocalOffer, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("كوبونات الخصم (${coupons.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Outlined.Discount, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                // Offers & Banners Tab
                if (offers.isEmpty()) {
                    EmptyStateCard(
                        title = "لا توجد عروض أو بنرات حالياً",
                        message = "أضف بنرات ترويجية لعروض المواسم أو التخفيضات الخاصة بمتجر بينكي",
                        icon = Icons.Outlined.LocalOffer,
                        actionButtonText = "إضافة أول بنر",
                        onActionClick = {
                            editingOffer = null
                            showOfferDialog = true
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("offers_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(offers, key = { it.id }) { offer ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("offer_card_${offer.id}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = PinkPrimary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = offer.discountText.ifBlank { "عرض خاص" },
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = PinkPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(onClick = {
                                                editingOffer = offer
                                                showOfferDialog = true
                                            }) {
                                                Icon(Icons.Outlined.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }

                                            IconButton(onClick = { offerToDelete = offer }) {
                                                Icon(Icons.Outlined.Delete, contentDescription = "حذف", tint = StatusCancelled)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = offer.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (offer.description.isNotEmpty()) {
                                        Text(
                                            text = offer.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "القسم المستهدف: ${offer.targetCategoryName ?: "جميع الأقسام"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (offer.isActive) Color(0xFF34D399).copy(alpha = 0.15f) else StatusCancelled.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (offer.isActive) "نشط" else "متوقف",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (offer.isActive) Color(0xFF34D399) else StatusCancelled,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Coupons Tab
                if (coupons.isEmpty()) {
                    EmptyStateCard(
                        title = "لا توجد كوبونات خصم مسجلة",
                        message = "أنشئي كوبونات خصم ترويجية لعملائك لزيادة مبيعات متجر بينكي",
                        icon = Icons.Outlined.Discount,
                        actionButtonText = "إنشاء أول كوبون",
                        onActionClick = {
                            editingCoupon = null
                            showCouponDialog = true
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("coupons_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(coupons, key = { it.id }) { coupon ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("coupon_card_${coupon.id}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = PinkPrimary.copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    text = coupon.code,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                                    color = PinkPrimary,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }

                                            if (coupon.isFirstOrderOnly) {
                                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFBBF24).copy(alpha = 0.2f)) {
                                                    Text("أول طلب فقط ⚡", fontSize = 11.sp, color = Color(0xFFFBBF24), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }

                                            if (coupon.isFreeShipping) {
                                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF34D399).copy(alpha = 0.2f)) {
                                                    Text("شحن مجاني 🚚", fontSize = 11.sp, color = Color(0xFF34D399), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Switch(
                                                checked = coupon.isActive,
                                                onCheckedChange = { onToggleCouponActive(coupon.id, it) }
                                            )

                                            IconButton(onClick = {
                                                editingCoupon = coupon
                                                showCouponDialog = true
                                            }) {
                                                Icon(Icons.Outlined.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }

                                            IconButton(onClick = { couponToDelete = coupon }) {
                                                Icon(Icons.Outlined.Delete, contentDescription = "حذف", tint = StatusCancelled)
                                            }
                                        }
                                    }

                                    Text(
                                        text = "الخصم: ${coupon.discountValue}${if (coupon.discountType == DiscountType.PERCENTAGE) "%" else " ج.م"} • حد أدنى للطلب: ${Formatters.formatCurrencyEgp(coupon.minOrderAmount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "مرات الاستخدام: ${coupon.usageCount}${coupon.usageLimit?.let { " / $it" } ?: ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Text(
                                            text = if (coupon.isActive) "ساري المفعول" else "متوقف مؤقتاً",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (coupon.isActive) Color(0xFF34D399) else StatusCancelled
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

    if (showOfferDialog) {
        OfferEditDialog(
            offer = editingOffer,
            categories = categories,
            onDismiss = { showOfferDialog = false },
            onSave = {
                onSaveOffer(it)
                showOfferDialog = false
            }
        )
    }

    if (showCouponDialog) {
        CouponEditDialog(
            coupon = editingCoupon,
            onDismiss = { showCouponDialog = false },
            onSave = {
                onSaveCoupon(it)
                showCouponDialog = false
            }
        )
    }

    offerToDelete?.let { offer ->
        ConfirmDestructiveDialog(
            title = "حذف العرض / البنر؟",
            message = "هل أنت متأكد من حذف \"${offer.title}\"؟",
            confirmButtonText = "نعم، حذف",
            onConfirm = {
                onDeleteOffer(offer.id)
                offerToDelete = null
            },
            onDismiss = { offerToDelete = null }
        )
    }

    couponToDelete?.let { coupon ->
        ConfirmDestructiveDialog(
            title = "حذف كود الخصم؟",
            message = "هل أنت متأكد من حذف الكوبون \"${coupon.code}\"؟",
            confirmButtonText = "نعم، حذف الكوبون",
            onConfirm = {
                onDeleteCoupon(coupon.id)
                couponToDelete = null
            },
            onDismiss = { couponToDelete = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferEditDialog(
    offer: Offer?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Offer) -> Unit
) {
    var title by remember { mutableStateOf(offer?.title ?: "") }
    var description by remember { mutableStateOf(offer?.description ?: "") }
    var discountText by remember { mutableStateOf(offer?.discountText ?: "خصم 30%") }
    var buttonText by remember { mutableStateOf(offer?.buttonText ?: "تسوقي الآن") }
    var imageUrl by remember { mutableStateOf(offer?.imageUrl ?: "") }
    var selectedCategoryId by remember { mutableStateOf(offer?.targetCategoryId ?: "") }
    var isActive by remember { mutableStateOf(offer?.isActive ?: true) }

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
                    text = if (offer == null) "إضافة بنر / عرض جديد" else "تعديل العرض",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = PinkPrimary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان العرض / البنر *") },
                    placeholder = { Text("مثال: تخفيضات الربيع الكبرى") },
                    modifier = Modifier.fillMaxWidth().testTag("offer_title_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = discountText,
                    onValueChange = { discountText = it },
                    label = { Text("شارة الخصم (Badge)") },
                    placeholder = { Text("خصم حتى 50%") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("تفاصيل العرض") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("رابط صورة البنر") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("العرض نشط حالياً على الموقع", fontWeight = FontWeight.SemiBold)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val cat = categories.firstOrNull { it.id == selectedCategoryId }
                            val updated = Offer(
                                id = offer?.id ?: "off_${System.currentTimeMillis()}",
                                title = title.ifBlank { "عرض خاص" },
                                description = description,
                                discountText = discountText,
                                imageUrl = imageUrl,
                                buttonText = buttonText,
                                targetCategoryId = selectedCategoryId.ifBlank { null },
                                targetCategoryName = cat?.name,
                                sortOrder = offer?.sortOrder ?: 1,
                                isActive = isActive
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("save_offer_btn")
                    ) {
                        Text("حفظ العرض", fontWeight = FontWeight.Bold)
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
fun CouponEditDialog(
    coupon: Coupon?,
    onDismiss: () -> Unit,
    onSave: (Coupon) -> Unit
) {
    var code by remember { mutableStateOf(coupon?.code ?: "") }
    var discountType by remember { mutableStateOf(coupon?.discountType ?: DiscountType.PERCENTAGE) }
    var discountValueText by remember { mutableStateOf(coupon?.discountValue?.toString() ?: "10") }
    var minimumOrderText by remember { mutableStateOf(coupon?.minOrderAmount?.toString() ?: "500") }
    var maxDiscountText by remember { mutableStateOf(coupon?.maxDiscountAmount?.toString() ?: "") }
    var isFirstOrderOnly by remember { mutableStateOf(coupon?.isFirstOrderOnly ?: false) }
    var isFreeShipping by remember { mutableStateOf(coupon?.isFreeShipping ?: false) }
    var isActive by remember { mutableStateOf(coupon?.isActive ?: true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (coupon == null) "إنشاء كوبون خصم جديد" else "تعديل الكوبون",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = PinkPrimary
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("كود الخصم (Promo Code) *") },
                    placeholder = { Text("مثال: PINKY2026") },
                    modifier = Modifier.fillMaxWidth().testTag("coupon_code_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = discountType == DiscountType.PERCENTAGE,
                        onClick = { discountType = DiscountType.PERCENTAGE },
                        label = { Text("نسبة مئوية (%)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = discountType == DiscountType.FIXED,
                        onClick = { discountType = DiscountType.FIXED },
                        label = { Text("مبلغ ثابت (ج.م)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = discountValueText,
                        onValueChange = { discountValueText = it },
                        label = { Text("قيمة الخصم *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = minimumOrderText,
                        onValueChange = { minimumOrderText = it },
                        label = { Text("الحد الأدنى للطلب (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("متاح للطلب الأول للعميلة فقط", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isFirstOrderOnly, onCheckedChange = { isFirstOrderOnly = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("شحن مجاني", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isFreeShipping, onCheckedChange = { isFreeShipping = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val saved = Coupon(
                                id = coupon?.id ?: "coup_${System.currentTimeMillis()}",
                                code = code.ifBlank { "PINKY" },
                                discountType = discountType,
                                discountValue = discountValueText.toDoubleOrNull() ?: 10.0,
                                minOrderAmount = minimumOrderText.toDoubleOrNull() ?: 0.0,
                                maxDiscountAmount = maxDiscountText.toDoubleOrNull(),
                                isFirstOrderOnly = isFirstOrderOnly,
                                isFreeShipping = isFreeShipping,
                                isActive = isActive
                            )
                            onSave(saved)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("save_coupon_btn")
                    ) {
                        Text("حفظ الكوبون", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}
