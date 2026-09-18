package com.pinky.dashboard.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SiteCustomizationConfig(
    @Json(name = "announcement") val announcement: AnnouncementConfig? = null,
    @Json(name = "header") val header: HeaderConfig? = null,
    @Json(name = "navigation") val navigation: List<NavigationConfig>? = null,
    @Json(name = "hero") val hero: HeroConfig? = null,
    @Json(name = "categories") val categories: CategoriesConfig? = null,
    @Json(name = "featured") val featured: FeaturedConfig? = null,
    @Json(name = "prime") val prime: PrimeConfig? = null,
    @Json(name = "features") val features: List<FeatureConfig>? = null,
    @Json(name = "instagram") val instagram: InstagramConfig? = null,
    @Json(name = "footer") val footer: FooterConfig? = null,
    @Json(name = "global") val global: GlobalConfig? = null,
    @Json(name = "elements") val elements: Map<String, String>? = null,
    @Json(name = "css") val css: String? = null,
    @Json(name = "customSections") val customSections: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class AnnouncementConfig(
    @Json(name = "message1") val message1: String = "أهلاً بكِ في Pinky",
    @Json(name = "message2") val message2: String = "اختاري منتجاتك واستمتعي بتجربة تسوق مميزة",
    @Json(name = "message3") val message3: String = "اكتشفي عالم Pinky Prime",
    @Json(name = "isVisible") val isVisible: Boolean = true,
    @Json(name = "backgroundColor") val backgroundColor: String = "#381528",
    @Json(name = "textColor") val textColor: String = "#FFFFFF"
)

@JsonClass(generateAdapter = true)
data class HeaderConfig(
    @Json(name = "logoUrl") val logoUrl: String = "",
    @Json(name = "showMenuBtn") val showMenuBtn: Boolean = true,
    @Json(name = "showAccountBtn") val showAccountBtn: Boolean = true,
    @Json(name = "showCartBtn") val showCartBtn: Boolean = true,
    @Json(name = "showFavBtn") val showFavBtn: Boolean = true,
    @Json(name = "iconsColor") val iconsColor: String = "#F472B6",
    @Json(name = "backgroundColor") val backgroundColor: String = "#FFFFFF"
)

@JsonClass(generateAdapter = true)
data class NavigationConfig(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "isVisible") val isVisible: Boolean = true,
    @Json(name = "sortOrder") val sortOrder: Int = 1,
    @Json(name = "link") val link: String = "/"
)

@JsonClass(generateAdapter = true)
data class HeroConfig(
    @Json(name = "slides") val slides: List<HeroSlideConfig> = emptyList()
)

@JsonClass(generateAdapter = true)
data class HeroSlideConfig(
    @Json(name = "id") val id: String,
    @Json(name = "imageUrl") val imageUrl: String,
    @Json(name = "smallText") val smallText: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "description") val description: String = "",
    @Json(name = "buttonText") val buttonText: String = "تسوقي الآن",
    @Json(name = "actionUrl") val actionUrl: String = "/",
    @Json(name = "sortOrder") val sortOrder: Int = 1,
    @Json(name = "isActive") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class CategoriesConfig(
    @Json(name = "title") val title: String = "أقسام المتجر",
    @Json(name = "subtitle") val subtitle: String = "تصفحي مجموعاتنا المتنوعة",
    @Json(name = "isVisible") val isVisible: Boolean = true
)

@JsonClass(generateAdapter = true)
data class FeaturedConfig(
    @Json(name = "title") val title: String = "قسم المنتجات",
    @Json(name = "subtitle") val subtitle: String = "أحدث المنتجات الحصرية",
    @Json(name = "smallTitle") val smallTitle: String = "جديدنا اليوم",
    @Json(name = "buttonText") val buttonText: String = "كل المنتجات"
)

@JsonClass(generateAdapter = true)
data class PrimeConfig(
    @Json(name = "name") val name: String = "Pinky Prime",
    @Json(name = "title") val title: String = "عالم من المزايا الحصرية",
    @Json(name = "description") val description: String = "اشتركي الآن في خدمة بينكي برايم واستمتعي بمميزات حصرية طوال الشهر",
    @Json(name = "price") val price: Double = 300.0,
    @Json(name = "durationDays") val durationDays: Int = 30,
    @Json(name = "discountPercent") val discountPercent: Double = 10.0,
    @Json(name = "freeShippingOrdersCount") val freeShippingOrdersCount: Int = 4,
    @Json(name = "packagingFeature") val packagingFeature: String = "تغليف مميز مجاني",
    @Json(name = "themeFeature") val themeFeature: String = "ثيم برونزي فخم",
    @Json(name = "buttonText") val buttonText: String = "اشتركي الآن",
    @Json(name = "isVisible") val isVisible: Boolean = true
)

@JsonClass(generateAdapter = true)
data class FeatureConfig(
    @Json(name = "id") val id: String,
    @Json(name = "icon") val icon: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "isVisible") val isVisible: Boolean = true,
    @Json(name = "sortOrder") val sortOrder: Int = 1
)

@JsonClass(generateAdapter = true)
data class InstagramConfig(
    @Json(name = "images") val images: List<InstagramImageConfig> = emptyList()
)

@JsonClass(generateAdapter = true)
data class InstagramImageConfig(
    @Json(name = "id") val id: String,
    @Json(name = "imageUrl") val imageUrl: String,
    @Json(name = "sortOrder") val sortOrder: Int = 1,
    @Json(name = "isActive") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class FooterConfig(
    @Json(name = "aboutText") val aboutText: String = "بينكي ستور هو خياركِ الأول للأناقة والجمال النسائي المتميز.",
    @Json(name = "linksTitle") val linksTitle: String = "روابط هامة",
    @Json(name = "whatsapp") val whatsapp: String = "01099887711",
    @Json(name = "instagram") val instagram: String = "@pinky_egypt_store",
    @Json(name = "facebook") val facebook: String = "facebook.com/pinkyegstore",
    @Json(name = "paymentIcons") val paymentIcons: List<String> = listOf("visa", "mastercard", "instapay", "fawry"),
    @Json(name = "bottomText") val bottomText: String = "جميع الحقوق محفوظة لمتجر بينكي 2026 🌸",
    @Json(name = "credits") val credits: String = "تم التطوير بواسطة Nemrawy"
)

@JsonClass(generateAdapter = true)
data class GlobalConfig(
    @Json(name = "storeName") val storeName: String = "Pinky Store",
    @Json(name = "metaTitle") val metaTitle: String = "بينكي ستور - عالم الأناقة والجمال",
    @Json(name = "metaDescription") val metaDescription: String = "تسوّقي أفضل الفساتين والإكسسوارات النسائية الراقية بجودة متميزة وتوصيل سريع لكل مصر.",
    @Json(name = "primaryColor") val primaryColor: String = "#F472B6",
    @Json(name = "backgroundColor") val backgroundColor: String = "#121212",
    @Json(name = "textColor") val textColor: String = "#FFFFFF",
    @Json(name = "announcementColor") val announcementColor: String = "#381528",
    @Json(name = "favicon") val favicon: String = ""
)
