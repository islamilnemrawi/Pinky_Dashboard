package com.pinky.dashboard.domain.model

data class Category(
    val id: String,
    val name: String,
    val slug: String = "",
    val imageUrl: String = "",
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val productsCount: Int = 0
)

data class Offer(
    val id: String,
    val title: String,
    val description: String = "",
    val discountText: String = "",
    val imageUrl: String = "",
    val buttonText: String = "تسوقي الآن",
    val targetCategoryId: String? = null,
    val targetCategoryName: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val isBanner: Boolean = false,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
)

enum class DiscountType(val arabicLabel: String) {
    PERCENTAGE("نسبة مئوية (%)"),
    FIXED("مبلغ ثابت (ج.م)")
}

data class Coupon(
    val id: String,
    val code: String,
    val discountType: DiscountType = DiscountType.PERCENTAGE,
    val discountValue: Double,
    val minOrderAmount: Double = 0.0,
    val maxDiscountAmount: Double? = null,
    val isFreeShipping: Boolean = false,
    val usageLimit: Int? = null,
    val usageCount: Int = 0,
    val isFirstOrderOnly: Boolean = false, // خيار خاص "أول طلب فقط"
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
    val isActive: Boolean = true
)

// Pinky Prime Models
enum class PrimeSubscriberStatus(val arabicLabel: String) {
    ACTIVE("نشط"),
    EXPIRING_SOON("ينتهي قريباً"),
    EXPIRED("منتهي")
}

data class PrimeBenefit(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String = "Star",
    val isEnabled: Boolean = true
)

data class PrimeSubscriber(
    val id: String,
    val customerName: String,
    val phone: String,
    val startDate: Long,
    val expiryDate: Long,
    val status: PrimeSubscriberStatus,
    val totalOrdersWithPrime: Int = 0,
    val totalSavedWithPrime: Double = 0.0
)

data class PinkyPrimeSettings(
    val monthlyPrice: Double = 149.0,
    val annualPrice: Double = 1190.0,
    val isServiceActive: Boolean = true,
    val benefits: List<PrimeBenefit> = listOf(
        PrimeBenefit("b1", "شحن مجاني غير محدود", "على جميع الطلبات لأي مكان في مصر بدون حد أدنى"),
        PrimeBenefit("b2", "خصم إضافي 5% حصري", "يطبق تلقائياً على كافة التشكيلات غير المخفضة"),
        PrimeBenefit("b3", "أولوية التجهيز والشحن السريع", "يتم تسليم الطلب خلال 24 ساعة في القاهرة والجيزة"),
        PrimeBenefit("b4", "هدية خاصة مع كل طلب", "عينات وإكسسوارات حصرية مع شحنات المشتركين")
    )
)

data class Customer(
    val id: String,
    val name: String,
    val phone: String,
    val email: String? = null,
    val address: String = "",
    val governorate: String = "",
    val totalOrders: Int = 0,
    val totalSpend: Double = 0.0,
    val firstOrderDate: Long = System.currentTimeMillis(),
    val lastOrderDate: Long = System.currentTimeMillis(),
    val isPrimeMember: Boolean = false
)

data class ShippingGovernorate(
    val id: String,
    val name: String,
    val deliveryPrice: Double,
    val estimatedDays: String = "1-3 أيام",
    val isActive: Boolean = true,
    val centersCount: Int = 0
)

data class ShippingCenter(
    val id: String,
    val governorateId: String,
    val name: String,
    val deliveryPrice: Double,
    val contactNumber: String? = null,
    val isActive: Boolean = true
)

// Payment Methods Configuration
data class PaymentMethodConfig(
    val id: String,
    val key: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean = true,
    val feeOrDiscountText: String = "",
    val statusText: String = "جاهز للتشغيل",
    val isSecretConfigured: Boolean = true
)

// Website Visual Editor Models
enum class DevicePreviewMode(val arabicLabel: String) {
    MOBILE("عرض الموبايل"),
    DESKTOP("عرض الشاشة الكبيرة")
}

data class SectionDisplayConfig(
    val id: String,
    val title: String,
    val isVisible: Boolean = true,
    val sortOrder: Int
)

