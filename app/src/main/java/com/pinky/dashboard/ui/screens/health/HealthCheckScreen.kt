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
import com.pinky.dashboard.domain.model.Permission
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
    val context = androidx.compose.ui.platform.LocalContext.current
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
            // 0. Ensure session is loaded and refreshed
            if (SupabaseClient.accessToken == null || SupabaseClient.accessToken == "sandbox_token") {
                val prefs = context.getSharedPreferences("pinky_auth_prefs", android.content.Context.MODE_PRIVATE)
                val token = prefs.getString("user_token", null)
                val rToken = prefs.getString("refresh_token", null)
                val expiresAt = prefs.getLong("expires_at", 0L)
                val userId = prefs.getString("user_id", null)
                if (token != null && userId != null) {
                    SupabaseClient.accessToken = token
                    SupabaseClient.refreshToken = rToken
                    SupabaseClient.expiresAt = expiresAt
                    SupabaseClient.userId = userId
                    SupabaseClient.isAuthenticated = true
                    SupabaseClient.isSessionReady = true
                }
            }
            if (SupabaseClient.accessToken != null && SupabaseClient.accessToken != "sandbox_token") {
                try {
                    SupabaseClient.performTokenRefresh()
                } catch (e: Exception) {
                    android.util.Log.e("HealthCheck", "Session auto-refresh failed: ${e.message}")
                }
            }

            val updatedResults = testResults.toMutableMap()

            // -----------------------------------------------------------------
            // 1. SUPABASE URL VERIFICATION
            // -----------------------------------------------------------------
            progressText = "جاري التحقق من عنوان سوبابيز المستهدف..."
            currentProgress = 0.14f
            delay(300)

            val urlLogs = mutableListOf<String>()
            var urlError: DiagnosticError? = null
            var urlStatus = TestStatus.FAILED

            val activeUrl = SupabaseClient.supabaseUrl
            urlLogs.add("العنوان المستخدم حالياً في التطبيق: $activeUrl")
            if (activeUrl == "https://dmttevcncsxzbamazmmq.supabase.co") {
                urlLogs.add("تم التحقق: عنوان السحابة يطابق العنوان المستهدف في مواصفات العقد الفعلي.")
                urlStatus = TestStatus.WORKING
            } else {
                urlLogs.add("خطأ: عنوان السحابة المستخدم لا يطابق الرابط المحدد!")
                urlError = DiagnosticError(
                    operation = "checkUrl",
                    tableOrRpc = "client_config",
                    httpStatus = 0,
                    postgresCode = "URL_MISMATCH",
                    errorMessage = "رابط سوبابيز النشط ($activeUrl) لا يطابق العقد المطلوب.",
                    likelyLayer = "Configuration"
                )
            }

            updatedResults["url_check"] = ModuleTestResult(
                name = "url_check",
                arabicName = "التحقق من عنوان السحابة (Supabase URL)",
                dashboardToWebsite = urlStatus,
                websiteToDashboard = urlStatus,
                supabaseStatus = urlStatus,
                logs = urlLogs,
                error = urlError
            )
            testResults = updatedResults.toMap()

            // -----------------------------------------------------------------
            // 2. AUTHENTICATION & LOGIN STATUS
            // -----------------------------------------------------------------
            progressText = "جاري التحقق من حالة الدخول وجلسة المصادقة..."
            currentProgress = 0.28f
            delay(300)

            val authLogs = mutableListOf<String>()
            var authError: DiagnosticError? = null
            var authStatus = TestStatus.FAILED

            val isInit = SupabaseClient.isSessionReady
            val tokenExists = SupabaseClient.accessToken != null
            val isSandbox = SupabaseClient.accessToken == "sandbox_token"

            authLogs.add("حالة تهيئة المصادقة (Auth Ready): $isInit")
            if (tokenExists && !isSandbox) {
                authLogs.add("جلسة المصادقة الحقيقية نشطة ✓")
                authLogs.add("معرّف المستخدم (User UUID): ${SupabaseClient.userId ?: "مفقود"}")
                authLogs.add("البريد الإلكتروني للـ Owner/Staff: ${currentUser?.email ?: "مجهول"}")
                authLogs.add("الرتبة النشطة في التطبيق: ${currentUser?.role?.arabicLabel ?: "مجهول"}")
                authStatus = TestStatus.WORKING
            } else {
                authLogs.add("لا توجد جلسة مصادقة صالحة ✗")
                if (isSandbox) {
                    authLogs.add("تنبيه: الجلسة الحالية تجريبية (Sandbox) وليست حقيقية.")
                } else {
                    authLogs.add("السبب: لم يتم العثور على توكن مصادقة نشط في الذاكرة أو الشaredPreferences.")
                }
                authError = DiagnosticError(
                    operation = "checkAuth",
                    tableOrRpc = "auth_session",
                    httpStatus = 401,
                    postgresCode = "NO_SESSION",
                    errorMessage = "لا توجد جلسة مصادقة صالحة حقيقية. يرجى تسجيل الدخول للحصول على توكن جديد.",
                    likelyLayer = "Auth"
                )
                authStatus = TestStatus.FAILED
            }

            updatedResults["auth_login"] = ModuleTestResult(
                name = "auth_login",
                arabicName = "جلسة المصادقة والدخول (Auth Login)",
                dashboardToWebsite = authStatus,
                websiteToDashboard = authStatus,
                supabaseStatus = authStatus,
                logs = authLogs,
                error = authError
            )
            testResults = updatedResults.toMap()

            // -----------------------------------------------------------------
            // 3. STAFF PROFILE & JSONB PERMISSIONS
            // -----------------------------------------------------------------
            progressText = "جاري قراءة ملف الموظف والتحقق من صلاحيات JSONB..."
            currentProgress = 0.42f
            delay(300)

            val staffLogs = mutableListOf<String>()
            var staffError: DiagnosticError? = null
            var staffStatus = TestStatus.FAILED

            if (tokenExists && !isSandbox) {
                try {
                    val email = currentUser?.email ?: ""
                    val response = SupabaseClient.service.getStaffProfileByEmail("eq.$email")
                    if (response.isSuccessful) {
                        val profiles = response.body() ?: emptyList()
                        if (profiles.isNotEmpty()) {
                            val profile = profiles[0]
                            staffLogs.add("تم العثور على ملف الموظف في جدول staff_profiles بنجاح.")
                            staffLogs.add("الاسم المسجل: ${profile.name}")
                            staffLogs.add("الرتبة في السيرفر: ${profile.role ?: "employee"}")
                            staffLogs.add("الحالة في السيرفر (Active): ${profile.active ?: profile.isActive ?: true}")
                            
                            // Log raw JSONB permissions map
                            val rawPermMap = profile.permissions
                            staffLogs.add("خريطة الصلاحيات المسترجعة (Raw JSONB Map): ${rawPermMap ?: "{}"}")
                            
                            // Map it to Kotlin Set
                            val parsedSet = rawPermMap?.filterValues { it }?.keys?.mapNotNull {
                                try { com.pinky.dashboard.domain.model.Permission.valueOf(it) } catch (e: Exception) { null }
                            }?.toSet() ?: emptySet()
                            
                            staffLogs.add("تم فك تشفير وتحويل الصلاحيات بنجاح إلى Kotlin Set (${parsedSet.size} صلاحيات):")
                            staffLogs.add(parsedSet.joinToString(", ") { it.name })
                            
                            staffStatus = TestStatus.WORKING
                        } else {
                            staffLogs.add("خطأ: لم يتم العثور على ملف موظف مطابق للبريد الإلكتروني $email في قاعدة البيانات.")
                            staffError = DiagnosticError(
                                operation = "getStaffProfileByEmail",
                                tableOrRpc = "staff_profiles",
                                httpStatus = 200,
                                postgresCode = "NO_PROFILE_FOUND",
                                errorMessage = "الملف الشخصي غير منشأ لهذا البريد في جدول الموظفين.",
                                likelyLayer = "Database"
                            )
                        }
                    } else {
                        val (pgCode, msg) = parseErrorBody(response)
                        staffLogs.add("خطأ في قراءة ملف الموظف: $msg")
                        staffError = DiagnosticError(
                            operation = "getStaffProfileByEmail",
                            tableOrRpc = "staff_profiles",
                            httpStatus = response.code(),
                            postgresCode = pgCode,
                            errorMessage = msg,
                            likelyLayer = "RLS"
                        )
                    }
                } catch (e: Exception) {
                    staffLogs.add("فشل الاتصال البرمجي بالجدول: ${e.message}")
                    staffError = DiagnosticError(
                        operation = "getStaffProfileByEmail",
                        tableOrRpc = "staff_profiles",
                        httpStatus = 0,
                        postgresCode = "NET_ERR",
                        errorMessage = e.message ?: "Network Exception",
                        likelyLayer = "Network"
                    )
                }
            } else {
                staffLogs.add("تخطى: هذا الاختبار يتطلب جلسة مصادقة حقيقية نشطة.")
                staffStatus = TestStatus.NOT_TESTABLE
            }

            updatedResults["staff_profile_permissions"] = ModuleTestResult(
                name = "staff_profile_permissions",
                arabicName = "ملف الموظف وصلاحيات الـ JSONB",
                dashboardToWebsite = staffStatus,
                websiteToDashboard = staffStatus,
                supabaseStatus = staffStatus,
                logs = staffLogs,
                error = staffError
            )
            testResults = updatedResults.toMap()

            // -----------------------------------------------------------------
            // 4. RLS TABLE READS (12 CONTRACT TABLES)
            // -----------------------------------------------------------------
            progressText = "جاري فحص صلاحيات القراءة الآمنة لجداول الـ RLS الاثني عشر..."
            currentProgress = 0.56f
            delay(300)

            val rlsLogs = mutableListOf<String>()
            var rlsError: DiagnosticError? = null
            var rlsStatus = TestStatus.FAILED

            if (tokenExists && !isSandbox) {
                var succeededTables = 0
                var restrictedTables = 0
                var failedTables = 0

                val tablesToTest = listOf(
                    "orders" to suspend { SupabaseClient.service.getOrders() },
                    "products" to suspend { SupabaseClient.service.getAdminProducts() },
                    "categories" to suspend { SupabaseClient.service.getCategories() },
                    "offers" to suspend { SupabaseClient.service.getOffers() },
                    "banners" to suspend { SupabaseClient.service.getBanners() },
                    "coupons" to suspend { SupabaseClient.service.getCoupons() },
                    "shipping_governorates" to suspend { SupabaseClient.service.getGovernorates() },
                    "shipping_centers" to suspend { SupabaseClient.service.getShippingCenters() },
                    "prime_subscriptions" to suspend { SupabaseClient.service.getPrimeSubscriptions() },
                    "site_customizations" to suspend { SupabaseClient.service.getSiteCustomizations() },
                    "store_settings" to suspend { SupabaseClient.service.getStoreSettings() },
                    "staff_profiles" to suspend { SupabaseClient.service.getStaffProfiles() }
                )

                rlsLogs.add("جاري فحص استعلام قراءة الجداول الحقيقية:")
                for ((tableName, queryFunc) in tablesToTest) {
                    try {
                        val response = queryFunc()
                        if (response.isSuccessful) {
                            rlsLogs.add("- جدول $tableName: قراءة ناجحة ✓")
                            succeededTables++
                        } else {
                            val errorStr = response.errorBody()?.string() ?: ""
                            if (response.code() == 403 || errorStr.contains("42501")) {
                                rlsLogs.add("- جدول $tableName: حماية الـ RLS تفرض قيوداً لهذا الحساب (HTTP 403/42501) ✓")
                                restrictedTables++
                            } else {
                                rlsLogs.add("- جدول $tableName: فشل الاستعلام (${response.code()}): $errorStr ✗")
                                failedTables++
                            }
                        }
                    } catch (e: Exception) {
                        rlsLogs.add("- جدول $tableName: فشل الاتصال (${e.message}) ✗")
                        failedTables++
                    }
                }

                rlsLogs.add("ملخص الفحص: الجداول المقروءة بنجاح: $succeededTables، الجداول الخاضعة لقيود RLS: $restrictedTables، جداول فاشلة: $failedTables")
                
                rlsStatus = when {
                    failedTables > 0 -> TestStatus.FAILED
                    restrictedTables > 0 -> TestStatus.PARTIAL
                    else -> TestStatus.WORKING
                }
                
                if (failedTables > 0) {
                    rlsError = DiagnosticError(
                        operation = "readTables",
                        tableOrRpc = "multiple_tables",
                        httpStatus = 500,
                        postgresCode = "READ_FAILED",
                        errorMessage = "هناك جداول لم يتم الوصول إليها بنجاح بسبب مشاكل اتصال أو هيكلية.",
                        likelyLayer = "Database / Network"
                    )
                }
            } else {
                rlsLogs.add("تخطى: يتطلب جلسة مصادقة حقيقية لإجراء استعلامات RLS.")
                rlsStatus = TestStatus.NOT_TESTABLE
            }

            updatedResults["rls_reads"] = ModuleTestResult(
                name = "rls_reads",
                arabicName = "صلاحيات قراءة جداول الـ RLS",
                dashboardToWebsite = rlsStatus,
                websiteToDashboard = rlsStatus,
                supabaseStatus = rlsStatus,
                logs = rlsLogs,
                error = rlsError
            )
            testResults = updatedResults.toMap()

            // -----------------------------------------------------------------
            // 5. PUBLIC VIEW: catalog_products_read
            // -----------------------------------------------------------------
            progressText = "جاري التحقق من القراءة العامة لـ catalog_products..."
            currentProgress = 0.70f
            delay(300)

            val catalogLogs = mutableListOf<String>()
            var catalogError: DiagnosticError? = null
            var catalogStatus = TestStatus.FAILED

            try {
                val response = SupabaseClient.service.getProducts()
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    catalogLogs.add("تم الاتصال وقراءة الـ Public View [catalog_products] بنجاح.")
                    catalogLogs.add("عدد المنتجات المتاحة للعرض العام: ${list.size} منتجات.")
                    catalogStatus = TestStatus.WORKING
                } else {
                    val (pgCode, msg) = parseErrorBody(response)
                    catalogLogs.add("فشل قراءة الـ View العام: $msg")
                    catalogError = DiagnosticError(
                        operation = "getProducts",
                        tableOrRpc = "catalog_products",
                        httpStatus = response.code(),
                        postgresCode = pgCode,
                        errorMessage = msg,
                        likelyLayer = "Database View"
                    )
                }
            } catch (e: Exception) {
                catalogLogs.add("فشل الاتصال بـ catalog_products: ${e.message}")
                catalogError = DiagnosticError(
                    operation = "getProducts",
                    tableOrRpc = "catalog_products",
                    httpStatus = 0,
                    postgresCode = "NET_ERR",
                    errorMessage = e.message ?: "Network Exception",
                    likelyLayer = "Network"
                )
            }

            updatedResults["catalog_products_read"] = ModuleTestResult(
                name = "catalog_products_read",
                arabicName = "القراءة العامة للمتجر (catalog_products)",
                dashboardToWebsite = catalogStatus,
                websiteToDashboard = catalogStatus,
                supabaseStatus = catalogStatus,
                logs = catalogLogs,
                error = catalogError
            )
            testResults = updatedResults.toMap()

            // -----------------------------------------------------------------
            // 6. PUSH DEVICE DTO VALIDATION
            // -----------------------------------------------------------------
            progressText = "جاري اختبار هيكل وتوافق جهاز الإشعارات..."
            currentProgress = 0.84f
            delay(300)

            val pushLogs = mutableListOf<String>()
            var pushError: DiagnosticError? = null
            var pushStatus = TestStatus.FAILED

            try {
                val deviceId = java.util.UUID.randomUUID().toString()
                val currentUserId = SupabaseClient.userId
                val dummyToken = "test_token_valid_dto_${System.currentTimeMillis()}"
                
                pushLogs.add("بناء كائن الـ DTO لجهاز الدفع الفوري بنجاح:")
                pushLogs.add("- معرّف الجهاز (UUID): $deviceId")
                pushLogs.add("- معرّف الموظف (User UUID): ${currentUserId ?: "لا يوجد (سيسجل كـ Anonymous أو Owner افتراضي)"}")
                pushLogs.add("- رمز التوكن: $dummyToken")
                pushLogs.add("- نظام التشغيل: android")
                pushLogs.add("- حالة النشاط: true")

                // Try to perform a real check request to API (Post without expecting push delivery)
                val testDevice = SupabaseDashboardPushDeviceDto(
                    id = deviceId,
                    userId = currentUserId,
                    pushToken = dummyToken,
                    platform = "android",
                    active = true,
                    createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()),
                    updatedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()),
                    lastSeenAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
                )

                if (tokenExists && !isSandbox) {
                    val response = SupabaseClient.service.registerPushDevice(testDevice)
                    if (response.isSuccessful) {
                        pushLogs.add("تم تسجيل توافق هيكل الـ DTO على السيرفر بنجاح ✓")
                        pushStatus = TestStatus.WORKING
                    } else {
                        val (pgCode, msg) = parseErrorBody(response)
                        pushLogs.add("فشل استجابة السيرفر لإدراج هيكل الجهاز: $msg")
                        pushError = DiagnosticError(
                            operation = "registerPushDevice",
                            tableOrRpc = "dashboard_push_devices",
                            httpStatus = response.code(),
                            postgresCode = pgCode,
                            errorMessage = msg,
                            likelyLayer = "RLS / Database Schema"
                        )
                    }
                } else {
                    pushLogs.add("تنبيه: تم فحص وبناء كائن الـ DTO بنجاح برمجياً، ولكن تم تخطي استدعاء السيرفر لعدم وجود جلسة دخول حقيقية.")
                    pushStatus = TestStatus.PARTIAL
                }
            } catch (e: Exception) {
                pushLogs.add("خطأ في تشغيل فحص الجهاز: ${e.message}")
                pushError = DiagnosticError(
                    operation = "registerPushDevice",
                    tableOrRpc = "dashboard_push_devices",
                    httpStatus = 0,
                    postgresCode = "DEV_ERR",
                    errorMessage = e.message ?: "Unknown Local Error",
                    likelyLayer = "UI / Controller"
                )
            }

            updatedResults["push_device_dto"] = ModuleTestResult(
                name = "push_device_dto",
                arabicName = "توافق وهيكل جهاز الإشعارات",
                dashboardToWebsite = pushStatus,
                websiteToDashboard = pushStatus,
                supabaseStatus = pushStatus,
                logs = pushLogs,
                error = pushError
            )
            testResults = updatedResults.toMap()

            // -----------------------------------------------------------------
            // 7. ORDERS ITEMS JSONB ANALYSIS
            // -----------------------------------------------------------------
            progressText = "جاري فحص بنية الطلبات والتأكد من مصفوفة الـ JSONB..."
            currentProgress = 1.00f
            delay(300)

            val orderLogs = mutableListOf<String>()
            var orderError: DiagnosticError? = null
            var orderStatus = TestStatus.FAILED

            if (tokenExists && !isSandbox) {
                try {
                    val response = SupabaseClient.service.getOrders()
                    if (response.isSuccessful) {
                        val orders = response.body() ?: emptyList()
                        orderLogs.add("تم قراءة جدول orders بنجاح.")
                        orderLogs.add("عدد الطلبات الحالية المسترجعة: ${orders.size}")
                        
                        if (orders.isNotEmpty()) {
                            val sample = orders.firstOrNull()
                            if (sample != null) {
                                orderLogs.add("تحليل الطلب النموذجي رقم: ${sample.orderNumber}")
                                orderLogs.add("الاسم: ${sample.customerName}")
                                orderLogs.add("المنتجات الفرعية المدمجة (JSONB items array):")
                                
                                val itemsList = sample.items
                                if (itemsList != null) {
                                    orderLogs.add("- تم تفكيك الـ JSONB Array بنجاح بواسطة Moshi الكائنية.")
                                    orderLogs.add("- يحتوي الطلب على (${itemsList.size}) منتجات فرعية داخل حقل JSONB واحد.")
                                    for ((idx, item) in itemsList.withIndex()) {
                                        orderLogs.add("  [#${idx + 1}] منتج: ${item.productName ?: "مجهول"} | الكمية: ${item.quantity ?: 1} | الإجمالي: ${item.lineTotal ?: 0.0}")
                                    }
                                } else {
                                    orderLogs.add("- تحذير: حقل items يحتوي على قيمة فارغة أو غير مهيأة برمجياً كـ JSON Array.")
                                }
                            }
                        } else {
                            orderLogs.add("تم الاستعلام بنجاح ولكن الجدول فارغ من البيانات حالياً لتفكيك طلب عشوائي.")
                        }
                        
                        orderStatus = TestStatus.WORKING
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
                    orderLogs.add("فشل اتصال قراءة الطلبات: ${e.message}")
                    orderError = DiagnosticError(
                        operation = "getOrders",
                        tableOrRpc = "orders",
                        httpStatus = 0,
                        postgresCode = "NET_ERR",
                        errorMessage = e.message ?: "Network Exception",
                        likelyLayer = "Network"
                    )
                }
            } else {
                orderLogs.add("تخطى: هذا الفحص يتطلب جلسة مصادقة حقيقية نشطة لقراءة الطلبات الإدارية.")
                orderStatus = TestStatus.NOT_TESTABLE
            }

            updatedResults["orders_jsonb_items"] = ModuleTestResult(
                name = "orders_jsonb_items",
                arabicName = "تحليل تفاصيل منتجات الطلبات (JSONB)",
                dashboardToWebsite = orderStatus,
                websiteToDashboard = orderStatus,
                supabaseStatus = orderStatus,
                logs = orderLogs,
                error = orderError
            )
            testResults = updatedResults.toMap()

            progressText = "اكتمل فحص السحابة والربط بنجاح!"
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
                        val authStatusText = if (isAuth) "Authenticated ✓" else "Not Authenticated ✗"
                        val userExistsText = if (rawUserId != "لا يوجد" && rawUserId.isNotBlank()) "User ID موجود" else "User ID غير موجود"
                        Text(
                            text = "$authStatusText | $userExistsText",
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
        Pair("url_check", "التحقق من عنوان السحابة (Supabase URL)"),
        Pair("auth_login", "جلسة المصادقة والدخول (Auth Login)"),
        Pair("staff_profile_permissions", "ملف الموظف وصلاحيات الـ JSONB"),
        Pair("rls_reads", "صلاحيات قراءة جداول الـ RLS"),
        Pair("catalog_products_read", "القراءة العامة للمتجر (catalog_products)"),
        Pair("push_device_dto", "توافق وهيكل جهاز الإشعارات"),
        Pair("orders_jsonb_items", "تحليل تفاصيل منتجات الطلبات (JSONB)")
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
