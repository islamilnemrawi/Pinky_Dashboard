package com.pinky.dashboard.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pinky.dashboard.domain.model.OrderStatus
import com.pinky.dashboard.ui.components.*
import com.pinky.dashboard.ui.navigation.DashboardSection
import com.pinky.dashboard.ui.screens.analytics.AnalyticsScreen
import com.pinky.dashboard.ui.screens.categories.CategoriesScreen
import com.pinky.dashboard.ui.screens.customers.CustomersScreen
import com.pinky.dashboard.ui.screens.dashboard.DashboardScreen
import com.pinky.dashboard.ui.screens.notifications.NotificationsScreen
import com.pinky.dashboard.ui.screens.offers.OffersScreen
import com.pinky.dashboard.ui.screens.orders.OrderDetailDialog
import com.pinky.dashboard.ui.screens.orders.OrdersScreen
import com.pinky.dashboard.ui.screens.payments.PaymentsScreen
import com.pinky.dashboard.ui.screens.prime.PrimeScreen
import com.pinky.dashboard.ui.screens.products.ProductsScreen
import com.pinky.dashboard.ui.screens.reports.ReportsScreen
import com.pinky.dashboard.ui.screens.settings.SettingsScreen
import com.pinky.dashboard.ui.screens.shipping.ShippingScreen
import com.pinky.dashboard.ui.screens.staff.StaffScreen
import com.pinky.dashboard.ui.screens.website.WebsiteEditorScreen
import com.pinky.dashboard.ui.screens.health.HealthCheckScreen
import com.pinky.dashboard.ui.theme.PinkyTheme
import kotlinx.coroutines.launch

