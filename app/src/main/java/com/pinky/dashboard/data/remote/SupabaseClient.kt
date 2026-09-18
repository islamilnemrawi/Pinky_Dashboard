package com.pinky.dashboard.data.remote

import com.pinky.dashboard.BuildConfig
import com.pinky.dashboard.domain.model.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

// DATA TRANSFER OBJECTS (DTOs) FOR SUPABASE

@JsonClass(generateAdapter = true)
data class SupabaseCategoryDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "sort_order") val sortOrder: Int? = null,
    @Json(name = "active") val active: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseProductDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "gallery") val gallery: List<String>? = null,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "category_name") val categoryName: String? = null,
    @Json(name = "price") val price: Double,
    @Json(name = "old_price") val oldPrice: Double? = null,
    @Json(name = "discount_percent") val discountPercent: Int? = null,
    @Json(name = "wholesale_price") val wholesalePrice: Double? = null,
    @Json(name = "internal_code") val internalCode: String? = null,
    @Json(name = "stock") val stock: Int? = null,
    @Json(name = "colors") val colors: List<String>? = null,
    @Json(name = "sizes") val sizes: List<String>? = null,
    @Json(name = "is_active") val isActive: Boolean? = null,
    @Json(name = "is_featured") val isFeatured: Boolean? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseCustomerDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "governorate") val governorate: String? = null,
    @Json(name = "total_orders") val totalOrders: Int? = null,
    @Json(name = "total_spend") val totalSpend: Double? = null,
    @Json(name = "first_order_date") val firstOrderDate: String? = null,
    @Json(name = "last_order_date") val lastOrderDate: String? = null,
    @Json(name = "is_prime_member") val isPrimeMember: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseOrderItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "order_id") val orderId: String? = null,
    @Json(name = "product_id") val productId: String? = null,
    @Json(name = "product_name") val productName: String? = null,
    @Json(name = "variant_name") val variantName: String? = null,
    @Json(name = "color") val color: String? = null,
    @Json(name = "size") val size: String? = null,
    @Json(name = "quantity") val quantity: Int? = null,
    @Json(name = "unit_price") val unitPrice: Double? = null,
    @Json(name = "line_total") val lineTotal: Double? = null,
    @Json(name = "image_url") val imageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseOrderDto(
    @Json(name = "id") val id: String,
    @Json(name = "order_number") val orderNumber: String,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "address") val address: String? = null,
    @Json(name = "governorate") val governorate: String? = null,
    @Json(name = "center") val center: String? = null,
    @Json(name = "subtotal") val subtotal: Double? = null,
    @Json(name = "shipping_cost") val shippingCost: Double? = null,
    @Json(name = "discount_amount") val discountAmount: Double? = null,
    @Json(name = "total") val total: Double? = null,
    @Json(name = "payment_method") val paymentMethod: String? = null,
    @Json(name = "payment_status") val paymentStatus: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "items") val items: List<SupabaseOrderItemDto>? = null
)

// RETROFIT INTERFACE FOR SUPABASE REST API

@JsonClass(generateAdapter = true)
data class SupabaseOfferDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "discount_text") val discountText: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "button_text") val buttonText: String? = null,
    @Json(name = "target_category_id") val targetCategoryId: String? = null,
    @Json(name = "target_category_name") val targetCategoryName: String? = null,
    @Json(name = "sort_order") val sortOrder: Int? = null,
    @Json(name = "is_active") val isActive: Boolean? = null,
    @Json(name = "start_date") val startDate: String? = null,
    @Json(name = "end_date") val endDate: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseGovernorateDto(
    @Json(name = "governorate") val governorate: String,
    @Json(name = "price") val price: Double,
    @Json(name = "centers_enabled") val centersEnabled: Boolean? = null,
    @Json(name = "active") val active: Boolean? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseShippingCenterDto(
    @Json(name = "id") val id: String,
    @Json(name = "governorate") val governorate: String,
    @Json(name = "center") val center: String,
    @Json(name = "price") val price: Double,
    @Json(name = "active") val active: Boolean? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseLoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class SupabaseRefreshRequest(
    @Json(name = "refresh_token") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class SupabaseLoginResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "expires_in") val expiresIn: Long,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "user") val user: SupabaseAuthUserDto
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthUserDto(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String?
)

@JsonClass(generateAdapter = true)
data class SupabasePrimeSubscriptionDto(
    @Json(name = "id") val id: String,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "start_date") val startDate: String,
    @Json(name = "expiry_date") val expiryDate: String,
    @Json(name = "status") val status: String,
    @Json(name = "total_orders") val totalOrders: Int? = 0,
    @Json(name = "total_saved") val totalSaved: Double? = 0.0
)

