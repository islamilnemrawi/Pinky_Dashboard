package com.pinky.dashboard.core.sync

import android.content.Context
import com.pinky.dashboard.core.notification.PinkyNotificationManager
import com.pinky.dashboard.domain.model.OrderStatus
import com.pinky.dashboard.domain.repository.OrderRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

enum class SyncInterval(val minutes: Int, val arabicLabel: String) {
    ONE_MINUTE(1, "كل دقيقة (مباشر)"),
    FIVE_MINUTES(5, "كل 5 دقائق"),
    FIFTEEN_MINUTES(15, "كل 15 دقيقة"),
    MANUAL(0, "يدوي فقط")
}

class OrderSyncManager(
    private val context: Context,
    private val orderRepository: OrderRepository
) {
    private val notificationManager = PinkyNotificationManager(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTime = _lastSyncTime.asStateFlow()

    private val _syncInterval = MutableStateFlow(SyncInterval.FIVE_MINUTES)
    val syncInterval = _syncInterval.asStateFlow()

    fun updateInterval(interval: SyncInterval) {
        _syncInterval.value = interval
        startPeriodicSync()
    }

    fun startPeriodicSync() {
        syncJob?.cancel()
        val interval = _syncInterval.value
        if (interval == SyncInterval.MANUAL) return

        syncJob = scope.launch {
            while (isActive) {
                delay(interval.minutes * 60 * 1000L)
                performSync()
            }
        }
    }

    suspend fun performSync(): Result<Unit> {
        _isSyncing.value = true
        return try {
            val result = orderRepository.syncOrders()
            _lastSyncTime.value = System.currentTimeMillis()
            result
        } finally {
            _isSyncing.value = false
        }
    }

    fun triggerTestNotification() {
        scope.launch {
            val orders = orderRepository.getOrdersFlow().first()
            val sampleOrder = orders.firstOrNull { it.status == OrderStatus.NEW } ?: orders.firstOrNull()
            if (sampleOrder != null) {
                notificationManager.showNewOrderNotification(sampleOrder)
            }
        }
    }

    fun destroy() {
        syncJob?.cancel()
    }
}