@Composable
fun PinkyApp(
    viewModel: PinkyViewModel = viewModel(
        factory = PinkyViewModel.provideFactory(LocalContext.current)
    ),
    initialOrderId: String? = null
) {
    val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val offers by viewModel.offers.collectAsStateWithLifecycle()
    val coupons by viewModel.coupons.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val primeSubscribers by viewModel.primeSubscribers.collectAsStateWithLifecycle()
    val staffMembers by viewModel.staffMembers.collectAsStateWithLifecycle()
    val websiteConfig by viewModel.websiteConfig.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val paymentMethods by viewModel.paymentMethods.collectAsStateWithLifecycle()
    val governorates by viewModel.governorates.collectAsStateWithLifecycle()
    val shippingCenters by viewModel.shippingCenters.collectAsStateWithLifecycle()
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()
    val syncError by viewModel.syncError.collectAsStateWithLifecycle()
    val syncInterval by viewModel.syncInterval.collectAsStateWithLifecycle()
    val selectedTimeframeDays by viewModel.selectedTimeframeDays.collectAsStateWithLifecycle()
    val selectedOrder by viewModel.selectedOrder.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSearchDialog by remember { mutableStateOf(false) }
    var showQuickActionsDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }

    val unreadNotificationsCount = remember(notifications) {
        notifications.count { !it.isRead }
    }

    // Handle initial order notification intent
    LaunchedEffect(initialOrderId) {
        if (initialOrderId != null) {
            viewModel.selectOrderById(initialOrderId)
        }
    }

    // Display non-destructive sync errors
    LaunchedEffect(syncError) {
        val err = syncError
        if (!err.isNullOrBlank()) {
            snackbarHostState.showSnackbar(
                message = err,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Permission launcher for Android 13+ Push Notifications
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { _ -> }

        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Wrap in Theme & force RTL for Egyptian Arabic experience
    PinkyTheme(darkTheme = isDarkTheme) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            if (currentUser == null) {
                PinkyLoginScreen(
                    onLoginClick = { email, password, onError ->
                        coroutineScope.launch {
                            val res = viewModel.login(email, password)
                            if (res.isFailure) {
                                onError(res.exceptionOrNull()?.message ?: "اسم المستخدم أو كلمة المرور غير صحيحة")
                            } else {
                                viewModel.performCentralizedSync()
                            }
                        }
                    }
                )
            } else {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        PinkyNavigationDrawerContent(
                            currentSection = currentSection,
                            currentUser = currentUser,
                            unreadNotificationsCount = unreadNotificationsCount,
                            onSelectSection = { section ->
                                viewModel.selectSection(section)
                                coroutineScope.launch { drawerState.close() }
                            },
                            onOpenAiChat = {
                                coroutineScope.launch { drawerState.close() }
                                showAiDialog = true
                            }
                        )
                    }
                ) {
                    Scaffold(
                    topBar = {
                        PinkyTopBar(
                            currentSection = currentSection,
                            currentUser = currentUser,
                            unreadNotificationsCount = unreadNotificationsCount,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { viewModel.toggleTheme() },
                            onSwitchRole = { viewModel.switchRole(it) },
                            onMenuClick = {
                                coroutineScope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            },
                            onSearchClick = { showSearchDialog = true },
                            onQuickActionClick = { showQuickActionsDialog = true },
                            onAiClick = { showAiDialog = true },
                            onNotificationsClick = { viewModel.selectSection(DashboardSection.NOTIFICATIONS) },
                            isRefreshing = isRefreshing,
                            lastSyncTimestamp = lastSyncTimestamp,
                            onRefreshClick = { viewModel.performCentralizedSync() }
                        )
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        PinkyBottomNav(
                            currentSection = currentSection,
                            onSelectSection = { viewModel.selectSection(it) },
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                            currentUser = currentUser
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("pinky_app_scaffold")
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        when (currentSection) {
                            DashboardSection.HOME -> {
                                DashboardScreen(
                                    orders = orders,
                                    products = products,
                                    customers = customers,
                                    analytics = analytics,
                                    currentUser = currentUser,
                                    onNavigateToSection = { viewModel.selectSection(it) },
                                    onSelectOrder = { viewModel.selectOrder(it) },
                                    onQuickAddSampleData = { viewModel.seedInitialStoreData() }
                                )
                            }

                            DashboardSection.ORDERS -> {
                                OrdersScreen(
                                    orders = orders,
                                    onSelectOrder = { viewModel.selectOrder(it) },
                                    onStatusChange = { id, status -> viewModel.updateOrderStatus(id, status) }
                                )
                            }

                            DashboardSection.PRODUCTS -> {
                                ProductsScreen(
                                    products = products,
                                    categories = categories,
                                    currentUser = currentUser,
                                    onSaveProduct = { viewModel.saveProduct(it) },
                                    onSaveCategory = { viewModel.saveCategory(it) },
                                    onDeleteProduct = { viewModel.deleteProduct(it) },
                                    onToggleActive = { id, active -> viewModel.toggleProductActive(id, active) }
                                )
                            }

                            DashboardSection.CATEGORIES -> {
                                CategoriesScreen(
                                    categories = categories,
                                    onSaveCategory = { viewModel.saveCategory(it) },
                                    onDeleteCategory = { viewModel.deleteCategory(it) }
                                )
                            }

                            DashboardSection.OFFERS_COUPONS -> {
                                OffersScreen(
                                    offers = offers,
                                    coupons = coupons,
                                    categories = categories,
                                    onSaveOffer = { viewModel.saveOffer(it) },
                                    onDeleteOffer = { viewModel.deleteOffer(it) },
                                    onSaveCoupon = { viewModel.saveCoupon(it) },
                                    onDeleteCoupon = { viewModel.deleteCoupon(it) },
                                    onToggleCouponActive = { id, active -> viewModel.toggleCouponActive(id, active) }
                                )
                            }

                            DashboardSection.CUSTOMERS -> {
                                CustomersScreen(
                                    customers = customers,
                                    orders = orders
                                )
                            }

                            DashboardSection.PRIME -> {
                                PrimeScreen(
                                    subscribers = primeSubscribers,
                                    onSaveSubscriber = { viewModel.savePrimeSubscriber(it) },
                                    onToggleStatus = { id, active -> viewModel.togglePrimeStatus(id, active) }
                                )
                            }

                            DashboardSection.SHIPPING -> {
                                ShippingScreen(
                                    governorates = governorates,
                                    shippingCenters = shippingCenters,
                                    onSaveGovernorate = { viewModel.saveGovernorate(it) },
                                    onSaveCenter = { viewModel.saveShippingCenter(it) },
                                    onDeleteCenter = { viewModel.deleteShippingCenter(it) }
                                )
                            }

                            DashboardSection.PAYMENTS -> {
                                PaymentsScreen(
                                    paymentMethods = paymentMethods,
                                    onSavePaymentMethod = { viewModel.savePaymentMethod(it) },
                                    onToggleActive = { method, active -> viewModel.togglePaymentMethodActive(method, active) }
                                )
                            }

                            DashboardSection.STAFF -> {
                                StaffScreen(
                                    staffMembers = staffMembers,
                                    onSaveStaffMember = { viewModel.saveStaffMember(it) },
                                    onDeleteStaffMember = { viewModel.deleteStaffMember(it) },
                                    onToggleActive = { id, active -> viewModel.toggleStaffActive(id, active) }
                                )
                            }

                            DashboardSection.WEBSITE_EDITOR -> {
                                WebsiteEditorScreen(
                                    initialConfig = websiteConfig,
                                    categories = categories,
                                    banners = offers.filter { it.isBanner || it.id.startsWith("ban_") },
                                    onSaveConfig = { viewModel.updateWebsiteConfig(it) },
                                    onSaveBanner = { viewModel.saveOffer(it) },
                                    onDeleteBanner = { viewModel.deleteOffer(it.id) },
                                    onSaveCategory = { viewModel.saveCategory(it) }
                                )
                            }

                            DashboardSection.REPORTS -> {
                                ReportsScreen(
                                    orders = orders,
                                    products = products,
                                    customers = customers,
                                    analytics = analytics,
                                    currentUser = currentUser
                                )
                            }

                            DashboardSection.NOTIFICATIONS -> {
                                NotificationsScreen(
                                    notifications = notifications,
                                    onMarkAsRead = { viewModel.markNotificationAsRead(it) },
                                    onMarkAllAsRead = { viewModel.markAllNotificationsAsRead() },
                                    onClearAll = { viewModel.clearNotifications() },
                                    onNavigateToRelated = { notif ->
                                        when (notif.type) {
                                            com.pinky.dashboard.domain.model.NotificationType.NEW_ORDER,
                                            com.pinky.dashboard.domain.model.NotificationType.ORDER_STATUS -> {
                                                notif.targetOrderId?.let { viewModel.selectOrderById(it) }
                                                    ?: viewModel.selectSection(DashboardSection.ORDERS)
                                            }
                                            com.pinky.dashboard.domain.model.NotificationType.LOW_STOCK -> {
                                                viewModel.selectSection(DashboardSection.PRODUCTS)
                                            }
                                            com.pinky.dashboard.domain.model.NotificationType.NEW_CUSTOMER -> {
                                                viewModel.selectSection(DashboardSection.CUSTOMERS)
                                            }
                                            com.pinky.dashboard.domain.model.NotificationType.COUPON_EXPIRY -> {
                                                viewModel.selectSection(DashboardSection.OFFERS_COUPONS)
                                            }
                                            com.pinky.dashboard.domain.model.NotificationType.SYSTEM_ALERT -> {
                                                viewModel.selectSection(DashboardSection.SETTINGS)
                                            }
                                        }
                                    }
                                )
                            }

                            DashboardSection.HEALTH_CHECK -> {
                                HealthCheckScreen(
                                    currentUser = currentUser
                                )
                            }

                            DashboardSection.SETTINGS -> {
                                SettingsScreen(
                                    currentUser = currentUser,
                                    isDarkTheme = isDarkTheme,
                                    syncInterval = syncInterval,
                                    isSyncing = isSyncing,
                                    onToggleTheme = { viewModel.toggleTheme() },
                                    onUpdateSyncInterval = { viewModel.updateSyncInterval(it) },
                                    onManualSync = { viewModel.manualSync() },
                                    onTriggerTestNotification = { viewModel.triggerTestNotification() },
                                    onSwitchRole = { viewModel.switchRole(it) },
                                    onSeedData = { viewModel.seedInitialStoreData() }
                                )
                            }
                        }
                    }
                }
            }

            // Global Search Dialog
            if (showSearchDialog) {
                GlobalSearchDialog(
                    orders = orders,
                    products = products,
                    customers = customers,
                    categories = categories,
                    coupons = coupons,
                    onDismiss = { showSearchDialog = false },
                    onSelectOrder = { order ->
                        showSearchDialog = false
                        viewModel.selectOrder(order)
                    },
                    onNavigateToSection = { section ->
                        showSearchDialog = false
                        viewModel.selectSection(section)
                    }
                )
            }

            // Quick Actions Dialog
            if (showQuickActionsDialog) {
                QuickActionsDialog(
                    onDismiss = { showQuickActionsDialog = false },
                    onSelectAction = { section ->
                        showQuickActionsDialog = false
                        viewModel.selectSection(section)
                    }
                )
            }

            // Pinky AI Assistant Dialog
            if (showAiDialog) {
                PinkyAiDialog(
                    orders = orders,
                    products = products,
                    customers = customers,
                    analytics = analytics,
                    currentUser = currentUser,
                    onDismiss = { showAiDialog = false }
                )
            }

            // Order detail dialog overlay
            selectedOrder?.let { order ->
                OrderDetailDialog(
                    order = order,
                    onDismiss = { viewModel.selectOrder(null) },
                    onStatusChange = { newStatus ->
                        viewModel.updateOrderStatus(order.id, newStatus)
                    }
                )
            }
            } // Closes else block for currentUser == null
        }
    }
}

