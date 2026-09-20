package com.pinky.dashboard.ui

import android.content.Context
import com.pinky.dashboard.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pinky.dashboard.PinkyApplication
import com.pinky.dashboard.core.sync.OrderSyncManager
import com.pinky.dashboard.core.sync.SyncInterval
import com.pinky.dashboard.data.local.PinkyDatabase
import com.pinky.dashboard.data.remote.*
import com.pinky.dashboard.data.repository.*
import com.pinky.dashboard.domain.model.*
import com.pinky.dashboard.domain.repository.*
import com.pinky.dashboard.ui.navigation.DashboardSection
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive

private fun isNetworkException(e: Throwable?): Boolean {
    if (e == null) return false
    val msg = e.message ?: ""
    return e is java.net.UnknownHostException ||
           e is java.net.ConnectException ||
           e is java.net.SocketTimeoutException ||
           e is java.io.InterruptedIOException ||
           msg.contains("Unable to resolve host", ignoreCase = true) ||
           msg.contains("No address associated with hostname", ignoreCase = true) ||
           msg.contains("Failed to connect", ignoreCase = true)
}

class PinkyViewModel(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val offerRepository: OfferRepository,
    private val customerRepository: CustomerRepository,
    private val shippingRepository: ShippingRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val authRepository: AuthRepository,
    private val syncManager: OrderSyncManager
) : ViewModel() {

    private val _currentSection = MutableStateFlow(DashboardSection.HOME)
    val currentSection: StateFlow<DashboardSection> = _currentSection.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _selectedTimeframeDays = MutableStateFlow(30)
    val selectedTimeframeDays: StateFlow<Int> = _selectedTimeframeDays.asStateFlow()

    private val _selectedOrder = MutableStateFlow<Order?>(null)
    val selectedOrder: StateFlow<Order?> = _selectedOrder.asStateFlow()

    val currentUser: StateFlow<AdminUser?> = authRepository.getCurrentUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val orders: StateFlow<List<Order>> = orderRepository.getOrdersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = productRepository.getProductsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.getCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offers: StateFlow<List<Offer>> = offerRepository.getOffersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = customerRepository.getCustomersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val governorates: StateFlow<List<ShippingGovernorate>> = shippingRepository.getGovernoratesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shippingCenters: StateFlow<List<ShippingCenter>> = shippingRepository.getShippingCentersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val analytics: StateFlow<AnalyticsData> = _selectedTimeframeDays
        .flatMapLatest { days -> analyticsRepository.getAnalyticsFlow(days) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AnalyticsData()
        )

    val isSyncing = syncManager.isSyncing
    val syncInterval = syncManager.syncInterval

    // In-memory state flows for sections
    private val _coupons = MutableStateFlow<List<Coupon>>(emptyList())
    val coupons: StateFlow<List<Coupon>> = _coupons.asStateFlow()

    private val _primeSubscribers = MutableStateFlow<List<PrimeSubscriber>>(emptyList())
    val primeSubscribers: StateFlow<List<PrimeSubscriber>> = _primeSubscribers.asStateFlow()

    private val _staffMembers = MutableStateFlow<List<StaffMember>>(emptyList())
    val staffMembers: StateFlow<List<StaffMember>> = _staffMembers.asStateFlow()

    private val _websiteConfig = MutableStateFlow(WebsiteConfig())
    val websiteConfig: StateFlow<WebsiteConfig> = _websiteConfig.asStateFlow()

    private val _notifications = MutableStateFlow<List<DashboardNotification>>(emptyList())
    val notifications: StateFlow<List<DashboardNotification>> = _notifications.asStateFlow()

    private val _paymentMethods = MutableStateFlow<List<PaymentMethodConfig>>(
        listOf(
            PaymentMethodConfig(
                id = "pm_1",
                key = "credit_card",
                name = "البطاقات الائتمانية (Visa / MasterCard)",
                description = "بوابة دفع آمنة ومؤمنة لمعالجة بطاقات الفيزا والماستركارد عبر بوابة الدفع الإلكترونية Paymob. لا توجد بيانات سرية مخزنة داخل التطبيق.",
                isEnabled = true,
                feeOrDiscountText = "معالجة آمنة عبر بوابة Paymob",
                statusText = "نشط ومفعل"
            ),
            PaymentMethodConfig(
                id = "pm_2",
                key = "vodafone_cash",
                name = "فودافون كاش ومحافظ الهاتف",
                description = "معالجة فورية للمحافظ الإلكترونية للهواتف المحمولة وتتم تسويتها إلكترونياً بالكامل عبر بوابة Paymob.",
                isEnabled = true,
                feeOrDiscountText = "معالجة فورية عبر Paymob",
                statusText = "نشط"
            ),
            PaymentMethodConfig(
                id = "pm_3",
                key = "cod",
                name = "الدفع عند الاستلام (COD)",
                description = "تحصيل قيمة الأوردر نقداً أو بالبطاقة عند الاستلام عبر مندوب الشحن بدون بوابة دفع إلكترونية.",
                isEnabled = true,
                feeOrDiscountText = "بدون رسوم إضافية",
                statusText = "نشط"
            )
        )
    )
    val paymentMethods: StateFlow<List<PaymentMethodConfig>> = _paymentMethods.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<String?>(null)
    val lastSyncTimestamp: StateFlow<String?> = _lastSyncTimestamp.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private var autoRefreshJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            // 1. Load, restore or refresh the local/silent Owner session completely sequentially
            val isAuthenticated = authRepository.initializeAuthSession()
            android.util.Log.i("SupabaseAuth", "AUTH_SESSION_READY - authenticated = $isAuthenticated")

            // 2. Start background periodic sync
            syncManager.startPeriodicSync()

            // 3. Perform the initial data sync only when authenticated/sandbox is ready
            if (isAuthenticated) {
                performCentralizedSync()
            }

            // 4. Start the 60-second background automatic refresh and sync timer
            startAutoRefreshTimer()
        }
    }

    suspend fun login(email: String, pass: String): Result<AdminUser> {
        val result = authRepository.login(email, pass)
        if (result.isSuccess) {
            // Immediately sync data upon a manual successful login
            performCentralizedSync()
        }
        return result
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun performCentralizedSync() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        _syncError.value = null

        // Reset/restart the 60-second automatic refresh timer to ensure manual clicks reset the countdown
        startAutoRefreshTimer()

        viewModelScope.launch {
            try {
                val isPlaceholderKey = SupabaseClient.supabaseAnonKey.contains("placeholder")
                if (isPlaceholderKey) {
                    // Running in Sandbox Mode: Bypass remote Supabase sync as keys are placeholder/unconfigured
                    val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale("ar", "EG"))
                    _lastSyncTimestamp.value = sdf.format(java.util.Date())
                    return@launch
                }

                // Fetch each repository's sync in parallel using async/await!
                val catDeferred = async { categoryRepository.syncCategories() }
                val prodDeferred = async { productRepository.syncProducts() }
                val orderDeferred = async { orderRepository.syncOrders() }
                val shipDeferred = async { shippingRepository.syncShipping() }
                val custDeferred = async { customerRepository.syncCustomers() }
                val offerDeferred = async { offerRepository.syncOffers() }

                val catRes = catDeferred.await()
                val prodRes = prodDeferred.await()
                val orderRes = orderDeferred.await()
                val shipRes = shipDeferred.await()
                val custRes = custDeferred.await()
                val offerRes = offerDeferred.await()

                // Sync remaining in-memory states (Coupons, Prime, Staff, Settings, Cust, Devices)
                try {
                    syncRemainingSupabaseData()
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseSync", "Error syncing remaining in-memory data: ${e.message}")
                }

                val errors = mutableListOf<String>()
                var hasNetworkError = false

                val results = listOf(
                    "الأقسام" to catRes,
                    "المنتجات" to prodRes,
                    "الطلبات" to orderRes,
                    "الشحن" to shipRes,
                    "العملاء" to custRes,
                    "العروض" to offerRes
                )

                for ((label, res) in results) {
                    if (res.isFailure) {
                        val exc = res.exceptionOrNull()
                        if (isNetworkException(exc)) {
                            hasNetworkError = true
                        } else {
                            errors.add("$label: ${exc?.message}")
                        }
                    }
                }

                if (hasNetworkError && errors.isEmpty()) {
                    _syncError.value = "التطبيق يعمل في وضع عدم الاتصال (Offline). سيتم التحديث تلقائياً عند استعادة الاتصال."
                } else if (errors.isNotEmpty()) {
                    _syncError.value = "حدث خطأ أثناء مزامنة بعض البيانات: \n" + errors.joinToString("\n")
                } else {
                    val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale("ar", "EG"))
                    _lastSyncTimestamp.value = sdf.format(java.util.Date())
                }
            } catch (e: Exception) {
                _syncError.value = "خطأ غير متوقع: ${e.message}"
                android.util.Log.e("SupabaseSync", "Unexpected sync exception", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun startAutoRefreshTimer() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(60000L) // 60 seconds
                try {
                    android.util.Log.d("SupabaseAuth", "Periodic check: verifying session expiry...")
                    authRepository.refreshAuthSessionIfNeeded()
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseAuth", "Background session refresh check failed: ${e.message}")
                }
                performCentralizedSync()
            }
        }
    }

    fun selectSection(section: DashboardSection) {
        _currentSection.value = section
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun setTimeframeDays(days: Int) {
        _selectedTimeframeDays.value = days
    }

    fun selectOrder(order: Order?) {
        _selectedOrder.value = order
    }

    fun selectOrderById(orderId: String) {
        viewModelScope.launch {
            val order = orderRepository.getOrderById(orderId)
            _selectedOrder.value = order
            _currentSection.value = DashboardSection.ORDERS
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus, note: String? = null) {
        viewModelScope.launch {
            orderRepository.updateOrderStatus(orderId, newStatus, note)
            if (_selectedOrder.value?.id == orderId) {
                _selectedOrder.value = orderRepository.getOrderById(orderId)
            }
            // Add notification
            addNotification(
                DashboardNotification(
                    id = "notif_${System.currentTimeMillis()}",
                    type = NotificationType.ORDER_STATUS,
                    title = "تحديث حالة الطلب $orderId",
                    message = "تم تغيير حالة الطلب إلى ${newStatus.arabicLabel}",
                    targetOrderId = orderId
                )
            )
        }
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            productRepository.saveProduct(product)
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            productRepository.deleteProduct(productId)
        }
    }

    fun toggleProductActive(productId: String, isActive: Boolean) {
        viewModelScope.launch {
            productRepository.toggleProductActive(productId, isActive)
        }
    }

    fun saveCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.saveCategory(category)
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(categoryId)
        }
    }

    fun saveOffer(offer: Offer) {
        viewModelScope.launch {
            offerRepository.saveOffer(offer)
        }
    }

    fun deleteOffer(offerId: String) {
        viewModelScope.launch {
            offerRepository.deleteOffer(offerId)
        }
    }

    fun saveGovernorate(gov: ShippingGovernorate) {
        viewModelScope.launch {
            shippingRepository.saveGovernorate(gov)
        }
    }

    fun saveShippingCenter(center: ShippingCenter) {
        viewModelScope.launch {
            shippingRepository.saveShippingCenter(center)
        }
    }

    fun deleteShippingCenter(centerId: String) {
        viewModelScope.launch {
            shippingRepository.deleteShippingCenter(centerId)
        }
    }

    // Coupon management
    fun saveCoupon(coupon: Coupon) {
        val current = _coupons.value.toMutableList()
        val idx = current.indexOfFirst { it.id == coupon.id }
        if (idx >= 0) {
            current[idx] = coupon
        } else {
            current.add(0, coupon)
        }
        _coupons.value = current

        viewModelScope.launch {
            try {
                val dto = SupabaseCouponDto(
                    id = coupon.id,
                    code = coupon.code,
                    discountType = coupon.discountType.name,
                    discountValue = coupon.discountValue,
                    minOrder = coupon.minOrderAmount,
                    maxDiscount = coupon.maxDiscountAmount,
                    freeShipping = coupon.isFreeShipping,
                    usageLimit = coupon.usageLimit,
                    usedCount = coupon.usageCount,
                    startsAt = coupon.startDate.toString(),
                    expiresAt = coupon.endDate.toString(),
                    active = coupon.isActive
                )
                SupabaseClient.service.upsertCoupon(dto)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteCoupon(couponId: String) {
        _coupons.value = _coupons.value.filter { it.id != couponId }
        viewModelScope.launch {
            try {
                SupabaseClient.service.deleteCoupon("eq.$couponId")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleCouponActive(couponId: String, isActive: Boolean) {
        _coupons.value = _coupons.value.map {
            if (it.id == couponId) it.copy(isActive = isActive) else it
        }
        viewModelScope.launch {
            try {
                SupabaseClient.service.patchCoupon("eq.$couponId", mapOf("active" to isActive))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Prime subscriber management
    fun savePrimeSubscriber(subscriber: PrimeSubscriber) {
        val current = _primeSubscribers.value.toMutableList()
        val idx = current.indexOfFirst { it.id == subscriber.id }
        if (idx >= 0) current[idx] = subscriber else current.add(0, subscriber)
        _primeSubscribers.value = current

        viewModelScope.launch {
            try {
                val dto = SupabasePrimeSubscriptionDto(
                    id = subscriber.id,
                    customerName = subscriber.customerName,
                    phone = subscriber.phone,
                    startDate = subscriber.startDate.toString(),
                    expiryDate = subscriber.expiryDate.toString(),
                    status = subscriber.status.name,
                    totalOrders = subscriber.totalOrdersWithPrime,
                    totalSaved = subscriber.totalSavedWithPrime
                )
                SupabaseClient.service.upsertPrimeSubscription(dto)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun togglePrimeStatus(id: String, isActive: Boolean) {
        _primeSubscribers.value = _primeSubscribers.value.map {
            if (it.id == id) it.copy(
                status = if (isActive) PrimeSubscriberStatus.ACTIVE else PrimeSubscriberStatus.EXPIRED
            ) else it
        }
        val updated = _primeSubscribers.value.find { it.id == id }
        if (updated != null) {
            savePrimeSubscriber(updated)
        }
    }

    // Staff member management
    fun saveStaffMember(staff: StaffMember) {
        val current = _staffMembers.value.toMutableList()
        val idx = current.indexOfFirst { it.id == staff.id }
        if (idx >= 0) current[idx] = staff else current.add(0, staff)
        _staffMembers.value = current

        viewModelScope.launch {
            try {
                val dto = SupabaseStaffProfileDto(
                    id = staff.id,
                    name = staff.name,
                    email = staff.email,
                    phone = staff.phone,
                    role = staff.role.name,
                    permissions = staff.permissions.associate { it.name to true },
                    active = staff.isActive,
                    isActive = staff.isActive,
                    lastActive = staff.lastActive.toString()
                )
                SupabaseClient.service.upsertStaffProfile(dto)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteStaffMember(staffId: String) {
        _staffMembers.value = _staffMembers.value.filter { it.id != staffId }
        viewModelScope.launch {
            try {
                SupabaseClient.service.deleteStaffProfile("eq.$staffId")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleStaffActive(staffId: String, isActive: Boolean) {
        _staffMembers.value = _staffMembers.value.map {
            if (it.id == staffId) it.copy(isActive = isActive) else it
        }
        viewModelScope.launch {
            try {
                SupabaseClient.service.patchStaffProfile("eq.$staffId", mapOf("active" to isActive, "is_active" to isActive))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Website visual config management
    fun updateWebsiteConfig(config: WebsiteConfig) {
        _websiteConfig.value = config

        viewModelScope.launch {
            try {
                // 1. Store settings
                val settingsDto = SupabaseStoreSettingsDto(
                    storeName = config.storeName,
                    whatsappNumber = config.whatsappNumber,
                    instagramHandle = config.instagramHandle,
                    facebookPage = config.facebookPage
                )
                SupabaseClient.service.upsertStoreSettings(settingsDto)

                // 2. Site customizations
                val sectionsStr = config.sections.joinToString(";") { "${it.id}:${it.title}:${it.isVisible}:${it.sortOrder}" }
                
                val siteConfigObj = SiteCustomizationConfig(
                    announcement = AnnouncementConfig(
                        message1 = config.announcementMsg1,
                        message2 = config.announcementMsg2,
                        message3 = config.announcementMsg3,
                        isVisible = config.showAnnouncementBar,
                        backgroundColor = config.announcementBgColor,
                        textColor = config.announcementTextColor
                    ),
                    header = HeaderConfig(
                        logoUrl = config.headerLogoUrl,
                        showMenuBtn = config.headerShowMenuBtn,
                        showAccountBtn = config.headerShowAccountBtn,
                        showCartBtn = config.headerShowCartBtn,
                        showFavBtn = config.headerShowFavBtn,
                        iconsColor = config.headerIconsColor,
                        backgroundColor = config.headerBgColor
                    ),
                    navigation = config.navigationItems,
                    categories = CategoriesConfig(
                        title = config.categoriesTitle,
                        subtitle = config.categoriesSubtitle,
                        isVisible = config.categoriesIsVisible
                    ),
                    featured = FeaturedConfig(
                        title = config.productsSectionTitle,
                        subtitle = config.productsSectionSubtitle,
                        smallTitle = config.productsSectionSmallTitle,
                        buttonText = config.productsSectionButtonText
                    ),
                    prime = PrimeConfig(
                        name = config.primeName,
                        title = config.primeTitle,
                        description = config.primeDescription,
                        price = config.primePrice,
                        durationDays = config.primeDurationDays,
                        discountPercent = config.primeDiscountPercent,
                        freeShippingOrdersCount = config.primeFreeShippingOrdersCount,
                        packagingFeature = config.primePackagingFeature,
                        themeFeature = config.primeThemeFeature,
                        buttonText = config.primeButtonText,
                        isVisible = config.primeIsVisible
                    ),
                    features = config.storeFeatures,
                    instagram = InstagramConfig(
                        images = config.instagramImages
                    ),
                    footer = FooterConfig(
                        aboutText = config.footerAboutText,
                        linksTitle = config.footerLinksTitle,
                        whatsapp = config.footerWhatsapp,
                        instagram = config.footerInstagram,
                        facebook = config.footerFacebook,
                        paymentIcons = config.footerPaymentIcons,
                        bottomText = config.footerBottomText,
                        credits = config.footerCredits
                    ),
                    global = GlobalConfig(
                        storeName = config.storeName,
                        metaTitle = config.storeName,
                        metaDescription = config.aboutText,
                        primaryColor = config.primaryColorHex,
                        backgroundColor = "#121212",
                        textColor = "#FFFFFF",
                        announcementColor = config.announcementBgColor
                    )
                )

                val customizationsDto = SupabaseSiteCustomizationsDto(
                    id = 1,
                    config = siteConfigObj
                )
                SupabaseClient.service.upsertSiteCustomizations(customizationsDto)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Notification management
    fun addNotification(notification: DashboardNotification) {
        _notifications.value = listOf(notification) + _notifications.value
    }

    fun markNotificationAsRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun markAllNotificationsAsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    // Payment methods management
    fun savePaymentMethod(method: PaymentMethodConfig) {
        val current = _paymentMethods.value.toMutableList()
        val idx = current.indexOfFirst { it.key == method.key }
        if (idx >= 0) current[idx] = method else current.add(method)
        _paymentMethods.value = current
    }

    private fun syncRemainingSupabaseData() {
        viewModelScope.launch {
            // Register device
            try {
                val deviceDto = SupabaseDashboardPushDeviceDto(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = SupabaseClient.userId,
                    pushToken = "dummy_token_android_" + System.currentTimeMillis(),
                    platform = "android",
                    active = true,
                    createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()),
                    updatedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()),
                    lastSeenAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
                )
                SupabaseClient.service.registerPushDevice(deviceDto)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Fetch Coupons
            try {
                val response = SupabaseClient.service.getCoupons()
                if (response.isSuccessful) {
                    val dtoList = response.body() ?: emptyList()
                    val domainList = dtoList.map { dto ->
                        Coupon(
                            id = dto.id,
                            code = dto.code,
                            discountType = try { DiscountType.valueOf(dto.discountType) } catch (e: Exception) { DiscountType.PERCENTAGE },
                            discountValue = dto.discountValue,
                            minOrderAmount = dto.minOrder ?: 0.0,
                            maxDiscountAmount = dto.maxDiscount,
                            isFreeShipping = dto.freeShipping ?: false,
                            usageLimit = dto.usageLimit,
                            usageCount = dto.usedCount ?: 0,
                            isFirstOrderOnly = false,
                            startDate = dto.startsAt?.toLongOrNull() ?: System.currentTimeMillis(),
                            endDate = dto.expiresAt?.toLongOrNull() ?: (System.currentTimeMillis() + 30L*24*60*60*1000),
                            isActive = dto.active ?: true
                        )
                    }
                    _coupons.value = domainList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Fetch Prime Subscriptions
            try {
                val response = SupabaseClient.service.getPrimeSubscriptions()
                if (response.isSuccessful) {
                    val dtoList = response.body() ?: emptyList()
                    val domainList = dtoList.map { dto ->
                        PrimeSubscriber(
                            id = dto.id,
                            customerName = dto.customerName,
                            phone = dto.phone,
                            startDate = dto.startDate.toLongOrNull() ?: System.currentTimeMillis(),
                            expiryDate = dto.expiryDate.toLongOrNull() ?: System.currentTimeMillis(),
                            status = try { PrimeSubscriberStatus.valueOf(dto.status) } catch (e: Exception) { PrimeSubscriberStatus.ACTIVE },
                            totalOrdersWithPrime = dto.totalOrders ?: 0,
                            totalSavedWithPrime = dto.totalSaved ?: 0.0
                        )
                    }
                    _primeSubscribers.value = domainList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Fetch Staff Profiles
            try {
                val response = SupabaseClient.service.getStaffProfiles()
                if (response.isSuccessful) {
                    val dtoList = response.body() ?: emptyList()
                    val domainList = dtoList.map { dto ->
                        StaffMember(
                            id = dto.id,
                            name = dto.name ?: dto.email?.substringBefore("@") ?: "Staff Member",
                            email = dto.email ?: "",
                            phone = dto.phone ?: "",
                            role = try { UserRole.valueOf(dto.role ?: "EMPLOYEE") } catch (e: Exception) { UserRole.EMPLOYEE },
                            permissions = dto.permissions?.filterValues { it }?.keys?.mapNotNull {
                                try { Permission.valueOf(it) } catch (e: Exception) { null }
                            }?.toSet() ?: emptySet(),
                            isActive = dto.active ?: dto.isActive ?: true,
                            lastActive = dto.lastActive?.toLongOrNull() ?: System.currentTimeMillis()
                        )
                    }
                    _staffMembers.value = domainList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Fetch Store Settings and Site Customizations
            try {
                val settingsResponse = SupabaseClient.service.getStoreSettings()
                val customizationsResponse = SupabaseClient.service.getSiteCustomizations()

                var currentConfig = _websiteConfig.value

                if (settingsResponse.isSuccessful) {
                    val settings = settingsResponse.body()?.firstOrNull()
                    if (settings != null) {
                        currentConfig = currentConfig.copy(
                            storeName = settings.storeName,
                            whatsappNumber = settings.whatsappNumber,
                            instagramHandle = settings.instagramHandle,
                            facebookPage = settings.facebookPage
                        )
                    }
                }

                if (customizationsResponse.isSuccessful) {
                    val customizations = customizationsResponse.body()?.firstOrNull()
                    if (customizations != null) {
                        val sectionsJsonLocal = customizations.sectionsJson
                        val parsedSections = if (!sectionsJsonLocal.isNullOrBlank()) {
                            sectionsJsonLocal.split(";").filter { it.isNotBlank() }.mapNotNull { sec ->
                                val parts = sec.split(":")
                                if (parts.size >= 4) {
                                    SectionDisplayConfig(
                                        id = parts[0],
                                        title = parts[1],
                                        isVisible = parts[2].toBooleanStrictOrNull() ?: true,
                                        sortOrder = parts[3].toIntOrNull() ?: 0
                                    )
                                } else null
                            }
                        } else null

                        val cfgObj = customizations.config

                        currentConfig = currentConfig.copy(
                            announcementBarText = customizations.announcementBarText,
                            showAnnouncementBar = customizations.showAnnouncementBar,
                            heroTitle = customizations.heroTitle,
                            heroSubtitle = customizations.heroSubtitle,
                            heroButtonText = customizations.heroButtonText,
                            heroImageUrl = customizations.heroImageUrl,
                            showHeroBanner = customizations.showHeroBanner,
                            showAboutSection = customizations.showAboutSection,
                            aboutText = customizations.aboutText,
                            primaryColorHex = customizations.primaryColorHex,
                            fontFamily = customizations.fontFamily,
                            sections = parsedSections ?: currentConfig.sections,

                            // Top Bar Announcement Multi-Messages
                            announcementMsg1 = cfgObj?.announcement?.message1 ?: currentConfig.announcementMsg1,
                            announcementMsg2 = cfgObj?.announcement?.message2 ?: currentConfig.announcementMsg2,
                            announcementMsg3 = cfgObj?.announcement?.message3 ?: currentConfig.announcementMsg3,
                            announcementBgColor = cfgObj?.announcement?.backgroundColor ?: currentConfig.announcementBgColor,
                            announcementTextColor = cfgObj?.announcement?.textColor ?: currentConfig.announcementTextColor,

                            // Header Settings
                            headerLogoUrl = cfgObj?.header?.logoUrl ?: currentConfig.headerLogoUrl,
                            headerShowMenuBtn = cfgObj?.header?.showMenuBtn ?: currentConfig.headerShowMenuBtn,
                            headerShowAccountBtn = cfgObj?.header?.showAccountBtn ?: currentConfig.headerShowAccountBtn,
                            headerShowCartBtn = cfgObj?.header?.showCartBtn ?: currentConfig.headerShowCartBtn,
                            headerShowFavBtn = cfgObj?.header?.showFavBtn ?: currentConfig.headerShowFavBtn,
                            headerIconsColor = cfgObj?.header?.iconsColor ?: currentConfig.headerIconsColor,
                            headerBgColor = cfgObj?.header?.backgroundColor ?: currentConfig.headerBgColor,

                            // Navigation
                            navigationItems = cfgObj?.navigation ?: currentConfig.navigationItems,

                            // Categories Customization
                            categoriesTitle = cfgObj?.categories?.title ?: currentConfig.categoriesTitle,
                            categoriesSubtitle = cfgObj?.categories?.subtitle ?: currentConfig.categoriesSubtitle,
                            categoriesIsVisible = cfgObj?.categories?.isVisible ?: currentConfig.categoriesIsVisible,

                            // Products Customization
                            productsSectionTitle = cfgObj?.featured?.title ?: currentConfig.productsSectionTitle,
                            productsSectionSubtitle = cfgObj?.featured?.subtitle ?: currentConfig.productsSectionSubtitle,
                            productsSectionSmallTitle = cfgObj?.featured?.smallTitle ?: currentConfig.productsSectionSmallTitle,
                            productsSectionButtonText = cfgObj?.featured?.buttonText ?: currentConfig.productsSectionButtonText,

                            // Prime Customizations
                            primeName = cfgObj?.prime?.name ?: currentConfig.primeName,
                            primeTitle = cfgObj?.prime?.title ?: currentConfig.primeTitle,
                            primeDescription = cfgObj?.prime?.description ?: currentConfig.primeDescription,
                            primePrice = cfgObj?.prime?.price ?: currentConfig.primePrice,
                            primeDurationDays = cfgObj?.prime?.durationDays ?: currentConfig.primeDurationDays,
                            primeDiscountPercent = cfgObj?.prime?.discountPercent ?: currentConfig.primeDiscountPercent,
                            primeFreeShippingOrdersCount = cfgObj?.prime?.freeShippingOrdersCount ?: currentConfig.primeFreeShippingOrdersCount,
                            primePackagingFeature = cfgObj?.prime?.packagingFeature ?: currentConfig.primePackagingFeature,
                            primeThemeFeature = cfgObj?.prime?.themeFeature ?: currentConfig.primeThemeFeature,
                            primeButtonText = cfgObj?.prime?.buttonText ?: currentConfig.primeButtonText,
                            primeIsVisible = cfgObj?.prime?.isVisible ?: currentConfig.primeIsVisible,

                            // Features
                            storeFeatures = cfgObj?.features ?: currentConfig.storeFeatures,

                            // Instagram
                            instagramImages = cfgObj?.instagram?.images ?: currentConfig.instagramImages,

                            // Footer
                            footerAboutText = cfgObj?.footer?.aboutText ?: currentConfig.footerAboutText,
                            footerLinksTitle = cfgObj?.footer?.linksTitle ?: currentConfig.footerLinksTitle,
                            footerWhatsapp = cfgObj?.footer?.whatsapp ?: currentConfig.footerWhatsapp,
                            footerInstagram = cfgObj?.footer?.instagram ?: currentConfig.footerInstagram,
                            footerFacebook = cfgObj?.footer?.facebook ?: currentConfig.footerFacebook,
                            footerPaymentIcons = cfgObj?.footer?.paymentIcons ?: currentConfig.footerPaymentIcons,
                            footerBottomText = cfgObj?.footer?.bottomText ?: currentConfig.footerBottomText,
                            footerCredits = cfgObj?.footer?.credits ?: currentConfig.footerCredits
                        )
                    }
                }

                _websiteConfig.value = currentConfig
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun togglePaymentMethodActive(key: String, isEnabled: Boolean) {
        _paymentMethods.value = _paymentMethods.value.map {
            if (it.key == key) it.copy(isEnabled = isEnabled) else it
        }
    }

    fun switchRole(role: UserRole) {
        viewModelScope.launch {
            authRepository.switchRoleForTesting(role)
        }
    }

    fun updateSyncInterval(interval: SyncInterval) {
        syncManager.updateInterval(interval)
    }

    fun manualSync() {
        viewModelScope.launch {
            syncManager.performSync()
        }
    }

    fun triggerTestNotification() {
        syncManager.triggerTestNotification()
        addNotification(
            DashboardNotification(
                id = "notif_${System.currentTimeMillis()}",
                type = NotificationType.NEW_ORDER,
                title = "أوردر جديد ورد الآن!",
                message = "العميلة سارة مجدي أتمت طلباً بقيمة 1,250 ج.م"
            )
        )
    }

    fun seedInitialStoreData() {
        // No hardcoded categories are seeded anymore. Only real categories fetched from Supabase.
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
        syncManager.destroy()
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = context.applicationContext as? PinkyApplication
                return if (app != null) {
                    PinkyViewModel(
                        orderRepository = app.orderRepository,
                        productRepository = app.productRepository,
                        categoryRepository = app.categoryRepository,
                        offerRepository = app.offerRepository,
                        customerRepository = app.customerRepository,
                        shippingRepository = app.shippingRepository,
                        analyticsRepository = app.analyticsRepository,
                        authRepository = app.authRepository,
                        syncManager = app.syncManager
                    ) as T
                } else {
                    val database = PinkyDatabase.getInstance(context.applicationContext)
                    val orderRepo = OrderRepositoryImpl(database)
                    val prodRepo = ProductRepositoryImpl(database)
                    val authRepo = AuthRepositoryImpl(context.applicationContext)
                    PinkyViewModel(
                        orderRepository = orderRepo,
                        productRepository = prodRepo,
                        categoryRepository = CategoryRepositoryImpl(database),
                        offerRepository = OfferRepositoryImpl(database),
                        customerRepository = CustomerRepositoryImpl(database),
                        shippingRepository = ShippingRepositoryImpl(database),
                        analyticsRepository = AnalyticsRepositoryImpl(orderRepo, prodRepo, authRepo),
                        authRepository = authRepo,
                        syncManager = OrderSyncManager(context.applicationContext, orderRepo)
                    ) as T
                }
            }
        }
    }
}
