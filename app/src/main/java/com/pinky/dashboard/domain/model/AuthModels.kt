package com.pinky.dashboard.domain.model

enum class UserRole(val arabicLabel: String) {
    OWNER("المالك"),
    ADMIN("مدير النظام"),
    EMPLOYEE("موظف")
}

enum class Permission(val arabicLabel: String, val description: String) {
    VIEW_DASHBOARD("عرض الرئيسية", "الاطلاع على إحصائيات لوحة التحكم"),
    MANAGE_ORDERS("إدارة الطلبات", "عرض والبحث وتحديث حالات طلبات المتجر"),
    UPDATE_ORDER_STATUS("تحديث حالة الطلب", "تغيير حالة الطلبات ومزامنتها"),
    MANAGE_PRODUCTS("إدارة المنتجات", "إضافة وتعديل وحذف المنتجات"),
    VIEW_WHOLESALE_PRICES("عرض أسعار الجملة", "الاطلاع على سعر التكلفة بالجملة"),
    VIEW_INTERNAL_CODES("عرض الأكواد الداخلية", "الاطلاع على كود المنتج وSKU"),
    MANAGE_CATEGORIES("إدارة الأقسام", "إضافة وتعديل أقسام المتجر"),
    MANAGE_OFFERS("إدارة العروض والبنرات", "إنشاء وتعديل العروض والبنرات"),
    MANAGE_COUPONS("إدارة الكوبونات", "إنشاء وتعديل قسائم الخصم"),
    MANAGE_CUSTOMERS("إدارة العملاء", "الاطلاع على بيانات وسجل العملاء"),
    MANAGE_PRIME("إدارة Pinky Prime", "التحكم في اشتراكات ومزايا برايم"),
    MANAGE_SHIPPING("إدارة الشحن والتوصيل", "تعديل أسعار ومحافظات ومراكز الشحن"),
    MANAGE_PAYMENT_METHODS("إدارة طرق الدفع", "تفعيل وتعطيل وسائل الدفع المعتمدة"),
    MANAGE_STAFF("إدارة الموظفين والصلاحيات", "إضافة وتعديل صلاحيات فريق العمل"),
    MANAGE_WEBSITE_EDITOR("تحرير الموقع والمظهر", "التعديل البصري على واجهة المتجر والبنرات"),
    VIEW_ANALYTICS("عرض التقارير والتحليلات", "الاطلاع على تقارير المبيعات وحركة الطلبات"),
    VIEW_PROFIT("عرض الأرباح الصافية", "الاطلاع على صافي الأرباح المالية وحساب التكلفة"),
    VIEW_NOTIFICATIONS("مركز الإشعارات", "استقبال وتتبع تنبيهات المتجر"),
    MANAGE_SETTINGS("إدارة الإعدادات", "التحكم في إعدادات التطبيق والمزامنة");

    companion object {
        fun defaultPermissionsFor(role: UserRole): Set<Permission> {
            return when (role) {
                UserRole.OWNER -> entries.toSet()
                UserRole.ADMIN -> setOf(
                    VIEW_DASHBOARD,
                    MANAGE_ORDERS,
                    UPDATE_ORDER_STATUS,
                    MANAGE_PRODUCTS,
                    VIEW_INTERNAL_CODES,
                    MANAGE_CATEGORIES,
                    MANAGE_OFFERS,
                    MANAGE_COUPONS,
                    MANAGE_CUSTOMERS,
                    MANAGE_PRIME,
                    MANAGE_SHIPPING,
                    MANAGE_PAYMENT_METHODS,
                    MANAGE_WEBSITE_EDITOR,
                    VIEW_ANALYTICS,
                    VIEW_NOTIFICATIONS,
                    MANAGE_SETTINGS
                )
                UserRole.EMPLOYEE -> setOf(
                    VIEW_DASHBOARD,
                    MANAGE_ORDERS,
                    UPDATE_ORDER_STATUS,
                    MANAGE_CUSTOMERS,
                    VIEW_NOTIFICATIONS
                )
            }
        }
    }
}

data class AdminUser(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val avatarUrl: String? = null,
    val permissions: Set<Permission> = Permission.defaultPermissionsFor(role)
) {
    fun hasPermission(permission: Permission): Boolean {
        return role == UserRole.OWNER || permissions.contains(permission)
    }

    val canViewWholesale: Boolean get() = hasPermission(Permission.VIEW_WHOLESALE_PRICES)
    val canViewInternalCode: Boolean get() = hasPermission(Permission.VIEW_INTERNAL_CODES)
    val canViewProfit: Boolean get() = hasPermission(Permission.VIEW_PROFIT)
}

data class StaffMember(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val permissions: Set<Permission>,
    val isActive: Boolean = true,
    val lastActive: Long = System.currentTimeMillis()
)
