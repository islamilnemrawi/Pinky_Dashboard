package com.pinky.dashboard.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.pinky.dashboard.domain.model.Permission

enum class DashboardSection(
    val route: String,
    val arabicTitle: String,
    val icon: ImageVector,
    val requiredPermission: Permission? = null,
    val isPrimaryBottomNav: Boolean = false
) {
    HOME("home", "الرئيسية", Icons.Outlined.Dashboard, Permission.VIEW_DASHBOARD, isPrimaryBottomNav = true),
    ORDERS("orders", "الطلبات", Icons.Outlined.ShoppingBag, Permission.MANAGE_ORDERS, isPrimaryBottomNav = true),
    PRODUCTS("products", "المنتجات", Icons.Outlined.Inventory2, Permission.MANAGE_PRODUCTS, isPrimaryBottomNav = true),
    CATEGORIES("categories", "الأقسام", Icons.Outlined.Category, Permission.MANAGE_CATEGORIES),
    OFFERS_COUPONS("offers_coupons", "العروض والكوبونات", Icons.Outlined.LocalOffer, Permission.MANAGE_OFFERS),
    CUSTOMERS("customers", "العملاء", Icons.Outlined.People, Permission.MANAGE_CUSTOMERS, isPrimaryBottomNav = true),
    PRIME("prime", "Pinky Prime", Icons.Outlined.WorkspacePremium, Permission.MANAGE_PRIME),
    SHIPPING("shipping", "الشحن والتوصيل", Icons.Outlined.LocalShipping, Permission.MANAGE_SHIPPING),
    PAYMENTS("payments", "طرق الدفع", Icons.Outlined.Payment, Permission.MANAGE_PAYMENT_METHODS),
    STAFF("staff", "الموظفين والصلاحيات", Icons.Outlined.Badge, Permission.MANAGE_STAFF),
    WEBSITE_EDITOR("website_editor", "تحرير الموقع", Icons.Outlined.Web, Permission.MANAGE_WEBSITE_EDITOR),
    REPORTS("reports", "التقارير والأرباح", Icons.Outlined.BarChart, Permission.VIEW_ANALYTICS),
    NOTIFICATIONS("notifications", "الإشعارات", Icons.Outlined.Notifications, Permission.VIEW_NOTIFICATIONS),
    HEALTH_CHECK("health_check", "فحص النظام والربط", Icons.Outlined.CheckCircle, null),
    SETTINGS("settings", "الإعدادات", Icons.Outlined.Settings, null, isPrimaryBottomNav = true);

    companion object {
        fun fromRoute(route: String): DashboardSection {
            return entries.firstOrNull { it.route == route } ?: HOME
        }
    }
}
