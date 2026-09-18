package com.pinky.dashboard.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pinky.dashboard.MainActivity
import com.pinky.dashboard.core.util.Formatters
import com.pinky.dashboard.domain.model.Order

class PinkyNotificationManager(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "pinky_orders_notifications"
        const val CHANNEL_NAME = "طلبات متجر بينكي"
        const val EXTRA_ORDER_ID = "extra_order_id"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات الطلبات الجديدة في لوحة تحكم بينكي"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNewOrderNotification(order: Order) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ORDER_ID, order.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            order.orderNumber.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val itemsSummary = order.items.joinToString(", ") { "${it.productName} (${it.quantity})" }
        val bodyText = "طلب جديد #${order.orderNumber} من ${order.customerName} - ${Formatters.formatCurrencyEgp(order.totalAmount)}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🛍️ طلب جديد في متجر بينكي!")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$bodyText\nالمنتجات: $itemsSummary\nالمحافظة: ${order.governorate}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(order.orderNumber.hashCode(), notification)
    }
}
