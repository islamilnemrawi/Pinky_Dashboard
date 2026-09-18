package com.pinky.dashboard.ui.screens.website

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.pinky.dashboard.data.remote.SupabaseClient
import com.pinky.dashboard.domain.model.*
import com.pinky.dashboard.ui.theme.PinkPrimary
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebsiteEditorScreen(
    initialConfig: WebsiteConfig,
    categories: List<Category>,
    banners: List<Offer>,
    onSaveConfig: (WebsiteConfig) -> Unit,
    onSaveBanner: (Offer) -> Unit,
    onDeleteBanner: (Offer) -> Unit,
    onSaveCategory: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    var config by remember { mutableStateOf(initialConfig) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Editor, 1 = Live Preview Mockup
    var showSavedMessage by remember { mutableStateOf(false) }

    // Dialog state for each of the 11 sections
    var activeEditingDialog by remember { mutableStateOf<Int?>(null) } // null = none, 1-11 = section dialog index

    // Sync local config with initialConfig when updated from server
    LaunchedEffect(initialConfig) {
        config = initialConfig
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    onSaveConfig(config)
                    showSavedMessage = true
                },
                containerColor = PinkPrimary,
                contentColor = Color(0xFF381528),
                icon = { Icon(Icons.Outlined.Save, contentDescription = null) },
                text = { Text("حفظ ونشر التعديلات", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("save_website_fab")
            )
        },
        snackbarHost = {
            if (showSavedMessage) {
                Snackbar(
                    action = {
                        TextButton(onClick = { showSavedMessage = false }) {
                            Text("حسناً", color = PinkPrimary)
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("تم حفظ إعدادات موقع بينكي بنجاح وستنعكس للمتسوقين فوراً.")
                }
            }
        },
        modifier = modifier.testTag("website_editor_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
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
                                text = "تحرير وتصميم واجهة المتجر",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تحكم في كافة عناصر موقع بينكي وتصميم الأقسام والبنرات بنقرة واحدة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Mode Tabs: Edit vs Live Preview Mockup
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PinkPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("أقسام التحرير", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Outlined.EditNote, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("المعاينة الحية (Live Mockup)", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Outlined.PhoneAndroid, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                // Editor Sections List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "اختر القسم لتعديل محتوياته وتصميمه بالتفصيل:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Card 1: الشريط العلوي
                    item {
                        SectionCard(
                            title = "الشريط العلوي (Announcement Bar)",
                            description = "إعلانات متحركة بألوان مخصصة ومسجات ترويجية متعددة",
                            icon = Icons.Outlined.Campaign,
                            isActive = config.showAnnouncementBar,
                            onToggleActive = { config = config.copy(showAnnouncementBar = it) },
                            onClick = { activeEditingDialog = 1 }
                        )
                    }

                    // Card 2: الهيدر
                    item {
                        SectionCard(
                            title = "الهيدر (Header Settings)",
                            description = "رفع شعار المتجر، أزرار المفضلة والسلة والحساب وخلفية الهيدر",
                            icon = Icons.Outlined.Storefront,
                            isActive = true,
                            onToggleActive = null,
                            onClick = { activeEditingDialog = 2 }
                        )
                    }

                    // Card 3: قائمة الموقع
                    item {
                        SectionCard(
                            title = "قائمة الموقع (Navigation Menu)",
                            description = "تنظيم وترتيب صفحات التنقل والروابط الأساسية للمتجر",
                            icon = Icons.Outlined.MenuOpen,
                            isActive = true,
                            onToggleActive = null,
                            onClick = { activeEditingDialog = 3 }
                        )
                    }

                    // Card 4: البنرات الرئيسية والشرائح
                    item {
                        SectionCard(
                            title = "البنرات والشرائح الرئيسية (Hero Banners)",
                            description = "إضافة وتعديل شرائح البنر الدوار مع الصور والروابط المباشرة",
                            icon = Icons.Outlined.ViewCarousel,
                            isActive = config.showHeroBanner,
                            onToggleActive = { config = config.copy(showHeroBanner = it) },
                            onClick = { activeEditingDialog = 4 }
                        )
                    }

                    // Card 5: قسم الأقسام
                    item {
                        SectionCard(
                            title = "تسميات الأقسام (Categories Layout)",
                            description = "العناوين والعناوين الفرعية لقسم تصفح المنتجات حسب الفئات",
                            icon = Icons.Outlined.Category,
                            isActive = config.categoriesIsVisible,
                            onToggleActive = { config = config.copy(categoriesIsVisible = it) },
                            onClick = { activeEditingDialog = 5 }
                        )
                    }

                    // Card 6: قسم المنتجات
                    item {
                        SectionCard(
                            title = "قسم المنتجات (Products Header)",
                            description = "تعديل عنوان وعنوان فرعي ونصوص قسم المعروضات وزر التصفح",
                            icon = Icons.Outlined.ShoppingBag,
                            isActive = true,
                            onToggleActive = null,
                            onClick = { activeEditingDialog = 6 }
                        )
                    }

                    // Card 7: Pinky Prime
                    item {
                        SectionCard(
                            title = "اشتراك بينكي برايم (Pinky Prime)",
                            description = "تخصيص الباقات والأسعار والمزايا الحصرية للأعضاء",
                            icon = Icons.Outlined.Star,
                            isActive = config.primeIsVisible,
                            onToggleActive = { config = config.copy(primeIsVisible = it) },
                            onClick = { activeEditingDialog = 7 }
                        )
                    }

                    // Card 8: مميزات المتجر الأربعة
                    item {
                        SectionCard(
                            title = "مميزات المتجر الأربعة (Store Features)",
                            description = "المميزات التشغيلية الأربعة: الشحن والضمانات والتواصل والدعم",
                            icon = Icons.Outlined.CardMembership,
                            isActive = true,
                            onToggleActive = null,
                            onClick = { activeEditingDialog = 8 }
                        )
                    }

                    // Card 9: صور الموقع والأنستجرام
                    item {
                        SectionCard(
                            title = "معرض صور الموقع (Instagram Gallery)",
                            description = "تنسيق معرض صور إنستجرام التفاعلي ورفع واستبدال اللقطات",
                            icon = Icons.Outlined.PhotoLibrary,
                            isActive = true,
                            onToggleActive = null,
                            onClick = { activeEditingDialog = 9 }
                        )
                    }

                    // Card 10: الفوتر ومعلومات التواصل
                    item {
                        SectionCard(
                            title = "تذييل الموقع (Footer Settings)",
                            description = "تعديل نبذة المتجر، روابط السوشيال ميديا وحقوق الملكية وطرق الدفع",
                            icon = Icons.Outlined.Info,
                            isActive = true,
                            onToggleActive = null,
                            onClick = { activeEditingDialog = 10 }
                        )
                    }

                    // Card 11: الإعدادات العامة والألوان
                    item {
                        SectionCard(
                            title = "إعدادات عامة وألوان وهوية الموقع",
                            description = "اسم المتجر، تفاصيل السيو والبحث، والخطوط ولون الهوية الأساسي",
                            icon = Icons.Outlined.SettingsSuggest,
                            isActive = true,
                            onToggleActive = null,
                            onClick = { activeEditingDialog = 11 }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            } else {
                // Live Mockup rendering
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    PhoneMockupView(
                        config = config,
                        categories = categories,
                        banners = banners
                    )
                }
            }
        }
    }

    // Handle Active Dialog Render
    activeEditingDialog?.let { dialogIndex ->
        SectionEditDialog(
            index = dialogIndex,
            config = config,
            categoriesList = categories,
            bannersList = banners,
            onSaveConfig = { updated ->
                config = updated
                onSaveConfig(updated)
            },
            onSaveBanner = onSaveBanner,
            onDeleteBanner = onDeleteBanner,
            onSaveCategory = onSaveCategory,
            onDismiss = { activeEditingDialog = null }
        )
    }
}

@Composable
fun SectionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onToggleActive: ((Boolean) -> Unit)?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PinkPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (onToggleActive != null) {
                Switch(
                    checked = isActive,
                    onCheckedChange = onToggleActive,
                    modifier = Modifier.scale(0.85f)
                )
            } else {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// Dialog Controller for editing each section
@Composable
fun SectionEditDialog(
    index: Int,
    config: WebsiteConfig,
    categoriesList: List<Category>,
    bannersList: List<Offer>,
    onSaveConfig: (WebsiteConfig) -> Unit,
    onSaveBanner: (Offer) -> Unit,
    onDeleteBanner: (Offer) -> Unit,
    onSaveCategory: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    var dialogConfig by remember { mutableStateOf(config) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header of dialog
                val (title, icon) = when (index) {
                    1 -> "الشريط العلوي (Announcement Bar)" to Icons.Outlined.Campaign
                    2 -> "الهيدر (Header Settings)" to Icons.Outlined.Storefront
                    3 -> "قائمة الموقع (Navigation Menu)" to Icons.Outlined.MenuOpen
                    4 -> "البنرات والشرائح الرئيسية" to Icons.Outlined.ViewCarousel
                    5 -> "تسميات الأقسام" to Icons.Outlined.Category
                    6 -> "قسم المنتجات المعروضة" to Icons.Outlined.ShoppingBag
                    7 -> "اشتراك بينكي برايم" to Icons.Outlined.Star
                    8 -> "مميزات المتجر الأربعة" to Icons.Outlined.CardMembership
                    9 -> "صور إنستجرام ومعرض الموقع" to Icons.Outlined.PhotoLibrary
                    10 -> "تذييل الموقع والفوتر" to Icons.Outlined.Info
                    11 -> "هوية وإعدادات الموقع العامة" to Icons.Outlined.SettingsSuggest
                    else -> "تعديل البيانات" to Icons.Outlined.Edit
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PinkPrimary.copy(alpha = 0.08f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(icon, contentDescription = null, tint = PinkPrimary)
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                    }
                }

                Divider()

                // Form Scrollable Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    when (index) {
                        1 -> AnnouncementBarForm(dialogConfig, onUpdate = { dialogConfig = it })
                        2 -> HeaderForm(dialogConfig, onUpdate = { dialogConfig = it })
                        3 -> NavigationMenuForm(dialogConfig, onUpdate = { dialogConfig = it })
                        4 -> HeroBannersForm(bannersList, onSaveBanner, onDeleteBanner, categoriesList)
                        5 -> CategoriesLayoutForm(dialogConfig, onUpdate = { dialogConfig = it })
                        6 -> ProductsHeaderForm(dialogConfig, onUpdate = { dialogConfig = it })
                        7 -> PinkyPrimeForm(dialogConfig, onUpdate = { dialogConfig = it })
                        8 -> StoreFeaturesForm(dialogConfig, onUpdate = { dialogConfig = it })
                        9 -> InstagramGalleryForm(dialogConfig, onUpdate = { dialogConfig = it })
                        10 -> FooterSettingsForm(dialogConfig, onUpdate = { dialogConfig = it })
                        11 -> GlobalSettingsForm(dialogConfig, onUpdate = { dialogConfig = it })
                    }
                }

                Divider()

                // Actions Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            onSaveConfig(dialogConfig)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text("تطبيق وحفظ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// FORM COMPOSABLES FOR EACH OF 11 SECTIONS
// ==========================================

@Composable
fun AnnouncementBarForm(config: WebsiteConfig, onUpdate: (WebsiteConfig) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("تفعيل شريط التنبيهات", fontWeight = FontWeight.SemiBold)
            Switch(
                checked = config.showAnnouncementBar,
                onCheckedChange = { onUpdate(config.copy(showAnnouncementBar = it)) }
            )
        }

        OutlinedTextField(
            value = config.announcementMsg1,
            onValueChange = { onUpdate(config.copy(announcementMsg1 = it)) },
            label = { Text("الرسالة الترويجية الأولى") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.announcementMsg2,
            onValueChange = { onUpdate(config.copy(announcementMsg2 = it)) },
            label = { Text("الرسالة الترويجية الثانية") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.announcementMsg3,
            onValueChange = { onUpdate(config.copy(announcementMsg3 = it)) },
            label = { Text("الرسالة الترويجية الثالثة") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = config.announcementBgColor,
                onValueChange = { onUpdate(config.copy(announcementBgColor = it)) },
                label = { Text("لون خلفية الشريط") },
                modifier = Modifier.weight(1f),
                leadingIcon = { ColorDot(colorHex = config.announcementBgColor) }
            )
            OutlinedTextField(
                value = config.announcementTextColor,
                onValueChange = { onUpdate(config.copy(announcementTextColor = it)) },
                label = { Text("لون خط الشريط") },
                modifier = Modifier.weight(1f),
                leadingIcon = { ColorDot(colorHex = config.announcementTextColor) }
            )
        }
    }
}

@Composable
fun HeaderForm(config: WebsiteConfig, onUpdate: (WebsiteConfig) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("شعار الهيدر (Store Logo)", fontWeight = FontWeight.SemiBold)
        ImageUploadSelector(
            imageUrl = config.headerLogoUrl,
            onUrlChanged = { onUpdate(config.copy(headerLogoUrl = it)) },
            bucket = "pinky-products",
            placeholderText = "رفع شعار المتجر"
        )

        Divider()

        Text("التحكم في أزرار الهيدر والوظائف:", fontWeight = FontWeight.SemiBold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("إظهار زر قائمة الهيدر المنسدلة")
            Switch(
                checked = config.headerShowMenuBtn,
                onCheckedChange = { onUpdate(config.copy(headerShowMenuBtn = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("إظهار زر حساب العميل")
            Switch(
                checked = config.headerShowAccountBtn,
                onCheckedChange = { onUpdate(config.copy(headerShowAccountBtn = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("إظهار سلة المشتريات بالهيدر")
            Switch(
                checked = config.headerShowCartBtn,
                onCheckedChange = { onUpdate(config.copy(headerShowCartBtn = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("إظهار قائمة المفضلة بالهيدر")
            Switch(
                checked = config.headerShowFavBtn,
                onCheckedChange = { onUpdate(config.copy(headerShowFavBtn = it)) }
            )
        }

        Divider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = config.headerIconsColor,
                onValueChange = { onUpdate(config.copy(headerIconsColor = it)) },
                label = { Text("لون الأيقونات بالهيدر") },
                modifier = Modifier.weight(1f),
                leadingIcon = { ColorDot(colorHex = config.headerIconsColor) }
            )
            OutlinedTextField(
                value = config.headerBgColor,
                onValueChange = { onUpdate(config.copy(headerBgColor = it)) },
                label = { Text("لون خلفية الهيدر") },
                modifier = Modifier.weight(1f),
                leadingIcon = { ColorDot(colorHex = config.headerBgColor) }
            )
        }
    }
}

@Composable
fun NavigationMenuForm(config: WebsiteConfig, onUpdate: (WebsiteConfig) -> Unit) {
    var itemsList by remember { mutableStateOf(config.navigationItems) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("عناصر قائمة التنقل الأساسية بموقع بينكي:", fontWeight = FontWeight.SemiBold)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(itemsList) { index, nav ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("#${index+1}", fontWeight = FontWeight.Bold, color = PinkPrimary)
                            OutlinedTextField(
                                value = nav.name,
                                onValueChange = { newVal ->
                                    val updated = itemsList.toMutableList().apply {
                                        this[index] = nav.copy(name = newVal)
                                    }
                                    itemsList = updated
                                    onUpdate(config.copy(navigationItems = updated))
                                },
                                label = { Text("اسم الصفحة") },
                                modifier = Modifier.weight(1.5f),
                                maxLines = 1
                            )
                            OutlinedTextField(
                                value = nav.link,
                                onValueChange = { newVal ->
                                    val updated = itemsList.toMutableList().apply {
                                        this[index] = nav.copy(link = newVal)
                                    }
                                    itemsList = updated
                                    onUpdate(config.copy(navigationItems = updated))
                                },
                                label = { Text("الرابط") },
                                modifier = Modifier.weight(2f),
                                maxLines = 1
                            )
                            IconButton(
                                onClick = {
                                    val updated = itemsList.toMutableList().apply {
                                        this[index] = nav.copy(isVisible = !nav.isVisible)
                                    }
                                    itemsList = updated
                                    onUpdate(config.copy(navigationItems = updated))
                                }
                            ) {
                                Icon(
                                    imageVector = if (nav.isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "رؤية",
                                    tint = if (nav.isVisible) PinkPrimary else Color.Gray
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (itemsList.size > 1) {
                                        val updated = itemsList.toMutableList().apply { removeAt(index) }
                                        itemsList = updated
                                        onUpdate(config.copy(navigationItems = updated))
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                val updated = itemsList.toMutableList().apply {
                    add(NavigationConfig("nav_${System.currentTimeMillis()}", "صفحة جديدة", true, size + 1, "/"))
                }
                itemsList = updated
                onUpdate(config.copy(navigationItems = updated))
            },
            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary.copy(alpha = 0.15f), contentColor = PinkPrimary),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("إضافة صفحة تنقل جديدة", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun HeroBannersForm(
    bannersList: List<Offer>,
    onSaveBanner: (Offer) -> Unit,
    onDeleteBanner: (Offer) -> Unit,
    categoriesList: List<Category>
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingBanner by remember { mutableStateOf<Offer?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("بنرات الصفحة الرئيسية النشطة:", fontWeight = FontWeight.SemiBold)
            Button(
                onClick = {
                    editingBanner = Offer(
                        id = "ban_${System.currentTimeMillis()}",
                        title = "عرض جديد رائع",
                        description = "اكتشفي الآن عروض وتخفيضات حصرية متميزة",
                        isBanner = true,
                        isActive = true,
                        sortOrder = bannersList.size + 1
                    )
                    showAddDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("إضافة بنر")
            }
        }

        if (bannersList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد بنرات حالياً. اضغط على 'إضافة بنر' لإنشاء أول شريحة.", color = Color.Gray, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(bannersList) { banner ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = banner.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray.copy(alpha = 0.1f)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(banner.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(banner.description, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.Gray)
                            }
                            IconButton(
                                onClick = {
                                    editingBanner = banner
                                    showAddDialog = true
                                }
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "تعديل", tint = PinkPrimary)
                            }
                            IconButton(
                                onClick = { onDeleteBanner(banner) }
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog && editingBanner != null) {
        var localBanner by remember { mutableStateOf(editingBanner!!) }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.8f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PinkPrimary.copy(alpha = 0.08f))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إعداد شريحة البنر الرئيسي", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showAddDialog = false }) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("صورة البنر (Hero Slide Image)", fontWeight = FontWeight.SemiBold)
                        ImageUploadSelector(
                            imageUrl = localBanner.imageUrl,
                            onUrlChanged = { localBanner = localBanner.copy(imageUrl = it) },
                            bucket = "pinky-banners",
                            placeholderText = "رفع صورة البنر الرئيسي"
                        )

                        OutlinedTextField(
                            value = localBanner.title,
                            onValueChange = { localBanner = localBanner.copy(title = it) },
                            label = { Text("العنوان الرئيسي للبنر") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = localBanner.description,
                            onValueChange = { localBanner = localBanner.copy(description = it) },
                            label = { Text("العنوان الفرعي للبنر / الوصف") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = localBanner.buttonText,
                            onValueChange = { localBanner = localBanner.copy(buttonText = it) },
                            label = { Text("نص زر الإجراء (Button Text)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("تفعيل البنر")
                            Switch(
                                checked = localBanner.isActive,
                                onCheckedChange = { localBanner = localBanner.copy(isActive = it) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                    ) {
                        OutlinedButton(onClick = { showAddDialog = false }) {
                            Text("إلغاء")
                        }
                        Button(
                            onClick = {
                                onSaveBanner(localBanner)
                                showAddDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528))
                        ) {
                            Text("حفظ الشريحة")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoriesLayoutForm(config: WebsiteConfig, onUpdate: (WebsiteConfig) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("إظهار قسم الفئات الدائرية بالموقع", fontWeight = FontWeight.SemiBold)
            Switch(
                checked = config.categoriesIsVisible,
                onCheckedChange = { onUpdate(config.copy(categoriesIsVisible = it)) }
            )
        }

        OutlinedTextField(
            value = config.categoriesTitle,
            onValueChange = { onUpdate(config.copy(categoriesTitle = it)) },
            label = { Text("عنوان القسم بموقع الويب") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.categoriesSubtitle,
            onValueChange = { onUpdate(config.copy(categoriesSubtitle = it)) },
            label = { Text("العنوان الفرعي للقسم") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ProductsHeaderForm(config: WebsiteConfig, onUpdate: (WebsiteConfig) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedTextField(
            value = config.productsSectionSmallTitle,
            onValueChange = { onUpdate(config.copy(productsSectionSmallTitle = it)) },
            label = { Text("العلامة الصغيرة العلوية (مثال: جديدنا اليوم)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.productsSectionTitle,
            onValueChange = { onUpdate(config.copy(productsSectionTitle = it)) },
            label = { Text("العنوان الرئيسي لقسم المنتجات") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.productsSectionSubtitle,
            onValueChange = { onUpdate(config.copy(productsSectionSubtitle = it)) },
            label = { Text("الوصف الفرعي لقسم المنتجات") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.productsSectionButtonText,
            onValueChange = { onUpdate(config.copy(productsSectionButtonText = it)) },
            label = { Text("نص زر عرض المنتجات") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PinkyPrimeForm(config: WebsiteConfig, onUpdate: (WebsiteConfig) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("عرض بنر اشتراك برايم في الصفحة الرئيسية", fontWeight = FontWeight.SemiBold)
            Switch(
                checked = config.primeIsVisible,
                onCheckedChange = { onUpdate(config.copy(primeIsVisible = it)) }
            )
        }

        OutlinedTextField(
            value = config.primeName,
            onValueChange = { onUpdate(config.copy(primeName = it)) },
            label = { Text("اسم الباقة (العلامة التجارية)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.primeTitle,
            onValueChange = { onUpdate(config.copy(primeTitle = it)) },
            label = { Text("عنوان البنر الرئيسي") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.primeDescription,
            onValueChange = { onUpdate(config.copy(primeDescription = it)) },
            label = { Text("وصف اشتراك برايم والمميزات") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = config.primePrice.toString(),
                onValueChange = { onUpdate(config.copy(primePrice = it.toDoubleOrNull() ?: 0.0)) },
                label = { Text("سعر الاشتراك الشهري (جنيه)") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = config.primeDurationDays.toString(),
                onValueChange = { onUpdate(config.copy(primeDurationDays = it.toIntOrNull() ?: 30)) },
                label = { Text("مدة الصلاحية بالأيام") },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = config.primeDiscountPercent.toString(),
                onValueChange = { onUpdate(config.copy(primeDiscountPercent = it.toDoubleOrNull() ?: 0.0)) },
                label = { Text("نسبة الخصم الإضافية للأعضاء (%)") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = config.primeFreeShippingOrdersCount.toString(),
                onValueChange = { onUpdate(config.copy(primeFreeShippingOrdersCount = it.toIntOrNull() ?: 4)) },
                label = { Text("عدد مرات الشحن المجاني شهرياً") },
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = config.primePackagingFeature,
            onValueChange = { onUpdate(config.copy(primePackagingFeature = it)) },
            label = { Text("ميزة التغليف للأعضاء (مثال: تغليف فخم مجاني)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.primeThemeFeature,
            onValueChange = { onUpdate(config.copy(primeThemeFeature = it)) },
            label = { Text("ميزة الهوية والمظهر للأعضاء") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.primeButtonText,
            onValueChange = { onUpdate(config.copy(primeButtonText = it)) },
            label = { Text("نص زر الاشتراك") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun StoreFeaturesForm(config: WebsiteConfig, onUpdate: (WebsiteConfig) -> Unit) {
    var featuresList by remember { mutableStateOf(config.storeFeatures) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("تعديل المميزات الأربعة المتواجدة أسفل الصفحة الرئيسية:", fontWeight = FontWeight.SemiBold)

        featuresList.forEachIndexed { index, feat ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الميزة #${index + 1}:", fontWeight = FontWeight.Bold, color = PinkPrimary)
                        Switch(
                            checked = feat.isVisible,
                            onCheckedChange = { newVal ->
                                val updated = featuresList.toMutableList().apply {
                                    this[index] = feat.copy(isVisible = newVal)
                                }
                                featuresList = updated
                                onUpdate(config.copy(storeFeatures = updated))
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }

                    OutlinedTextField(
                        value = feat.title,
                        onValueChange = { newVal ->
                            val updated = featuresList.toMutableList().apply {
                                this[index] = feat.copy(title = newVal)
                            }
                            featuresList = updated
                            onUpdate(config.copy(storeFeatures = updated))
                        },
                        label = { Text("عنوان الميزة") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = feat.description,
                        onValueChange = { newVal ->
                            val updated = featuresList.toMutableList().apply {
                                this[index] = feat.copy(description = newVal)
                            }
                            featuresList = updated
                            onUpdate(config.copy(storeFeatures = updated))
                        },
                        label = { Text("الوصف المختصر") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun InstagramGalleryForm(config: WebsiteConfig, onUpdate: (WebsiteConfig) -> Unit) {
    var itemsList by remember { mutableStateOf(config.instagramImages) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("معرض صور موقع الويب المخصص:", fontWeight = FontWeight.SemiBold)
            Button(
                onClick = {
                    val updated = itemsList.toMutableList().apply {
                        add(InstagramImageConfig("img_${System.currentTimeMillis()}", "https://picsum.photos/400/400?random=${System.currentTimeMillis()}", size + 1, true))
                    }
                    itemsList = updated
                    onUpdate(config.copy(instagramImages = updated))
                },
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة صورة")
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(itemsList) { index, img ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("#${index + 1}", fontWeight = FontWeight.Bold, color = PinkPrimary)

                            Box(modifier = Modifier.weight(1f)) {
                                ImageUploadSelector(
                                    imageUrl = img.imageUrl,
                                    onUrlChanged = { newVal ->
                                        val updated = itemsList.toMutableList().apply {
                                            this[index] = img.copy(imageUrl = newVal)
                                        }
                                        itemsList = updated
                                        onUpdate(config.copy(instagramImages = updated))
                                    },
                                    bucket = "pinky-banners",
                                    placeholderText = "رفع واستبدال الصورة"
                                )
                            }

                            IconButton(
                                onClick = {
                                    val updated = itemsList.toMutableList().apply {
                                        this[index] = img.copy(isActive = !img.isActive)
                                    }
                                    itemsList = updated
                                    onUpdate(config.copy(instagramImages = updated))
                                }
                            ) {
                                Icon(
                                    imageVector = if (img.isActive) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    tint = if (img.isActive) PinkPrimary else Color.Gray
                                )
                            }

                            IconButton(
                                onClick = {
                                    val updated = itemsList.toMutableList().apply { removeAt(index) }
                                    itemsList = updated
                                    onUpdate(config.copy(instagramImages = updated))
                                }
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FooterSettingsForm(config: WebsiteConfig, onUpdate: (WebsiteConfig) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedTextField(
            value = config.footerAboutText,
            onValueChange = { onUpdate(config.copy(footerAboutText = it)) },
            label = { Text("نبذة قصيرة عن المتجر للفوتر") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        OutlinedTextField(
            value = config.footerLinksTitle,
            onValueChange = { onUpdate(config.copy(footerLinksTitle = it)) },
            label = { Text("عنوان عمود الروابط الهامة بالفوتر") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.footerBottomText,
            onValueChange = { onUpdate(config.copy(footerBottomText = it)) },
            label = { Text("نص حقوق الملكية أسفل الموقع") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.footerCredits,
            onValueChange = { onUpdate(config.copy(footerCredits = it)) },
            label = { Text("المطور وائتمانات الفوتر") },
            modifier = Modifier.fillMaxWidth()
        )

        Divider()

        Text("روابط التواصل الاجتماعي ومبيعات الدعم:", fontWeight = FontWeight.SemiBold)

        OutlinedTextField(
            value = config.footerWhatsapp,
            onValueChange = { onUpdate(config.copy(footerWhatsapp = it)) },
            label = { Text("رقم الواتساب للدعم والطلبات") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.footerInstagram,
            onValueChange = { onUpdate(config.copy(footerInstagram = it)) },
            label = { Text("اسم مستخدم إنستجرام") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.footerFacebook,
            onValueChange = { onUpdate(config.copy(footerFacebook = it)) },
            label = { Text("رابط صفحة فيسبوك") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun GlobalSettingsForm(config: WebsiteConfig, onUpdate: (WebsiteConfig) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedTextField(
            value = config.storeName,
            onValueChange = { onUpdate(config.copy(storeName = it)) },
            label = { Text("اسم المتجر الرئيسي") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.fontFamily,
            onValueChange = { onUpdate(config.copy(fontFamily = it)) },
            label = { Text("خط موقع الويب الرئيسي (Font Family)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.primaryColorHex,
            onValueChange = { onUpdate(config.copy(primaryColorHex = it)) },
            label = { Text("اللون الأساسي للهوية (Hex Code)") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { ColorDot(colorHex = config.primaryColorHex) }
        )

        Divider()

        Text("التهيئة لمحركات البحث (SEO Meta Tags):", fontWeight = FontWeight.SemiBold)

        OutlinedTextField(
            value = config.storeName, // Bind or use helper
            onValueChange = { onUpdate(config.copy(storeName = it)) },
            label = { Text("عنوان السيو الترويجي (Meta Title)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.aboutText,
            onValueChange = { onUpdate(config.copy(aboutText = it)) },
            label = { Text("الوصف لمحركات البحث (Meta Description)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
    }
}

// ==========================================
// HELPER COMPOSABLES & UTILS
// ==========================================

@Composable
fun ColorDot(colorHex: String) {
    val parsedColor = remember(colorHex) {
        try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            PinkPrimary
        }
    }
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(parsedColor)
            .border(1.dp, Color.LightGray, CircleShape)
    )
}

@Composable
fun ImageUploadSelector(
    imageUrl: String,
    onUrlChanged: (String) -> Unit,
    bucket: String,
    placeholderText: String
) {
    val context = LocalContext.current
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var uploadSuccess by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploading = true
            uploadError = null
            uploadSuccess = false

            // Perform async upload to Supabase storage
            val coroutineScope = kotlinx.coroutines.MainScope()
            coroutineScope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val mediaType = "image/jpeg".toMediaTypeOrNull()
                        val requestBody = okhttp3.RequestBody.create(mediaType, bytes)

                        val fileName = "uploads/${System.currentTimeMillis()}_${uri.lastPathSegment ?: "image"}.jpg"

                        val response = SupabaseClient.service.uploadStorageFile(
                            bucket = bucket,
                            path = fileName,
                            file = requestBody,
                            contentType = "image/jpeg"
                        )

                        if (response.isSuccessful) {
                            val publicUrl = "${SupabaseClient.supabaseUrl}/storage/v1/object/public/$bucket/$fileName"
                            onUrlChanged(publicUrl)
                            uploadSuccess = true
                        } else {
                            val errBody = response.errorBody()?.string() ?: ""
                            android.util.Log.e("SupabaseDiagnostic", "Upload error: $errBody")
                            uploadError = "فشل الرفع السحابي: ${response.code()}"
                        }
                    } else {
                        uploadError = "فشل في قراءة ملف الصورة"
                    }
                } catch (e: Exception) {
                    uploadError = "حدث خطأ غير متوقع: ${e.localizedMessage}"
                } finally {
                    isUploading = false
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (imageUrl.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { onUrlChanged("") },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .size(24.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "إزالة", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد صورة محددة", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary.copy(alpha = 0.15f), contentColor = PinkPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(placeholderText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PinkPrimary)
                } else if (uploadSuccess) {
                    Text("تم الرفع بنجاح 🌸", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else if (uploadError != null) {
                    Text(uploadError!!, color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                }
            }
        }
    }
}

// ==========================================
// PHONE REAL-TIME SITE MOCKUP
// ==========================================

@Composable
fun PhoneMockupView(
    config: WebsiteConfig,
    categories: List<Category>,
    banners: List<Offer>
) {
    Card(
        modifier = Modifier
            .widthIn(max = 380.dp)
            .fillMaxHeight(0.95f),
        shape = RoundedCornerShape(36.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(4.dp, Color(0xFF1E1B1D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Speaker bar
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Phone Inner Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Dynamic Announcement Bar
                if (config.showAnnouncementBar) {
                    val parsedBg = remember(config.announcementBgColor) {
                        try { Color(android.graphics.Color.parseColor(config.announcementBgColor)) } catch (e: Exception) { Color(0xFF381528) }
                    }
                    val parsedText = remember(config.announcementTextColor) {
                        try { Color(android.graphics.Color.parseColor(config.announcementTextColor)) } catch (e: Exception) { Color.White }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(parsedBg)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = config.announcementMsg1,
                            color = parsedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 2. Custom Styled Header
                val headerBg = remember(config.headerBgColor) {
                    try { Color(android.graphics.Color.parseColor(config.headerBgColor)) } catch (e: Exception) { Color.White }
                }
                val headerIconsColor = remember(config.headerIconsColor) {
                    try { Color(android.graphics.Color.parseColor(config.headerIconsColor)) } catch (e: Exception) { PinkPrimary }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (config.headerLogoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = config.headerLogoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .height(28.dp)
                                    .widthIn(max = 100.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(headerIconsColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("P", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text(config.storeName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (config.headerShowFavBtn) {
                            Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = headerIconsColor, modifier = Modifier.size(18.dp))
                        }
                        if (config.headerShowCartBtn) {
                            Icon(Icons.Outlined.ShoppingBag, contentDescription = null, tint = headerIconsColor, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 3. Navigation horizontal scroll
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(config.navigationItems.filter { it.isVisible }) { nav ->
                        Text(
                            text = nav.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = headerIconsColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Hero Banner Slides Slider Mockup
                if (config.showHeroBanner) {
                    val activeBanners = banners.filter { it.isActive }
                    if (activeBanners.isNotEmpty()) {
                        val firstBanner = activeBanners.first()
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .padding(horizontal = 10.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (firstBanner.imageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = firstBanner.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.45f))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(headerIconsColor.copy(alpha = 0.2f))
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = firstBanner.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    if (firstBanner.description.isNotEmpty()) {
                                        Text(
                                            text = firstBanner.description,
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.85f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Button(
                                        onClick = {},
                                        colors = ButtonDefaults.buttonColors(containerColor = headerIconsColor, contentColor = Color.White),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text(firstBanner.buttonText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        // Fallback static Hero
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(horizontal = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = headerIconsColor.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(config.heroTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = headerIconsColor)
                                Text(config.heroSubtitle, fontSize = 10.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Circular Categories Scroll Mockup
                if (config.categoriesIsVisible) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(config.categoriesTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(config.categoriesSubtitle, fontSize = 9.sp, color = Color.Gray)

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(categories.filter { it.isActive }) { cat ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color.Gray.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (cat.imageUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = cat.imageUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Filled.Category, contentDescription = null, tint = headerIconsColor, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Text(cat.name, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 6. Featured Products Section Header Mockup
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                ) {
                    Text(config.productsSectionSmallTitle, fontSize = 8.sp, color = headerIconsColor, fontWeight = FontWeight.Bold)
                    Text(config.productsSectionTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(config.productsSectionSubtitle, fontSize = 9.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulated product grid row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("فستان ليلى الأنيق", "عباية روزا الفاخرة").forEach { pName ->
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.LightGray.copy(alpha = 0.3f))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(pName, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("450 جنيه", fontSize = 9.sp, color = headerIconsColor)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 7. Pinky Prime Promo Banner Mockup
                if (config.primeIsVisible) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBF4E6)),
                        border = BorderStroke(1.dp, Color(0xFFE5C158))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(config.primeName, color = Color(0xFFB45309), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(config.primeTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF78350F))
                            Text(config.primeDescription, fontSize = 9.sp, color = Color(0xFF78350F).copy(alpha = 0.8f), textAlign = TextAlign.Center)
                            Button(
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706), contentColor = Color.White),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text(config.primeButtonText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 8. Custom 4 Features block
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    config.storeFeatures.filter { it.isVisible }.take(4).forEach { feat ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(headerIconsColor.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (feat.icon) {
                                        "shipped" -> Icons.Filled.LocalShipping
                                        "shield" -> Icons.Filled.Shield
                                        "chat" -> Icons.Filled.Chat
                                        else -> Icons.Filled.CardGiftcard
                                    },
                                    contentDescription = null,
                                    tint = headerIconsColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(feat.title, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 9. Instagram Gallery Images Mockup
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                ) {
                    Text("معرض لقطات من المتجر 🌸", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        config.instagramImages.filter { it.isActive }.take(3).forEach { img ->
                            AsyncImage(
                                model = img.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 10. Custom Footer
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(config.storeName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(config.footerAboutText, color = Color.Gray, fontSize = 8.sp, textAlign = TextAlign.Center)
                        Text(config.footerBottomText, color = Color.Gray, fontSize = 7.sp)
                        Text(config.footerCredits, color = headerIconsColor, fontSize = 7.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