@JsonClass(generateAdapter = true)
data class SupabaseCouponDto(
    @Json(name = "id") val id: String,
    @Json(name = "code") val code: String,
    @Json(name = "discount_type") val discountType: String,
    @Json(name = "discount_value") val discountValue: Double,
    @Json(name = "min_order_amount") val minOrderAmount: Double? = 0.0,
    @Json(name = "max_discount_amount") val maxDiscountAmount: Double? = null,
    @Json(name = "is_free_shipping") val isFreeShipping: Boolean? = false,
    @Json(name = "usage_limit") val usageLimit: Int? = null,
    @Json(name = "usage_count") val usageCount: Int? = 0,
    @Json(name = "is_first_order_only") val isFirstOrderOnly: Boolean? = false,
    @Json(name = "start_date") val startDate: String? = null,
    @Json(name = "end_date") val endDate: String? = null,
    @Json(name = "is_active") val isActive: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class SupabaseStoreSettingsDto(
    @Json(name = "id") val id: String = "settings_1",
    @Json(name = "store_name") val storeName: String,
    @Json(name = "whatsapp_number") val whatsappNumber: String,
    @Json(name = "instagram_handle") val instagramHandle: String,
    @Json(name = "facebook_page") val facebookPage: String
)

@JsonClass(generateAdapter = true)
data class SupabaseSiteCustomizationsDto(
    @Json(name = "id") val id: String = "customization_1",
    @Json(name = "announcement_bar_text") val announcementBarText: String,
    @Json(name = "show_announcement_bar") val showAnnouncementBar: Boolean,
    @Json(name = "hero_title") val heroTitle: String,
    @Json(name = "hero_subtitle") val heroSubtitle: String,
    @Json(name = "hero_button_text") val heroButtonText: String,
    @Json(name = "hero_image_url") val heroImageUrl: String,
    @Json(name = "show_hero_banner") val showHeroBanner: Boolean,
    @Json(name = "show_about_section") val showAboutSection: Boolean,
    @Json(name = "about_text") val aboutText: String,
    @Json(name = "primary_color_hex") val primaryColorHex: String,
    @Json(name = "font_family") val fontFamily: String,
    @Json(name = "sections_json") val sectionsJson: String? = null,
    @Json(name = "config") val config: SiteCustomizationConfig? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseStaffProfileDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "role") val role: String,
    @Json(name = "permissions") val permissions: String,
    @Json(name = "is_active") val isActive: Boolean? = true,
    @Json(name = "last_active") val lastActive: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseDashboardPushDeviceDto(
    @Json(name = "id") val id: String,
    @Json(name = "device_name") val deviceName: String,
    @Json(name = "push_token") val pushToken: String,
    @Json(name = "last_active") val lastActive: String
)

interface SupabaseService {

    // CATEGORIES
    @GET("rest/v1/categories?select=*")
    suspend fun getCategories(): Response<List<SupabaseCategoryDto>>

    @POST("rest/v1/categories")
    suspend fun upsertCategory(
        @Body category: SupabaseCategoryDto,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates"
    ): Response<Unit>

    @DELETE("rest/v1/categories")
    suspend fun deleteCategory(@Query("id") filter: String): Response<Unit>

    // STORAGE UPLOAD API
    @POST("storage/v1/object/{bucket}/{path}")
    suspend fun uploadStorageFile(
        @Path("bucket") bucket: String,
        @Path("path") path: String,
        @Body file: okhttp3.RequestBody,
        @Header("Content-Type") contentType: String = "image/jpeg"
    ): Response<Unit>


    // PRODUCTS (using catalog_products as specified!)
    @GET("rest/v1/catalog_products?select=*")
    suspend fun getProducts(): Response<List<SupabaseProductDto>>

    @POST("rest/v1/catalog_products")
    suspend fun upsertProduct(
        @Body product: SupabaseProductDto,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates"
    ): Response<Unit>

    @DELETE("rest/v1/catalog_products")
    suspend fun deleteProduct(@Query("id") filter: String): Response<Unit>

    @PATCH("rest/v1/catalog_products")
    suspend fun patchProduct(
        @Query("id") filter: String,
        @Body updates: Map<String, Boolean>
    ): Response<Unit>


    // CUSTOMERS
    @GET("rest/v1/customers?select=*")
    suspend fun getCustomers(): Response<List<SupabaseCustomerDto>>

    @POST("rest/v1/customers")
    suspend fun upsertCustomer(
        @Body customer: SupabaseCustomerDto,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates"
    ): Response<Unit>


    // ORDERS
    @GET("rest/v1/orders?select=*")
    suspend fun getOrders(): Response<List<SupabaseOrderDto>>

    @POST("rest/v1/orders")
    suspend fun upsertOrder(
        @Body order: SupabaseOrderDto,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates"
    ): Response<Unit>

    @POST("rest/v1/order_items")
    suspend fun insertOrderItems(
        @Body items: List<SupabaseOrderItemDto>
    ): Response<Unit>

    @PATCH("rest/v1/orders")
    suspend fun updateOrderStatus(
        @Query("id") filter: String,
        @Body updates: Map<String, String>
    ): Response<Unit>

    @DELETE("rest/v1/orders")
    suspend fun deleteOrder(@Query("id") filter: String): Response<Unit>


    // OFFERS & BANNERS
    @GET("rest/v1/offers?select=*")
    suspend fun getOffers(): Response<List<SupabaseOfferDto>>

    @POST("rest/v1/offers")
    suspend fun upsertOffer(
        @Body offer: SupabaseOfferDto,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates"
    ): Response<Unit>

    @DELETE("rest/v1/offers")
    suspend fun deleteOffer(@Query("id") filter: String): Response<Unit>

    @GET("rest/v1/banners?select=*")
    suspend fun getBanners(): Response<List<SupabaseOfferDto>>

    @POST("rest/v1/banners")
    suspend fun upsertBanner(
        @Body banner: SupabaseOfferDto,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates"
    ): Response<Unit>

    @DELETE("rest/v1/banners")
    suspend fun deleteBanner(@Query("id") filter: String): Response<Unit>


    // SHIPPING GOVERNORATES & CENTERS
    @GET("rest/v1/shipping_governorates?select=*")
    suspend fun getGovernorates(): Response<List<SupabaseGovernorateDto>>

    @POST("rest/v1/shipping_governorates")
    suspend fun upsertGovernorate(
        @Body gov: SupabaseGovernorateDto,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates"
    ): Response<Unit>

    @GET("rest/v1/shipping_centers?select=*")
    suspend fun getShippingCenters(): Response<List<SupabaseShippingCenterDto>>

    @POST("rest/v1/shipping_centers")
    suspend fun upsertShippingCenter(
        @Body center: SupabaseShippingCenterDto,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates"
    ): Response<Unit>

    @DELETE("rest/v1/shipping_centers")
    suspend fun deleteShippingCenter(@Query("id") filter: String): Response<Unit>


    // PRIME SUBSCRIPTIONS
    @GET("rest/v1/prime_subscriptions?select=*")
    suspend fun getPrimeSubscriptions(): Response<List<SupabasePrimeSubscriptionDto>>

    @POST("rest/v1/prime_subscriptions")
    suspend fun upsertPrimeSubscription(
        @Body sub: SupabasePrimeSubscriptionDto,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates"
    ): Response<Unit>

    @DELETE("rest/v1/prime_subscriptions")
    suspend fun deletePrimeSubscription(@Query("user_id") filter: String): Response<Unit>


    // COUPONS
    @GET("rest/v1/coupons?select=*")
    suspend fun getCoupons(): Response<List<SupabaseCouponDto>>

    @POST("rest/v1/coupons")
    suspend fun upsertCoupon(
        @Body coupon: SupabaseCouponDto,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates"
    ): Response<Unit>

    @DELETE("rest/v1/coupons")
    suspend fun deleteCoupon(@Query("id") filter: String): Response<Unit>

    @PATCH("rest/v1/coupons")
    suspend fun patchCoupon(
        @Query("id") filter: String,
        @Body updates: Map<String, Boolean>
    ): Response<Unit>


    // STORE SETTINGS
    @GET("rest/v1/store_settings?select=*")
    suspend fun getStoreSettings(): Response<List<SupabaseStoreSettingsDto>>

    @POST("rest/v1/store_settings")
    suspend fun upsertStoreSettings(
        @Body settings: SupabaseStoreSettingsDto,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates"
    ): Response<Unit>


    // SITE CUSTOMIZATIONS
    @GET("rest/v1/site_customizations?select=*")
    suspend fun getSiteCustomizations(): Response<List<SupabaseSiteCustomizationsDto>>

    @POST("rest/v1/site_customizations")
    suspend fun upsertSiteCustomizations(
        @Body customizations: SupabaseSiteCustomizationsDto,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates"
    ): Response<Unit>


    // STAFF PROFILES
    @GET("rest/v1/staff_profiles?select=*")
    suspend fun getStaffProfiles(): Response<List<SupabaseStaffProfileDto>>

    @POST("rest/v1/staff_profiles")
    suspend fun upsertStaffProfile(
        @Body staff: SupabaseStaffProfileDto,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates"
    ): Response<Unit>

    @DELETE("rest/v1/staff_profiles")
    suspend fun deleteStaffProfile(@Query("id") filter: String): Response<Unit>

    @PATCH("rest/v1/staff_profiles")
    suspend fun patchStaffProfile(
        @Query("id") filter: String,
        @Body updates: Map<String, Boolean>
    ): Response<Unit>

    @GET("rest/v1/staff_profiles")
    suspend fun getStaffProfileByEmail(
        @Query("email") emailFilter: String
    ): Response<List<SupabaseStaffProfileDto>>

    @POST("auth/v1/token?grant_type=password")
    suspend fun login(
        @Body request: SupabaseLoginRequest
    ): Response<SupabaseLoginResponse>

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refresh(
        @Body request: SupabaseRefreshRequest
    ): Response<SupabaseLoginResponse>


    // PUSH DEVICES
    @POST("rest/v1/dashboard_push_devices")
    suspend fun registerPushDevice(
        @Body device: SupabaseDashboardPushDeviceDto,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates"
    ): Response<Unit>
}

// CENTRAL CLIENT SINGLETON

object SupabaseClient {
    @Volatile
    var accessToken: String? = null
        set(value) {
            field = value
            _userToken = value
        }

    @Volatile
    private var _userToken: String? = null

    var userToken: String?
        get() = _userToken
        set(value) {
            _userToken = value
            accessToken = value
        }

    @Volatile
    var refreshToken: String? = null

    @Volatile
    var userId: String? = null

    @Volatile
    var expiresAt: Long = 0L

    @Volatile
    @JvmField
    var isSessionReady: Boolean = false

    @Volatile
    var isAuthenticated: Boolean = false

    @Volatile
    private var sharedPrefs: android.content.SharedPreferences? = null

    private val refreshMutex = kotlinx.coroutines.sync.Mutex()

    fun initPrefs(context: android.content.Context) {
        sharedPrefs = context.getSharedPreferences("pinky_auth_prefs", android.content.Context.MODE_PRIVATE)
    }

    suspend fun performTokenRefresh(): Boolean {
        val rToken = refreshToken ?: sharedPrefs?.getString("refresh_token", null)
        if (rToken == null) return false

        return refreshMutex.withLock {
            val now = System.currentTimeMillis()
            if ((expiresAt - now) >= 5 * 60 * 1000L) {
                return@withLock true
            }

            android.util.Log.i("SupabaseAuth", "AUTH_SESSION_REFRESH_STARTED")
            try {
                val response = service.refresh(SupabaseRefreshRequest(rToken))
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null) {
                        val newAccessToken = loginResponse.accessToken
                        val newRefreshToken = loginResponse.refreshToken
                        val expiresInSeconds = loginResponse.expiresIn
                        val newExpiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)

                        accessToken = newAccessToken
                        refreshToken = newRefreshToken
                        expiresAt = newExpiresAt
                        isAuthenticated = true
                        isSessionReady = true

                        sharedPrefs?.edit()?.apply {
                            putString("user_token", newAccessToken)
                            putString("refresh_token", newRefreshToken)
                            putLong("expires_at", newExpiresAt)
                            apply()
                        }

                        android.util.Log.i("SupabaseAuth", "AUTH_SESSION_REFRESHED: user_id=$userId")
                        return@withLock true
                    }
                } else {
                    val errorCode = response.code()
                    val errorMsg = response.errorBody()?.string() ?: ""
                    android.util.Log.e("SupabaseAuth", "Refresh token failed (HTTP $errorCode): $errorMsg")
                    if (errorCode in 400..403) {
                        clearSession()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SupabaseAuth", "Refresh token exception: ${e.message}", e)
            }
            return@withLock false
        }
    }

    fun clearSession() {
        accessToken = null
        refreshToken = null
        userId = null
        expiresAt = 0L
        isAuthenticated = false
        isSessionReady = true

        sharedPrefs?.edit()?.clear()?.apply()
    }

    val supabaseUrl: String by lazy {
        val buildConfigUrl = try {
            BuildConfig.SUPABASE_URL
        } catch (e: Throwable) {
            ""
        }
        if (buildConfigUrl.isNotBlank() && !buildConfigUrl.contains("placeholder")) {
            buildConfigUrl
        } else {
            SupabaseConfig.DEFAULT_SUPABASE_URL
        }
    }

    val supabaseAnonKey: String by lazy {
        val buildConfigKey = try {
            BuildConfig.SUPABASE_ANON_KEY
        } catch (e: Throwable) {
            ""
        }
        if (buildConfigKey.isNotBlank() && !buildConfigKey.contains("placeholder")) {
            buildConfigKey
        } else {
            SupabaseConfig.DEFAULT_ANON_KEY
        }
    }

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()

                // 1. Wait for Auth Initialization if not ready (skip for login / refresh requests to avoid deadlock)
                val path = original.url.encodedPath
                val isAuthRequest = path.contains("/auth/v1/")
                if (!isSessionReady && !isAuthRequest) {
                    android.util.Log.d("SupabaseAuth", "Interceptor waiting for Auth initialization... url=${original.url}")
                    val start = System.currentTimeMillis()
                    while (!isSessionReady && (System.currentTimeMillis() - start) < 10000L) {
                        try { Thread.sleep(100) } catch (e: Exception) { break }
                    }
                }

                // 2. Auto refresh token before request if expired/near-expiry
                val currentToken = accessToken
                val currentRefreshToken = refreshToken
                val currentExpiresAt = expiresAt
                if (currentToken != null && currentToken.isNotBlank() && currentToken != "sandbox_token" && currentRefreshToken != null && !isAuthRequest) {
                    val now = System.currentTimeMillis()
                    if ((currentExpiresAt - now) < 5 * 60 * 1000L) {
                        android.util.Log.d("SupabaseAuth", "Interceptor detected token near expiry, refreshing... (expiresAt=$currentExpiresAt, now=$now)")
                        try {
                            kotlinx.coroutines.runBlocking {
                                performTokenRefresh()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SupabaseAuth", "Interceptor refresh token failed", e)
                        }
                    }
                }

                val hasAuth = original.header("Authorization") != null
                val hasContentType = original.header("Content-Type") != null
                val hasApiKey = original.header("apikey") != null
                val request = original.newBuilder()
                    .apply {
                        if (!hasApiKey) {
                            addHeader("apikey", supabaseAnonKey)
                        }
                        if (!hasAuth) {
                            val activeToken = accessToken
                            if (activeToken != null && activeToken.isNotBlank() && activeToken != "sandbox_token") {
                                addHeader("Authorization", "Bearer $activeToken")
                                android.util.Log.d("SupabaseAuth", "REQUEST_AUTH_ATTACHED=true | REQUEST_ROLE=authenticated")
                            } else {
                                android.util.Log.d("SupabaseAuth", "REQUEST_AUTH_ATTACHED=false | REQUEST_ROLE=anonymous")
                            }
                        } else {
                            android.util.Log.d("SupabaseAuth", "REQUEST_AUTH_ATTACHED=true (pre-existing)")
                        }
                        if (!hasContentType) {
                            addHeader("Content-Type", "application/json")
                        }
                    }
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }

    val service: SupabaseService by lazy {
        val baseUrl = if (supabaseUrl.endsWith("/")) supabaseUrl else "$supabaseUrl/"
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseService::class.java)
    }
}
