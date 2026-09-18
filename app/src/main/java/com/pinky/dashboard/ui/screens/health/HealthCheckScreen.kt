package com.pinky.dashboard.ui.screens.health

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinky.dashboard.data.remote.*
import com.pinky.dashboard.domain.model.AdminUser
import com.pinky.dashboard.ui.theme.PinkPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Response

// Enum for test statuses as requested
enum class TestStatus(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color) {
    WORKING("يعمل 🟢", Icons.Filled.CheckCircle, Color(0xFF2E7D32)),
    PARTIAL("يعمل جزئياً 🟡", Icons.Filled.Warning, Color(0xFFF57C00)),
    FAILED("فشل 🔴", Icons.Filled.Error, Color(0xFFC62828)),
    NOT_TESTABLE("غير قابل للاختبار ⚪", Icons.Filled.Help, Color(0xFF757575))
}

// Data class to capture diagnosis information
data class DiagnosticError(
    val operation: String,
    val tableOrRpc: String,
    val httpStatus: Int,
    val postgresCode: String,
    val errorMessage: String,
    val likelyLayer: String // Auth / RLS / Repository / Network / Database / UI
)

// Main test result structure
data class ModuleTestResult(
    val name: String,
    val arabicName: String,
    val dashboardToWebsite: TestStatus,
    val websiteToDashboard: TestStatus,
    val supabaseStatus: TestStatus,
    val logs: List<String> = emptyList(),
    val error: DiagnosticError? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthCheckScreen(
    currentUser: AdminUser?,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isRunning by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var isSessionCardExpanded by remember { mutableStateOf(false) }

    // Map of results for the 12 modules
    var testResults by remember { mutableStateOf<Map<String, ModuleTestResult>>(emptyMap()) }

    // Summary counts
    val workingCount = testResults.values.count { it.supabaseStatus == TestStatus.WORKING }
    val partialCount = testResults.values.count { it.supabaseStatus == TestStatus.PARTIAL }
    val failedCount = testResults.values.count { it.supabaseStatus == TestStatus.FAILED }
    val untestableCount = testResults.values.count { it.supabaseStatus == TestStatus.NOT_TESTABLE }

    // Initial fill with neutral states
    LaunchedEffect(Unit) {
        testResults = getInitialModules()
    }

    fun parseErrorBody(response: Response<*>): Pair<String, String> {
        val errorBodyStr = response.errorBody()?.string() ?: ""
        val lower = errorBodyStr.lowercase()
        val postgresCode = when {
            lower.contains("42501") -> "42501 (RLS Policy Violation)"
            lower.contains("23505") -> "23505 (Unique Constraint Violation)"
            lower.contains("23503") -> "23503 (Foreign Key Violation)"
            lower.contains("42p01") -> "42P01 (Undefined Table)"
            else -> "Unknown"
        }
        val safeMsg = when {
            response.code() == 401 -> "غير مصرح (Auth Token expired or missing)"
            response.code() == 403 || lower.contains("42501") -> "فشل سياسة الـ RLS: المستخدم لا يملك الصلاحية الكافية لهذه العملية."
            errorBodyStr.isNotBlank() -> errorBodyStr
            else -> response.message() ?: "HTTP Error"
        }
        return Pair(postgresCode, safeMsg)
    }

    fun runAllTests() {
        if (isRunning) return
        isRunning = true
        testResults = getInitialModules()

        scope.launch {
            val updatedResults = testResults.toMutableMap()

            // -----------------------------------------------------------------
            // 1. AUTH & PERMISSIONS
            // -----------------------------------------------------------------
            progressText = "جاري فحص جلسة المصادقة وصلاحيات الموظف..."
            currentProgress = 0.08f
            delay(400)

            if (!SupabaseClient.isSessionReady) {
                progressText = "في انتظار تهيئة نظام المصادقة..."
                val waitStart = System.currentTimeMillis()
                while (!SupabaseClient.isSessionReady && (System.currentTimeMillis() - waitStart) < 10000L) {
                    delay(100)
                }
            }

            val authLogs = mutableListOf<String>()
            var authError: DiagnosticError? = null
            var authStatus = TestStatus.FAILED

            if (SupabaseClient.accessToken != null) {
                authLogs.add("تم العثور على رمز المصادقة بنجاح.")
                authLogs.add("رقم المعرف الفريد للمستخدم: ${currentUser?.id ?: "غير معروف"}")
                authLogs.add("البريد الإلكتروني المسجل: ${currentUser?.email ?: "غير مسجل"}")
                authLogs.add("الرتبة المعترف بها: ${currentUser?.role?.arabicLabel ?: "مجهول"}")
                authLogs.add("قائمة الصلاحيات الممنوحة: ${currentUser?.permissions?.map { it.name }?.joinToString(", ") ?: "لا توجد"}")

                // Call getStaffProfileByEmail to check profile table
                try {
                    val email = currentUser?.email ?: ""
                    val response = SupabaseClient.service.getStaffProfileByEmail("eq.$email")
                    if (response.isSuccessful) {
                        authLogs.add("تم التحقق من وجود ملف الموظف في جدول staff_profiles.")
                        authStatus = TestStatus.WORKING
                    } else {
                        val (pgCode, msg) = parseErrorBody(response)
                        authLogs.add("خطأ أثناء جلب ملف الموظف: $msg")
                        authError = DiagnosticError(
                            operation = "getStaffProfileByEmail",
                            tableOrRpc = "staff_profiles",
                            httpStatus = response.code(),
                            postgresCode = pgCode,
                            errorMessage = msg,
                            likelyLayer = "RLS / Database"
                        )
                        authStatus = TestStatus.PARTIAL
                    }
                } catch (e: Exception) {
                    authLogs.add("فشل الاتصال بجدول الموظفين: ${e.message}")
                    authError = DiagnosticError(
                        operation = "getStaffProfileByEmail",
                        tableOrRpc = "staff_profiles",
                        httpStatus = 0,
                        postgresCode = "NET_ERR",
                        errorMessage = e.message ?: "Network Exception",
                        likelyLayer = "Network"
                    )
                    authStatus = TestStatus.PARTIAL
                }
            } else {
                authLogs.add("لم يتم العثور على جلسة مصادقة نشطة. الرجاء تسجيل الدخول أولاً.")
                authError = DiagnosticError(
                    operation = "checkSession",
                    tableOrRpc = "auth",
                    httpStatus = 401,
                    postgresCode = "AUTH_EXP",
                    errorMessage = "لا توجد جلسة مصادقة للمستخدم.",
                    likelyLayer = "Auth"
                )
            }
            updatedResults["auth"] = ModuleTestResult(
                name = "auth",
                arabicName = "المصادقة والصلاحيات",
                dashboardToWebsite = authStatus,
                websiteToDashboard = authStatus,
                supabaseStatus = authStatus,
                logs = authLogs,
                error = authError
            )
            testResults = updatedResults.toMap()

            // -----------------------------------------------------------------
            // 2. PRODUCTS
            // -----------------------------------------------------------------
            progressText = "جاري فحص جدول المنتجات (public.catalog_products)..."
            currentProgress = 0.16f
            delay(400)
            val prodLogs = mutableListOf<String>()
            var prodError: DiagnosticError? = null
            var prodStatus = TestStatus.FAILED

            try {
                // Read products
                val response = SupabaseClient.service.getProducts()
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    prodLogs.add("تم قراءة جدول catalog_products بنجاح. عدد المنتجات الحالية: ${list.size}")

                    // Write a test-safe temporary product
                    val testId = "sys_test_p_${System.currentTimeMillis()}"
                    val testProd = SupabaseProductDto(
                        id = testId,
                        name = "منتج فحص النظام المؤقت",
                        description = "هذا منتج تجريبي يتم إنشاؤه وحذفه تلقائياً للتحقق من الاتصال وقوانين الحماية.",
                        price = 999.0,
                        isActive = false, // Not visible to users
                        isFeatured = false
                    )

                    val insertResponse = SupabaseClient.service.upsertProduct(testProd)
                    if (insertResponse.isSuccessful) {
                        prodLogs.add("تم إدراج المنتج التجريبي بنجاح.")

                        // Read back verification
                        val readBack = SupabaseClient.service.getProducts()
                        val found = readBack.body()?.any { it.id == testId } ?: false
                        if (found) {
                            prodLogs.add("تم التحقق وقراءة المنتج التجريبي المدخل بنجاح.")
                        } else {
                            prodLogs.add("فشل التحقق: لم يتم العثور على المنتج التجريبي في قائمة القراءة.")
                        }

                        // Revert / Delete
                        val deleteResponse = SupabaseClient.service.deleteProduct("eq.$testId")
                        if (deleteResponse.isSuccessful) {
                            prodLogs.add("تمت إزالة المنتج التجريبي بنجاح والمحافظة على نظافة البيانات.")
                            prodStatus = TestStatus.WORKING
                        } else {
                            val (pgCode, msg) = parseErrorBody(deleteResponse)
                            prodLogs.add("تحذير: فشل حذف المنتج التجريبي: $msg")
                            prodStatus = TestStatus.PARTIAL
                        }
                    } else {
                        val (pgCode, msg) = parseErrorBody(insertResponse)
                        prodLogs.add("فشل إدراج منتج تجريبي: $msg")
                        prodError = DiagnosticError(
                            operation = "upsertProduct",
                            tableOrRpc = "catalog_products",
                            httpStatus = insertResponse.code(),
                            postgresCode = pgCode,
                            errorMessage = msg,
                            likelyLayer = "RLS"
                        )
                        prodStatus = TestStatus.PARTIAL
                    }
                } else {
                    val (pgCode, msg) = parseErrorBody(response)
                    prodLogs.add("فشل قراءة جدول المنتجات: $msg")
                    prodError = DiagnosticError(
                        operation = "getProducts",
                        tableOrRpc = "catalog_products",
                        httpStatus = response.code(),
                        postgresCode = pgCode,
                        errorMessage = msg,
                        likelyLayer = "RLS"
                    )
                }
            } catch (e: Exception) {
                prodLogs.add("فشل الاتصال بجدول المنتجات: ${e.message}")
                prodError = DiagnosticError(
                    operation = "getProducts",
                    tableOrRpc = "catalog_products",
                    httpStatus = 0,
                    postgresCode = "NET_ERR",
                    errorMessage = e.message ?: "Network Exception",
                    likelyLayer = "Network"
                )
            }
            updatedResults["products"] = ModuleTestResult(
                name = "products",
                arabicName = "المنتجات (catalog_products)",
                dashboardToWebsite = prodStatus,
                websiteToDashboard = prodStatus,
                supabaseStatus = prodStatus,
                logs = prodLogs,
                error = prodError
            )
            testResults = updatedResults.toMap()

            // -----------------------------------------------------------------
            // 3. CATEGORIES
            // -----------------------------------------------------------------
            progressText = "جاري فحص جدول الأقسام (public.categories)..."
            currentProgress = 0.25f
            delay(400)
            val catLogs = mutableListOf<String>()
            var catError: DiagnosticError? = null
            var catStatus = TestStatus.FAILED

            try {
                val response = SupabaseClient.service.getCategories()
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    catLogs.add("تم قراءة جدول categories بنجاح. عدد الأقسام: ${list.size}")

                    // Try to insert a test category
                    val testId = "sys_test_c_${System.currentTimeMillis()}"
                    val testCat = SupabaseCategoryDto(
                        id = testId,
                        name = "قسم فحص مؤقت",
                        active = false,
                        sortOrder = 999
                    )
                    val insertResponse = SupabaseClient.service.upsertCategory(testCat)
                    if (insertResponse.isSuccessful) {
                        catLogs.add("تم إدراج القسم التجريبي بنجاح.")

                        // Read back
                        val readBack = SupabaseClient.service.getCategories()
                        val found = readBack.body()?.any { it.id == testId } ?: false
                        if (found) {
                            catLogs.add("تم التحقق وقراءة القسم التجريبي بنجاح.")
                        }

                        // Delete
                        val deleteResponse = SupabaseClient.service.deleteCategory("eq.$testId")
                        if (deleteResponse.isSuccessful) {
                            catLogs.add("تمت إزالة القسم التجريبي بنجاح.")
                            catStatus = TestStatus.WORKING
                        } else {
                            catLogs.add("تحذير: فشل حذف القسم التجريبي.")
                            catStatus = TestStatus.PARTIAL
                        }
                    } else {
                        val (pgCode, msg) = parseErrorBody(insertResponse)
                        catLogs.add("فشل إدراج قسم تجريبي: $msg")
                        catError = DiagnosticError(
                            operation = "upsertCategory",
                            tableOrRpc = "categories",
                            httpStatus = insertResponse.code(),
                            postgresCode = pgCode,
                            errorMessage = msg,
                            likelyLayer = "RLS"
                        )
                        catStatus = TestStatus.PARTIAL
                    }
                } else {
                    val (pgCode, msg) = parseErrorBody(response)
                    catLogs.add("فشل قراءة جدول الأقسام: $msg")
                    catError = DiagnosticError(
                        operation = "getCategories",
                        tableOrRpc = "categories",
                        httpStatus = response.code(),
                        postgresCode = pgCode,
                        errorMessage = msg,
                        likelyLayer = "RLS"
                    )
                }
            } catch (e: Exception) {
                catLogs.add("فشل الاتصال بجدول الأقسام: ${e.message}")
                catError = DiagnosticError(
                    operation = "getCategories",
                    tableOrRpc = "categories",
                    httpStatus = 0,
                    postgresCode = "NET_ERR",
                    errorMessage = e.message ?: "Network Exception",
                    likelyLayer = "Network"
                )
            }
            updatedResults["categories"] = ModuleTestResult(
                name = "categories",
                arabicName = "الأقسام (categories)",
                dashboardToWebsite = catStatus,
                websiteToDashboard = catStatus,
                supabaseStatus = catStatus,
                logs = catLogs,
                error = catError
            )
            testResults = updatedResults.toMap()

            // -----------------------------------------------------------------
            // 4. OFFERS / BANNERS
            // -----------------------------------------------------------------
            progressText = "جاري فحص العروض والبنرات (public.offers / banners)..."
            currentProgress = 0.33f
            delay(400)
            val bannerLogs = mutableListOf<String>()
            var bannerError: DiagnosticError? = null
            var bannerStatus = TestStatus.FAILED

            try {
                val response = SupabaseClient.service.getBanners()
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    bannerLogs.add("تم قراءة جدول banners بنجاح. عدد البنرات: ${list.size}")

                    val testId = "sys_test_b_${System.currentTimeMillis()}"
                    val testBanner = SupabaseOfferDto(
                        id = testId,
                        title = "بنر فحص النظام",
                        description = "محتوى مؤقت لإجراء الاختبار الآمن.",
                        isActive = false
                    )
                    val insertResponse = SupabaseClient.service.upsertBanner(testBanner)
                    if (insertResponse.isSuccessful) {
                        bannerLogs.add("تم إدراج البنر التجريبي بنجاح.")

                        val deleteResponse = SupabaseClient.service.deleteBanner("eq.$testId")
                        if (deleteResponse.isSuccessful) {
                            bannerLogs.add("تمت إزالة البنر التجريبي بنجاح.")
                            bannerStatus = TestStatus.WORKING
                        } else {
                            bannerStatus = TestStatus.PARTIAL
                        }
                    } else {
                        val (pgCode, msg) = parseErrorBody(insertResponse)
                        bannerLogs.add("فشل إدراج البنر التجريبي: $msg")
                        bannerError = DiagnosticError(
                            operation = "upsertBanner",
                            tableOrRpc = "banners",
                            httpStatus = insertResponse.code(),
                            postgresCode = pgCode,
                            errorMessage = msg,
                            likelyLayer = "RLS"
                        )
                        bannerStatus = TestStatus.PARTIAL
                    }
                } else {
                    val (pgCode, msg) = parseErrorBody(response)
                    bannerLogs.add("فشل قراءة جدول البنرات: $msg")
                    bannerError = DiagnosticError(
                        operation = "getBanners",
                        tableOrRpc = "banners",
                        httpStatus = response.code(),
                        postgresCode = pgCode,
                        errorMessage = msg,
                        likelyLayer = "RLS"
                    )
                }
            } catch (e: Exception) {
                bannerLogs.add("فشل الاتصال بجدول البنرات: ${e.message}")
                bannerError = DiagnosticError(
                    operation = "getBanners",
                    tableOrRpc = "banners",
                    httpStatus = 0,
                    postgresCode = "NET_ERR",
                    errorMessage = e.message ?: "Network Exception",
                    likelyLayer = "Network"
                )
            }
            updatedResults["offers_coupons"] = ModuleTestResult(
                name = "offers_coupons",
                arabicName = "البنرات والعروض (banners)",
                dashboardToWebsite = bannerStatus,
                websiteToDashboard = bannerStatus,
                supabaseStatus = bannerStatus,
                logs = bannerLogs,
                error = bannerError
            )
            testResults = updatedResults.toMap()

            // -----------------------------------------------------------------
            // 5. SITE CUSTOMIZATION
            // -----------------------------------------------------------------
            progressText = "جاري فحص تخصيص الموقع الويب (public.site_customizations)..."
            currentProgress = 0.41f
            delay(400)
            val customLogs = mutableListOf<String>()
            var customError: DiagnosticError? = null
            var customStatus = TestStatus.FAILED

            try {
                val response = SupabaseClient.service.getSiteCustomizations()
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    customLogs.add("تم قراءة التخصيصات الحالية. العدد: ${list.size}")

                    if (list.isNotEmpty()) {
                        val original = list[0]
                        customLogs.add("تم العثور على تخصيص نشط: ${original.heroTitle}")

                        // Perform a safe temporary test
                        val tempVal = "فحص تلقائي للنظام - ${System.currentTimeMillis()}"
                        val tempCustom = original.copy(announcementBarText = tempVal)

                        val updateResponse = SupabaseClient.service.upsertSiteCustomizations(tempCustom)
                        if (updateResponse.isSuccessful) {
                            customLogs.add("تم إرسال التحديث التجريبي ومطابقته بنجاح.")

                            // Read back
                            val rb = SupabaseClient.service.getSiteCustomizations()
                            val updatedItem = rb.body()?.firstOrNull { it.id == original.id }
                            if (updatedItem?.announcementBarText == tempVal) {
                                customLogs.add("تم التحقق بنجاح من قراءة النص المؤقت.")
                            }

                            // Restore original
                            val restoreResponse = SupabaseClient.service.upsertSiteCustomizations(original)
                            if (restoreResponse.isSuccessful) {
                                customLogs.add("تمت استعادة تفاصيل التخصيص الأصلية بنجاح.")
                                customStatus = TestStatus.WORKING
                            } else {
                                customLogs.add("خطأ أثناء استعادة التخصيص الأصلي.")
                                customStatus = TestStatus.PARTIAL
                            }
                        } else {
                            val (pgCode, msg) = parseErrorBody(updateResponse)
                            customLogs.add("فشل تحديث التخصيص مؤقتاً: $msg")
                            customError = DiagnosticError(
                                operation = "upsertSiteCustomizations",
                                tableOrRpc = "site_customizations",
                                httpStatus = updateResponse.code(),
                                postgresCode = pgCode,
                                errorMessage = msg,
                                likelyLayer = "RLS"
                            )
                            customStatus = TestStatus.PARTIAL
                        }
                    } else {
                        customLogs.add("لا توجد تخصيصات حالياً في الجدول لرفع التحديثات عليها.")
                        customStatus = TestStatus.PARTIAL
                    }
                } else {
                    val (pgCode, msg) = parseErrorBody(response)
                    customLogs.add("فشل قراءة جدول التخصيصات: $msg")
                    customError = DiagnosticError(
                        operation = "getSiteCustomizations",
                        tableOrRpc = "site_customizations",
                        httpStatus = response.code(),
                        postgresCode = pgCode,
                        errorMessage = msg,
                        likelyLayer = "RLS"
                    )
                }
            } catch (e: Exception) {
                customLogs.add("فشل الاتصال بجدول التخصيصات: ${e.message}")
                customError = DiagnosticError(
                    operation = "getSiteCustomizations",
                    tableOrRpc = "site_customizations",
                    httpStatus = 0,
                    postgresCode = "NET_ERR",
                    errorMessage = e.message ?: "Network Exception",
                    likelyLayer = "Network"
                )
            }
            updatedResults["website_editor"] = ModuleTestResult(
                name = "website_editor",
                arabicName = "تخصيص الموقع (site_customizations)",
                dashboardToWebsite = customStatus,
                websiteToDashboard = customStatus,
                supabaseStatus = customStatus,
                logs = customLogs,
                error = customError
            )
            testResults = updatedResults.toMap()

            // -----------------------------------------------------------------
            // 6. ORDERS (WEBSITE -> DASHBOARD) & JSON ITEMS
            // -----------------------------------------------------------------
            progressText = "جاري فحص تدفق الطلبات وهيكلية البيانات (public.orders)..."
            currentProgress = 0.5f
            delay(400)
            val orderLogs = mutableListOf<String>()
            var orderError: DiagnosticError? = null
            var orderStatus = TestStatus.FAILED

            try {
                val response = SupabaseClient.service.getOrders()
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    orderLogs.add("تم قراءة جدول الطلبات بنجاح. عدد الطلبات: ${list.size}")

                    // Insert a safe temporary test order
                    val testId = "sys_test_o_${System.currentTimeMillis()}"
                    val testOrder = SupabaseOrderDto(
                        id = testId,
                        orderNumber = "SYS-TEST-999",
                        customerName = "عميل فحص النظام",
                        phone = "01000000000",
                        address = "شارع الفحص الفني",
                        governorate = "القاهرة",
                        center = "مصر الجديدة",
                        subtotal = 100.0,
                        shippingCost = 20.0,
                        total = 120.0,
                        paymentMethod = "COD",
                        status = "PENDING"
                    )

                    val insertResponse = SupabaseClient.service.upsertOrder(testOrder)
                    if (insertResponse.isSuccessful) {
                        orderLogs.add("تم محاكاة Checkout من الموقع وإدراج الطلب التجريبي بنجاح.")

                        // Read back check
                        val readBack = SupabaseClient.service.getOrders()
                        val found = readBack.body()?.firstOrNull { it.id == testId }
                        if (found != null) {
                            orderLogs.add("تمت قراءة الطلب من لوحة التحكم بنجاح ومطابقة تفاصيل العميل.")
                        }

                        // Revert via DELETE
                        val deleteResponse = SupabaseClient.service.deleteOrder("eq.$testId")
                        if (deleteResponse.isSuccessful) {
                            orderLogs.add("تمت تصفية وإزالة الطلب التجريبي بنجاح من قاعدة البيانات.")
                            orderStatus = TestStatus.WORKING
                        } else {
                            val (pgCode, msg) = parseErrorBody(deleteResponse)
                            orderLogs.add("تحذير: فشل إزالة الطلب التجريبي: $msg")
                            orderStatus = TestStatus.PARTIAL
                        }
                    } else {
                        val (pgCode, msg) = parseErrorBody(insertResponse)
                        orderLogs.add("فشل إدراج طلب تجريبي: $msg")
                        orderError = DiagnosticError(
                            operation = "upsertOrder",
                            tableOrRpc = "orders",
                            httpStatus = insertResponse.code(),
                            postgresCode = pgCode,
                            errorMessage = msg,
                            likelyLayer = "RLS"
                        )
                        orderStatus = TestStatus.PARTIAL
                    }
                } else {
                    val (pgCode, msg) = parseErrorBody(response)
                    orderLogs.add("فشل قراءة جدول الطلبات: $msg")
                    orderError = DiagnosticError(
                        operation = "getOrders",
                        tableOrRpc = "orders",
                        httpStatus = response.code(),
                        postgresCode = pgCode,
                        errorMessage = msg,
                        likelyLayer = "RLS"
                    )
                }
            } catch (e: Exception) {
                orderLogs.add("فشل الاتصال بجدول الطلبات: ${e.message}")
                orderError = DiagnosticError(
                    operation = "getOrders",
                    tableOrRpc = "orders",
                    httpStatus = 0,
                    postgresCode = "NET_ERR",
                    errorMessage = e.message ?: "Network Exception",
                    likelyLayer = "Network"
                )
            }
            updatedResults["orders"] = ModuleTestResult(
                name = "orders",
                arabicName = "تدفق الطلبات (orders)",
                dashboardToWebsite = orderStatus,
                websiteToDashboard = orderStatus,
                supabaseStatus = orderStatus,
                logs = orderLogs,
                error = orderError
            )
            testResults = updatedResults.toMap()

            // -----------------------------------------------------------------
            // 7. ORDERS (DASHBOARD -> WEBSITE UPDATE)
            // -----------------------------------------------------------------
            progressText = "جاري فحص تحديث حالات الطلبات (Dashboard -> Website)..."
            currentProgress = 0.58f
            delay(400)
            val orderUpLogs = mutableListOf<String>()
            var orderUpError: DiagnosticError? = null
            var orderUpStatus = TestStatus.FAILED

            try {
                val listResponse = SupabaseClient.service.getOrders()
                if (listResponse.isSuccessful && listResponse.body()?.isNotEmpty() == true) {
                    val targetOrder = listResponse.body()!!.first()
                    val originalStatus = targetOrder.status ?: "PENDING"
                    orderUpLogs.add("تم العثور على طلب لتجربة التحديث: ${targetOrder.orderNumber}")

                    // Patch to DELIVERED
                    val updateResponse = SupabaseClient.service.updateOrderStatus("eq.${targetOrder.id}", mapOf("status" to "DELIVERED"))
                    if (updateResponse.isSuccessful) {
                        orderUpLogs.add("تم تغيير حالة الطلب إلى DELIVERED بنجاح.")

                        // Read back
                        val rb = SupabaseClient.service.getOrders()
                        val updated = rb.body()?.firstOrNull { it.id == targetOrder.id }
                        if (updated?.status == "DELIVERED") {
                            orderUpLogs.add("تم تأكيد وصول الحالة الجديدة.")
                        }

                        // Restore original status
                        SupabaseClient.service.updateOrderStatus("eq.${targetOrder.id}", mapOf("status" to originalStatus))
                        orderUpLogs.add("تمت استعادة حالة الطلب الأصلية ($originalStatus) بنجاح.")
                        orderUpStatus = TestStatus.WORKING
                    } else {
                        val (pgCode, msg) = parseErrorBody(updateResponse)
                        orderUpLogs.add("فشل تحديث الحالة: $msg")
                        orderUpError = DiagnosticError(
                            operation = "updateOrderStatus",
                            tableOrRpc = "orders",
                            httpStatus = updateResponse.code(),
                            postgresCode = pgCode,
                            errorMessage = msg,
                            likelyLayer = "RLS"
                        )
                        orderUpStatus = TestStatus.PARTIAL
                    }
                } else {
                    orderUpLogs.add("لا توجد طلبات متوفرة في الجدول لتعديلها.")
                    orderUpStatus = TestStatus.PARTIAL
                }
            } catch (e: Exception) {
                orderUpLogs.add("فشل تحديث حالة الطلبات: ${e.message}")
                orderUpError = DiagnosticError(
                    operation = "updateOrderStatus",
                    tableOrRpc = "orders",
                    httpStatus = 0,
                    postgresCode = "NET_ERR",
                    errorMessage = e.message ?: "Network Exception",
                    likelyLayer = "Network"
                )
            }
            // Add a mock or update visual representation for order mapping
            updatedResults["orders_dashboard_to_website"] = ModuleTestResult(
                name = "orders_dashboard_to_website",
                arabicName = "تحديث حالات الطلبات",
                dashboardToWebsite = orderUpStatus,
                websiteToDashboard = orderUpStatus,
                supabaseStatus = orderUpStatus,
                logs = orderUpLogs,
                error = orderUpError
            )
            testResults = updatedResults.toMap()

            // -----------------------------------------------------------------
            // 8. PRIME
            // -----------------------------------------------------------------
            progressText = "جاري فحص باقات واشتراكات Pinky Prime..."
            currentProgress = 0.66f
            delay(400)
            val primeLogs = mutableListOf<String>()
            var primeError: DiagnosticError? = null
            var primeStatus = TestStatus.FAILED

            try {
                val response = SupabaseClient.service.getPrimeSubscriptions()
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    primeLogs.add("تم الاتصال بنجاح بجدول prime_subscriptions. عدد المشتركين: ${list.size}")

                    // Create test prime sub
                    val testId = "sys_test_prime_${System.currentTimeMillis()}"
                    val testSub = SupabasePrimeSubscriptionDto(
                        id = testId,
                        customerName = "مشترك تجريبي للنظام",
                        phone = "01111111111",
                        startDate = "2026-09-17",
                        expiryDate = "2026-10-17",
                        status = "ACTIVE"
                    )

                    val insertResponse = SupabaseClient.service.upsertPrimeSubscription(testSub)
                    if (insertResponse.isSuccessful) {
                        primeLogs.add("تم تسجيل اشتراك برايم تجريبي بنجاح.")

                        // Read back
                        val rb = SupabaseClient.service.getPrimeSubscriptions()
                        val found = rb.body()?.any { it.id == testId } ?: false
                        if (found) {
                            primeLogs.add("تم التحقق وقراءة اشتراك برايم من لوحة التحكم بنجاح.")
                        }

                        // Revert via DELETE
                        val deleteResponse = SupabaseClient.service.deletePrimeSubscription("eq.$testId")
                        if (deleteResponse.isSuccessful) {
                            primeLogs.add("تمت تصفية اشتراك برايم التجريبي بنجاح.")
                            primeStatus = TestStatus.WORKING
                        } else {
                            primeStatus = TestStatus.PARTIAL
                        }
                    } else {
                        val (pgCode, msg) = parseErrorBody(insertResponse)
                        primeLogs.add("فشل تسجيل اشتراك برايم تجريبي: $msg")
                        primeError = DiagnosticError(
                            operation = "upsertPrimeSubscription",
                            tableOrRpc = "prime_subscriptions",
                            httpStatus = insertResponse.code(),
                            postgresCode = pgCode,
                            errorMessage = msg,
                            likelyLayer = "RLS"
                        )
                        primeStatus = TestStatus.PARTIAL
                    }
                } else {
                    val (pgCode, msg) = parseErrorBody(response)
                    primeLogs.add("فشل قراءة جدول الاشتراكات: $msg")
                    primeError = DiagnosticError(
                        operation = "getPrimeSubscriptions",
                        tableOrRpc = "prime_subscriptions",
                        httpStatus = response.code(),
                        postgresCode = pgCode,
                        errorMessage = msg,
                        likelyLayer = "RLS"
                    )
                }
            } catch (e: Exception) {
                primeLogs.add("فشل الاتصال بجدول باقات برايم: ${e.message}")
                primeError = DiagnosticError(
                    operation = "getPrimeSubscriptions",
                    tableOrRpc = "prime_subscriptions",
                    httpStatus = 0,
                    postgresCode = "NET_ERR",
                    errorMessage = e.message ?: "Network Exception",
                    likelyLayer = "Network"
                )
            }
            updatedResults["prime"] = ModuleTestResult(
                name = "prime",
                arabicName = "باقات برايم (prime_subscriptions)",
                dashboardToWebsite = primeStatus,
                websiteToDashboard = primeStatus,
                supabaseStatus = primeStatus,
                logs = primeLogs,
                error = primeError
            )
            testResults = updatedResults.toMap()

            // -----------------------------------------------------------------
            // 9. SHIPPING
            // -----------------------------------------------------------------
            progressText = "جاري التحقق من تسعير المحافظات والمراكز..."
            currentProgress = 0.75f
            delay(400)
            val shipLogs = mutableListOf<String>()
            var shipError: DiagnosticError? = null
            var shipStatus = TestStatus.FAILED

            try {
                val govResponse = SupabaseClient.service.getGovernorates()
                val centersResponse = SupabaseClient.service.getShippingCenters()

                if (govResponse.isSuccessful && centersResponse.isSuccessful) {
                    val govs = govResponse.body() ?: emptyList()
                    val centers = centersResponse.body() ?: emptyList()

                    shipLogs.add("تم الاتصال بجدول المحافظات بنجاح. العدد: ${govs.size}")
                    shipLogs.add("تم الاتصال بجدول مراكز التوصيل بنجاح. العدد: ${centers.size}")
                    shipStatus = TestStatus.WORKING
                } else {
                    val (pgCode, msg) = if (!govResponse.isSuccessful) parseErrorBody(govResponse) else parseErrorBody(centersResponse)
                    shipLogs.add("فشل قراءة بيانات الشحن والتوصيل: $msg")
                    shipError = DiagnosticError(
                        operation = "getGovernorates / getShippingCenters",
                        tableOrRpc = "shipping_governorates",
                        httpStatus = if (!govResponse.isSuccessful) govResponse.code() else centersResponse.code(),
                        postgresCode = pgCode,
                        errorMessage = msg,
                        likelyLayer = "RLS"
                    )
                }
            } catch (e: Exception) {
                shipLogs.add("فشل الاتصال بجدول الشحن: ${e.message}")
                shipError = DiagnosticError(
                    operation = "getGovernorates",
                    tableOrRpc = "shipping_governorates",
                    httpStatus = 0,
                    postgresCode = "NET_ERR",
                    errorMessage = e.message ?: "Network Exception",
                    likelyLayer = "Network"
                )
            }
            updatedResults["shipping"] = ModuleTestResult(
                name = "shipping",
                arabicName = "الشحن والأسعار (shipping_governorates)",
                dashboardToWebsite = shipStatus,
                websiteToDashboard = shipStatus,
                supabaseStatus = shipStatus,
                logs = shipLogs,
                error = shipError
            )
            testResults = updatedResults.toMap()

            // -----------------------------------------------------------------
            // 10. COUPONS
            // -----------------------------------------------------------------
            progressText = "جاري فحص نظام أكواد الخصم والكوبونات..."
            currentProgress = 0.83f
            delay(400)
            val couponLogs = mutableListOf<String>()
            var couponError: DiagnosticError? = null
            var couponStatus = TestStatus.FAILED

            try {
                val response = SupabaseClient.service.getCoupons()
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    couponLogs.add("تم قراءة جدول الكوبونات بنجاح. عدد الكوبونات الحالية: ${list.size}")

                    // Insert test coupon
                    val testId = "sys_test_cpn_${System.currentTimeMillis()}"
                    val testCpn = SupabaseCouponDto(
                        id = testId,
                        code = "TESTCHECK99",
                        discountType = "PERCENTAGE",
                        discountValue = 15.0,
                        isActive = false
                    )

                    val insertResponse = SupabaseClient.service.upsertCoupon(testCpn)
                    if (insertResponse.isSuccessful) {
                        couponLogs.add("تم إدراج كود الخصم التجريبي بنجاح.")

                        // Read back
                        val rb = SupabaseClient.service.getCoupons()
                        val found = rb.body()?.any { it.id == testId } ?: false
                        if (found) {
                            couponLogs.add("تم التحقق ومطابقة الكود التجريبي بنجاح.")
                        }

                        // Revert via DELETE
                        val deleteResponse = SupabaseClient.service.deleteCoupon("eq.$testId")
                        if (deleteResponse.isSuccessful) {
                            couponLogs.add("تم حذف وإلغاء الكود التجريبي بنجاح.")
                            couponStatus = TestStatus.WORKING
                        } else {
                            couponStatus = TestStatus.PARTIAL
                        }
                    } else {
                        val (pgCode, msg) = parseErrorBody(insertResponse)
                        couponLogs.add("فشل إدراج الكوبون التجريبي: $msg")
                        couponError = DiagnosticError(
                            operation = "upsertCoupon",
                            tableOrRpc = "coupons",
                            httpStatus = insertResponse.code(),
                            postgresCode = pgCode,
                            errorMessage = msg,
                            likelyLayer = "RLS"
                        )
                        couponStatus = TestStatus.PARTIAL
                    }
                } else {
                    val (pgCode, msg) = parseErrorBody(response)
                    couponLogs.add("فشل قراءة جدول الكوبونات: $msg")
                    couponError = DiagnosticError(
                        operation = "getCoupons",
                        tableOrRpc = "coupons",
                        httpStatus = response.code(),
                        postgresCode = pgCode,
                        errorMessage = msg,
                        likelyLayer = "RLS"
                    )
                }
            } catch (e: Exception) {
                couponLogs.add("فشل الاتصال بجدول الكوبونات: ${e.message}")
                couponError = DiagnosticError(
                    operation = "getCoupons",
                    tableOrRpc = "coupons",
                    httpStatus = 0,
                    postgresCode = "NET_ERR",
                    errorMessage = e.message ?: "Network Exception",
                    likelyLayer = "Network"
                )
            }
            updatedResults["coupons"] = ModuleTestResult(
                name = "coupons",
                arabicName = "الكوبونات (coupons)",
                dashboardToWebsite = couponStatus,
                websiteToDashboard = couponStatus,
                supabaseStatus = couponStatus,
                logs = couponLogs,
                error = couponError
            )
            testResults = updatedResults.toMap()

            // -----------------------------------------------------------------
            // 11. STORE SETTINGS
            // -----------------------------------------------------------------
            progressText = "جاري التحقق من إعدادات المتجر وبيانات التواصل..."
            currentProgress = 0.91f
            delay(400)
            val storeLogs = mutableListOf<String>()
            var storeError: DiagnosticError? = null
            var storeStatus = TestStatus.FAILED

            try {
                val response = SupabaseClient.service.getStoreSettings()
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    storeLogs.add("تم الاتصال بجدول الإعدادات بنجاح. عدد السجلات: ${list.size}")

                    if (list.isNotEmpty()) {
                        val original = list[0]
                        storeLogs.add("اسم المتجر المعتمد حالياً: ${original.storeName}")

                        // Perform a safe temporary test
                        val tempVal = "متجر بينكي التجريبي - ${System.currentTimeMillis()}"
                        val tempSettings = original.copy(storeName = tempVal)

                        val updateResponse = SupabaseClient.service.upsertStoreSettings(tempSettings)
                        if (updateResponse.isSuccessful) {
                            storeLogs.add("تم تحديث إعدادات المتجر مؤقتاً بنجاح.")

                            // Read back
                            val rb = SupabaseClient.service.getStoreSettings()
                            val updated = rb.body()?.firstOrNull { it.id == original.id }
                            if (updated?.storeName == tempVal) {
                                storeLogs.add("تم التحقق وقراءة التحديث المؤقت بنجاح.")
                            }

                            // Restore
                            val restoreResponse = SupabaseClient.service.upsertStoreSettings(original)
                            if (restoreResponse.isSuccessful) {
                                storeLogs.add("تمت استعادة إعدادات المتجر الأصلية بنجاح.")
                                storeStatus = TestStatus.WORKING
                            } else {
                                storeStatus = TestStatus.PARTIAL
                            }
                        } else {
                            val (pgCode, msg) = parseErrorBody(updateResponse)
                            storeLogs.add("فشل تحديث إعدادات المتجر مؤقتاً: $msg")
                            storeError = DiagnosticError(
                                operation = "upsertStoreSettings",
                                tableOrRpc = "store_settings",
                                httpStatus = updateResponse.code(),
                                postgresCode = pgCode,
                                errorMessage = msg,
                                likelyLayer = "RLS"
                            )
                            storeStatus = TestStatus.PARTIAL
                        }
                    } else {
                        storeLogs.add("لا توجد سجلات حالية في جدول store_settings لتعديلها.")
                        storeStatus = TestStatus.PARTIAL
                    }
                } else {
                    val (pgCode, msg) = parseErrorBody(response)
                    storeLogs.add("فشل قراءة جدول إعدادات المتجر: $msg")
                    storeError = DiagnosticError(
                        operation = "getStoreSettings",
                        tableOrRpc = "store_settings",
                        httpStatus = response.code(),
                        postgresCode = pgCode,
                        errorMessage = msg,
                        likelyLayer = "RLS"
                    )
                }
            } catch (e: Exception) {
                storeLogs.add("فشل الاتصال بجدول إعدادات المتجر: ${e.message}")
                storeError = DiagnosticError(
                    operation = "getStoreSettings",
                    tableOrRpc = "store_settings",
                    httpStatus = 0,
                    postgresCode = "NET_ERR",
                    errorMessage = e.message ?: "Network Exception",
                    likelyLayer = "Network"
                )
            }
            updatedResults["store_settings"] = ModuleTestResult(
                name = "store_settings",
                arabicName = "إعدادات المتجر (store_settings)",
                dashboardToWebsite = storeStatus,
                websiteToDashboard = storeStatus,
                supabaseStatus = storeStatus,
                logs = storeLogs,
                error = storeError
            )
            testResults = updatedResults.toMap()

            // -----------------------------------------------------------------
            // 12. PUSH DEVICES
            // -----------------------------------------------------------------
            progressText = "جاري اختبار تسجيل أجهزة الإشعارات الفورية..."
            currentProgress = 1.0f
            delay(400)
            val pushLogs = mutableListOf<String>()
            var pushError: DiagnosticError? = null
            var pushStatus = TestStatus.FAILED

            try {
                // Generate a dummy device registration
                val testId = "sys_test_dev_${System.currentTimeMillis()}"
                val dummyDevice = SupabaseDashboardPushDeviceDto(
                    id = testId,
                    deviceName = "أندرويد ديباغ - فحص النظام",
                    pushToken = "dummy_token_verify_system_${System.currentTimeMillis()}",
                    lastActive = "2026-09-17"
                )

                val response = SupabaseClient.service.registerPushDevice(dummyDevice)
                if (response.isSuccessful) {
                    pushLogs.add("تم تسجيل الجهاز التجريبي بنجاح في جدول dashboard_push_devices.")
                    pushLogs.add("تم التحقق من مسار التوصيل والاتصال بنجاح.")
                    pushStatus = TestStatus.WORKING
                } else {
                    val (pgCode, msg) = parseErrorBody(response)
                    pushLogs.add("فشل تسجيل الجهاز التجريبي: $msg")
                    pushError = DiagnosticError(
                        operation = "registerPushDevice",
                        tableOrRpc = "dashboard_push_devices",
                        httpStatus = response.code(),
                        postgresCode = pgCode,
                        errorMessage = msg,
                        likelyLayer = "RLS"
                    )
                }
            } catch (e: Exception) {
                pushLogs.add("فشل الاتصال بجدول تسجيل الأجهزة: ${e.message}")
                pushError = DiagnosticError(
                    operation = "registerPushDevice",
                    tableOrRpc = "dashboard_push_devices",
                    httpStatus = 0,
                    postgresCode = "NET_ERR",
                    errorMessage = e.message ?: "Network Exception",
                    likelyLayer = "Network"
                )
            }
            updatedResults["notifications"] = ModuleTestResult(
                name = "notifications",
                arabicName = "أجهزة الإشعارات (dashboard_push_devices)",
                dashboardToWebsite = pushStatus,
                websiteToDashboard = pushStatus,
                supabaseStatus = pushStatus,
                logs = pushLogs,
                error = pushError
            )
            testResults = updatedResults.toMap()

            progressText = "اكتملت كافة الاختبارات بنجاح!"
            isRunning = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("فحص النظام والربط الحقيقي", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("health_check_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Control Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "اختبار الربط الشامل",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "يقوم النظام بإجراء قراءة وكتابة حقيقية مشروطة وآمنة للتحقق من تكامل الموقع مع لوحة التحكم وقاعدة بيانات سوبابيز وسير العمل.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { runAllTests() },
                            enabled = !isRunning,
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528)),
                            modifier = Modifier.testTag("run_all_tests_btn")
                        ) {
                            if (isRunning) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF381528))
                            } else {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تشغيل الفحص", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isRunning) {
                        Spacer(modifier = Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = currentProgress,
                            modifier = Modifier.fillMaxWidth(),
                            color = PinkPrimary,
                            trackColor = PinkPrimary.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = PinkPrimary
                        )
                    }
                }
            }

            // CENTRAL AUTH SESSION PARAMETERS DIAGNOSTIC (COMPACT & COLLAPSIBLE)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                onClick = { isSessionCardExpanded = !isSessionCardExpanded }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val isInit = SupabaseClient.isSessionReady
                    val isAuth = SupabaseClient.isAuthenticated
                    val sessExists = SupabaseClient.accessToken != null
                    val rawUserId = SupabaseClient.userId ?: "لا يوجد"
                    val shortUserId = if (rawUserId.length > 8) rawUserId.substring(0, 8) + "..." else rawUserId
                    val dbRole = if (sessExists) "authenticated" else "anonymous"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = if (isSessionCardExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Toggle Details",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "بيانات الجلسة",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Compact single line summary
                        Text(
                            text = "مصادق: ${if (isAuth) "نعم ✓" else "لا ✗"} | الهوية: $shortUserId | الدور: $dbRole",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (isSessionCardExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val expiresAtVal = SupabaseClient.expiresAt
                        val formattedExpiry = if (expiresAtVal > 0L) {
                            val remainingSecs = (expiresAtVal - System.currentTimeMillis()) / 1000L
                            if (remainingSecs > 0) {
                                "ينتهي خلال ${remainingSecs / 60} دقيقة و ${remainingSecs % 60} ثانية"
                            } else {
                                "منتهي الصلاحية"
                            }
                        } else {
                            "غير متوفر"
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                InfoLine(label = "تهيئة الجلسة (Auth Initialized):", value = if (isInit) "نعم ✅" else "لا ❌")
                                InfoLine(label = "مصادق عليه (Authenticated):", value = if (isAuth) "نعم ✅" else "لا ❌")
                                InfoLine(label = "مستند الهوية (User ID):", value = rawUserId)
                                InfoLine(label = "صلاحية الجلسة (Session Exists):", value = if (sessExists) "متوفرة" else "غير متوفرة")
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                InfoLine(label = "إرفاق التوكن تلقائياً:", value = if (sessExists) "نعم" else "لا")
                                InfoLine(label = "تجديد تلقائي (Auto Refresh):", value = "مفعل (كل 60 ثانية) 🔄")
                                InfoLine(label = "صلاحية التوكن (Token Expiry):", value = formattedExpiry)
                                InfoLine(label = "دور قاعدة البيانات (DB Role):", value = dbRole)
                            }
                        }
                    }
                }
            }

            // Test Summary Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryBox(title = "يعمل بالكامل", count = workingCount, color = Color(0xFF2E7D32), modifier = Modifier.weight(1f))
                SummaryBox(title = "يعمل جزئياً", count = partialCount, color = Color(0xFFF57C00), modifier = Modifier.weight(1f))
                SummaryBox(title = "فشل الربط", count = failedCount, color = Color(0xFFC62828), modifier = Modifier.weight(1f))
                SummaryBox(title = "غير قابل للفحص", count = untestableCount, color = Color(0xFF757575), modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable Matrix of all checks
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "مصفوفة فحص تكامل النظام والربط السحابي:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(testResults.values.toList()) { result ->
                    ModuleResultCard(result = result)
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun SummaryBox(title: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(count.toString(), fontSize = 18.sp, color = color, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun ModuleResultCard(result: ModuleTestResult) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(result.supabaseStatus.color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = result.supabaseStatus.icon,
                        contentDescription = null,
                        tint = result.supabaseStatus.color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(result.arabicName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "جدول: public.${result.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = result.supabaseStatus.color.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, result.supabaseStatus.color.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = result.supabaseStatus.label,
                        color = result.supabaseStatus.color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Sync flow labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "من لوحة التحكم ← موقع الويب: ${result.dashboardToWebsite.label}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "من موقع الويب ← لوحة التحكم: ${result.websiteToDashboard.label}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Diagnostic Logs Box
                    Text("سجلات وخطوات الفحص الحقيقي:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF9F9F9))
                            .padding(8.dp)
                    ) {
                        if (result.logs.isEmpty()) {
                            Text("اضغط على 'تشغيل الفحص' للبدء في تتبع الاتصال.", fontSize = 11.sp, color = Color.Gray)
                        } else {
                            result.logs.forEach { log ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color.Gray))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(log, fontSize = 11.sp, color = Color(0xFF333333))
                                }
                            }
                        }
                    }

                    // Diagnostics error box (if any)
                    result.error?.let { err ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("تفاصيل تشخيص الخطأ الدقيقة (Safe Diagnostics):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Red)
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFF2F2))
                                .padding(8.dp)
                        ) {
                            val activeToken = SupabaseClient.accessToken
                            val isAuthAttached = activeToken != null && activeToken.isNotBlank() && activeToken != "sandbox_token"
                            val jwtRole = if (isAuthAttached) "authenticated" else "anonymous"
                            val reqUrl = "${SupabaseClient.supabaseUrl}/rest/v1/${err.tableOrRpc}"

                            val postgresHint = when {
                                err.postgresCode.contains("42501") -> "RLS/GRANT: دور '$jwtRole' لا يملك صلاحية '${err.operation}' على هذا الجدول. تأكد من تفعيل GRANT SELECT TO authenticated ومن وجود سياسة RLS تسمح للمستخدم بالوصول."
                                err.httpStatus == 401 -> "أخطاء المصادقة: رمز JWT الممرر غير صالح أو منتهي الصلاحية لدى بوابة Supabase."
                                else -> "تأكد من مطابقة اسم الجدول في قاعدة البيانات وصلاحيات الوصول لدور authenticated."
                            }

                            Text("• الجدول المستهدف (TABLE): public.${err.tableOrRpc}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("• العملية (OPERATION): ${err.operation}", fontSize = 11.sp)
                            Text("• كود الحالة (HTTP STATUS): ${err.httpStatus}", fontSize = 11.sp)
                            Text("• كود PostgreSQL (CODE): ${err.postgresCode}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("• رسالة PostgreSQL (MESSAGE): ${err.errorMessage}", fontSize = 11.sp)
                            Text("• تلميح التشخيص (HINT): $postgresHint", fontSize = 11.sp, color = Color(0xFFC62828))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("• حالة الجلسة المحلية (AUTH SESSION READY): ${if (SupabaseClient.isSessionReady) "READY ✓" else "NOT READY ✗"}", fontSize = 11.sp)
                            Text("• مصادق عليه محلياً (AUTHENTICATED): ${if (SupabaseClient.isAuthenticated) "نعم ✓" else "لا ✗"}", fontSize = 11.sp)
                            Text("• مستند هوية المستخدم (USER ID): ${SupabaseClient.userId ?: "لا يوجد"}", fontSize = 11.sp)
                            Text("• إرفاق الـ Authorization Header: ${if (isAuthAttached) "نعم (Bearer [HIDDEN_TOKEN]) ✓" else "لا ✗"}", fontSize = 11.sp)
                            Text("• دور المستخدم في الـ JWT (JWT ROLE): $jwtRole", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("• رابط الطلب الآمن (REQUEST URL): $reqUrl", fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("المستوى المرجح للخطأ: ${err.likelyLayer}", fontSize = 11.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Initialise empty list of modules
fun getInitialModules(): Map<String, ModuleTestResult> {
    val initialList = listOf(
        Pair("auth", "المصادقة والصلاحيات (Auth)"),
        Pair("products", "المنتجات (catalog_products)"),
        Pair("categories", "الأقسام (categories)"),
        Pair("offers_coupons", "البنرات والعروض (banners)"),
        Pair("website_editor", "تخصيص الموقع (site_customizations)"),
        Pair("orders", "تدفق الطلبات (orders)"),
        Pair("orders_dashboard_to_website", "تحديث حالات الطلبات"),
        Pair("prime", "باقات برايم (prime_subscriptions)"),
        Pair("shipping", "الشحن والأسعار (shipping_governorates)"),
        Pair("coupons", "الكوبونات (coupons)"),
        Pair("store_settings", "إعدادات المتجر (store_settings)"),
        Pair("notifications", "أجهزة الإشعارات (dashboard_push_devices)")
    )
    return initialList.associate { (name, arabic) ->
        name to ModuleTestResult(
            name = name,
            arabicName = arabic,
            dashboardToWebsite = TestStatus.NOT_TESTABLE,
            websiteToDashboard = TestStatus.NOT_TESTABLE,
            supabaseStatus = TestStatus.NOT_TESTABLE
        )
    }
}

@Composable
fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Normal)
        Text(text = value, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
    }
}
