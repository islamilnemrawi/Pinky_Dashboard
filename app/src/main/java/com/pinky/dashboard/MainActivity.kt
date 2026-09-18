package com.pinky.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pinky.dashboard.core.notification.PinkyNotificationManager
import com.pinky.dashboard.ui.PinkyApp
import com.pinky.dashboard.ui.PinkyViewModel

class MainActivity : ComponentActivity() {
    private var openedOrderId by mutableStateOf<String?>(null)
    private val viewModel: PinkyViewModel by viewModels { PinkyViewModel.provideFactory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        openedOrderId = intent?.getStringExtra(PinkyNotificationManager.EXTRA_ORDER_ID)

        setContent {
            PinkyApp(
                viewModel = viewModel,
                initialOrderId = openedOrderId
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val orderId = intent.getStringExtra(PinkyNotificationManager.EXTRA_ORDER_ID)
        if (orderId != null) {
            openedOrderId = orderId
        }
    }
}
