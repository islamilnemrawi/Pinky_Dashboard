package com.pinky.dashboard.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    fun formatCurrencyEgp(amount: Double): String {
        return String.format(Locale.getDefault(), "%,.1f", amount) + " ج.م"
    }

    fun formatOrderDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar"))
        return sdf.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))
        return sdf.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM", Locale("ar"))
        return sdf.format(Date(timestamp))
    }

    fun formatRelativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / (60 * 1000)
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> "الآن"
            minutes < 60 -> "منذ $minutes دقيقة"
            hours < 24 -> "منذ $hours ساعة"
            days == 1L -> "أمس"
            days < 30 -> "منذ $days يوم"
            else -> formatShortDate(timestamp)
        }
    }
}