@Composable
fun PinkyLoginScreen(
    onLoginClick: (String, String, (String) -> Unit) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Pinky Logo badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = com.pinky.dashboard.ui.theme.PinkPrimary,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = "P",
                        color = androidx.compose.ui.graphics.Color(0xFF381528),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                        fontSize = 36.sp
                    )
                }

                Text(
                    text = "تسجيل الدخول للوحة التحكم",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Text(
                    text = "الرجاء إدخال بيانات حساب الموظف أو المالك لمتابعة المتجر",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        errorMessage = null
                    },
                    label = { Text("البريد الإلكتروني") },
                    leadingIcon = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Email,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("login_email_input"),
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        errorMessage = null
                    },
                    label = { Text("كلمة المرور") },
                    leadingIcon = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        val image = if (passwordVisible)
                            androidx.compose.material.icons.Icons.Default.Visibility
                        else androidx.compose.material.icons.Icons.Default.VisibilityOff

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    },
                    visualTransformation = if (passwordVisible) 
                        androidx.compose.ui.text.input.VisualTransformation.None 
                    else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
                    enabled = !isLoading
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "الرجاء ملء جميع الحقول المطلوبة"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        onLoginClick(email, password) { error ->
                            isLoading = false
                            errorMessage = error
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.pinky.dashboard.ui.theme.PinkPrimary,
                        contentColor = androidx.compose.ui.graphics.Color(0xFF381528)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("login_submit_btn")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = androidx.compose.ui.graphics.Color(0xFF381528),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "تسجيل الدخول المباشر",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