data class WebsiteConfig(
    val storeName: String = "Pinky Store",
    val announcementBarText: String = "توصيل سريع لجميع محافظات مصر • خصم 10% عند الدفع عبر إنستاباي",
    val showAnnouncementBar: Boolean = true,
    val heroTitle: String = "أحدث صيحات الموضة النسائية بين يديكِ",
    val heroSubtitle: String = "تشكيلة راقية من الفساتين، العبايات، والأطقم العصرية بجودة مصرية استثنائية",
    val heroButtonText: String = "تصفحي المجموعة الجديدة",
    val heroImageUrl: String = "",
    val showHeroBanner: Boolean = true,
    val showAboutSection: Boolean = true,
    val aboutText: String = "بينكي ستور هو براند أزياء مصري نسائي يقدم تصاميم حصرية بجودة أقمشة فائقة وأسعار مناسبة.",
    val whatsappNumber: String = "01099887711",
    val instagramHandle: String = "@pinky_egypt_store",
    val facebookPage: String = "facebook.com/pinkyegstore",
    val primaryColorHex: String = "#F472B6",
    val fontFamily: String = "Cairo / Almarai",
    val sections: List<SectionDisplayConfig> = listOf(
        SectionDisplayConfig("sec_announcement", "شريط التنبيهات العلوي", true, 1),
        SectionDisplayConfig("sec_hero", "البنر الرئيسي (Hero Section)", true, 2),
        SectionDisplayConfig("sec_categories", "أقسام المتجر الدائرية", true, 3),
        SectionDisplayConfig("sec_best_sellers", "المنتجات الأكثر مبيعاً", true, 4),
        SectionDisplayConfig("sec_offers", "عروض وتخفيضات بينكي الحصرية", true, 5),
        SectionDisplayConfig("sec_prime_promo", "بنر الاشتراك في Pinky Prime", true, 6),
        SectionDisplayConfig("sec_reviews", "آراء وتقييمات العملاء", true, 7),
        SectionDisplayConfig("sec_footer", "معلومات المتجر والتواصل والشحن", true, 8)
    ),
    // 1 - Announcement Top Bar Multi-Messages
    val announcementMsg1: String = "أهلاً بكِ في Pinky",
    val announcementMsg2: String = "اختاري منتجاتك واستمتعي بتجربة تسوق مميزة",
    val announcementMsg3: String = "اكتشفي عالم Pinky Prime",
    val announcementBgColor: String = "#381528",
    val announcementTextColor: String = "#FFFFFF",

    // 2 - Header Settings
    val headerLogoUrl: String = "",
    val headerShowMenuBtn: Boolean = true,
    val headerShowAccountBtn: Boolean = true,
    val headerShowCartBtn: Boolean = true,
    val headerShowFavBtn: Boolean = true,
    val headerIconsColor: String = "#F472B6",
    val headerBgColor: String = "#FFFFFF",

    // 3 - Navigation Menu List
    val navigationItems: List<NavigationConfig> = listOf(
        NavigationConfig("nav_home", "الرئيسية", true, 1, "/"),
        NavigationConfig("nav_products", "المنتجات", true, 2, "/products"),
        NavigationConfig("nav_categories", "الأقسام", true, 3, "/categories")
    ),

    // 5 - Categories Header Customize
    val categoriesTitle: String = "أقسام المتجر",
    val categoriesSubtitle: String = "تصفحي مجموعاتنا المتنوعة",
    val categoriesIsVisible: Boolean = true,

    // 6 - Products Section Header Customize
    val productsSectionTitle: String = "قسم المنتجات",
    val productsSectionSubtitle: String = "أحدث المنتجات الحصرية",
    val productsSectionSmallTitle: String = "جديدنا اليوم",
    val productsSectionButtonText: String = "عرض المنتجات / كل المنتجات",

    // 7 - Pinky Prime Customizations
    val primeName: String = "Pinky Prime",
    val primeTitle: String = "عالم من المزايا الحصرية",
    val primeDescription: String = "اشتركي الآن في خدمة بينكي برايم واستمتعي بمميزات حصرية طوال الشهر",
    val primePrice: Double = 300.0,
    val primeDurationDays: Int = 30,
    val primeDiscountPercent: Double = 10.0,
    val primeFreeShippingOrdersCount: Int = 4,
    val primePackagingFeature: String = "تغليف مميز مجاني",
    val primeThemeFeature: String = "ثيم برونزي فخم",
    val primeButtonText: String = "اشتركي الآن",
    val primeIsVisible: Boolean = true,

    // 8 - 4 Features
    val storeFeatures: List<FeatureConfig> = listOf(
        FeatureConfig("feat_1", "shipped", "شحن سريع", "توصيل آمن لجميع المحافظات", true, 1),
        FeatureConfig("feat_2", "shield", "تجربة آمنة", "دفع آمن بالكامل", true, 2),
        FeatureConfig("feat_3", "chat", "خدمة العملاء", "دعم متواصل على مدار الساعة", true, 3),
        FeatureConfig("feat_4", "gift", "تغليف مميز", "نهتم بأدق التفاصيل والجمال", true, 4)
    ),

    // 9 - Instagram Gallery Images
    val instagramImages: List<InstagramImageConfig> = listOf(
        InstagramImageConfig("img_1", "https://picsum.photos/400/400?random=1", 1, true),
        InstagramImageConfig("img_2", "https://picsum.photos/400/400?random=2", 2, true),
        InstagramImageConfig("img_3", "https://picsum.photos/400/400?random=3", 3, true)
    ),

    // 10 - Footer Customizations
    val footerAboutText: String = "بينكي ستور هو خياركِ الأول للأناقة والجمال النسائي المتميز.",
    val footerLinksTitle: String = "روابط هامة",
    val footerWhatsapp: String = "01099887711",
    val footerInstagram: String = "@pinky_egypt_store",
    val footerFacebook: String = "facebook.com/pinkyegstore",
    val footerPaymentIcons: List<String> = listOf("visa", "mastercard", "instapay", "fawry"),
    val footerBottomText: String = "جميع الحقوق محفوظة لمتجر بينكي 2026 🌸",
    val footerCredits: String = "تم التطوير بواسطة Nemrawy"
)

// Notifications Models
enum class NotificationType(val arabicLabel: String) {
    NEW_ORDER("طلب جديد"),
    ORDER_STATUS("تحديث حالة طلب"),
    LOW_STOCK("تنبيه مخزون"),
    NEW_CUSTOMER("عميل جديد"),
    COUPON_EXPIRY("انتهاء كوبون"),
    SYSTEM_ALERT("تنبيه نظام")
}

data class DashboardNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val targetOrderId: String? = null,
    val targetProductId: String? = null
)

// Smart Dashboard Insights
enum class InsightSeverity {
    INFO,
    WARNING,
    SUCCESS
}

data class DashboardInsight(
    val id: String,
    val title: String,
    val description: String,
    val severity: InsightSeverity,
    val actionTitle: String? = null,
    val targetRoute: String? = null
)

// Pinky AI Chat
enum class AiSender {
    USER,
    ASSISTANT
}

data class AiChatMessage(
    val id: String,
    val sender: AiSender,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
