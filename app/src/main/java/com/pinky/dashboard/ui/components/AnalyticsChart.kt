package com.pinky.dashboard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinky.dashboard.core.util.Formatters
import com.pinky.dashboard.domain.model.DailyMetric
import com.pinky.dashboard.ui.theme.PinkPrimary
import com.pinky.dashboard.ui.theme.PinkPrimaryDark

@Composable
fun SalesBarChartCard(
    metrics: List<DailyMetric>,
    title: String = "حركة المبيعات والطلبات",
    subtitle: String = "إحصائيات الأيام السابقة محسوبة من الطلبات الفعلية",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .background(PinkPrimary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ج.م EGP",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PinkPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (metrics.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد مبيعات مسجلة في هذه الفترة حتى الآن",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxSale = (metrics.maxOfOrNull { it.salesAmount } ?: 1.0).coerceAtLeast(100.0)

                Column(modifier = Modifier.fillMaxWidth()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val barCount = metrics.size
                        val barSpacing = canvasWidth / (barCount * 1.6f)
                        val barWidth = (canvasWidth - (barSpacing * (barCount + 1))) / barCount

                        // Draw baseline
                        drawLine(
                            color = Color(0xFF2E2E38),
                            start = Offset(0f, canvasHeight - 2f),
                            end = Offset(canvasWidth, canvasHeight - 2f),
                            strokeWidth = 1.5f,
                            cap = StrokeCap.Round
                        )

                        // Draw bars
                        metrics.forEachIndexed { index, metric ->
                            val left = barSpacing + index * (barWidth + barSpacing)
                            val normalizedHeight = (metric.salesAmount / maxSale).toFloat() * (canvasHeight - 20f)
                            val barHeight = normalizedHeight.coerceAtLeast(6f)
                            val top = canvasHeight - barHeight

                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(PinkPrimary, PinkPrimaryDark)
                                ),
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Labels Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        metrics.forEach { metric ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = metric.dayLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${metric.ordersCount} طلب",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = PinkPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
