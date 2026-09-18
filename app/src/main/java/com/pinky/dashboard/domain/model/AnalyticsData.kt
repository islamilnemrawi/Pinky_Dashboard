package com.pinky.dashboard.domain.model

data class DailyMetric(
    val dayLabel: String,
    val salesAmount: Double,
    val ordersCount: Int,
    val profitAmount: Double = 0.0
)

data class TopProductMetric(
    val productId: String,
    val productName: String,
    val unitsSold: Int,
    val totalRevenue: Double,
    val imageUrl: String? = null
)

data class AnalyticsData(
    val totalSales: Double = 0.0,
    val totalOrders: Int = 0,
    val totalProductsSold: Int = 0,
    val totalProfit: Double? = null,
    val averageOrderValue: Double = if (totalOrders > 0) totalSales / totalOrders else 0.0,
    val dailyMetrics: List<DailyMetric> = emptyList(),
    val topSellingProducts: List<TopProductMetric> = emptyList(),
    val ordersByStatus: Map<OrderStatus, Int> = emptyMap(),
    val salesByGovernorate: Map<String, Double> = emptyMap()
)
