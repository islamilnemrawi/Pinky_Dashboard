package com.pinky.dashboard.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinky.dashboard.core.util.Formatters
import com.pinky.dashboard.domain.model.DashboardNotification
import com.pinky.dashboard.domain.model.NotificationType
import com.pinky.dashboard.ui.components.EmptyStateCard
import com.pinky.dashboard.ui.theme.PinkPrimary
import com.pinky.dashboard.ui.theme.StatusCancelled

@Composable
fun NotificationsScreen(
    notifications: List<DashboardNotification>,
    onMarkAsRead: (String) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onClearAll: () -> Unit,
    onNavigateToRelated: (DashboardNotification) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("notifications_screen")
    ) {
        // Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Row(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(PinkPrimary)
                        )
                        Text(
                            text = "مركز الإشعارات والتنبيهات",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "متابعة فورية للطلبات الجديدة، تنبيهات نفاد المخزون، وتحديثات حالة الشحن",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (notifications.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = onMarkAllAsRead) {
                            Text("قراءة الكل", color = PinkPrimary, fontSize = 12.sp)
                        }
                        IconButton(onClick = onClearAll) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = "مسح الإشعارات", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (notifications.isEmpty()) {
            EmptyStateCard(
                title = "صندوق الإشعارات فارغ",
                message = "لا توجد تنبيهات جديدة حالياً، جميع عمليات المتجر مستقرة",
                icon = Icons.Outlined.NotificationsNone,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onMarkAsRead(notif.id)
                                onNavigateToRelated(notif)
                            }
                            .testTag("notif_item_${notif.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!notif.isRead) PinkPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Icon Box
                            val iconTint = when (notif.type) {
                                NotificationType.NEW_ORDER -> PinkPrimary
                                NotificationType.LOW_STOCK -> Color(0xFFFBBF24)
                                NotificationType.ORDER_STATUS -> Color(0xFF34D399)
                                NotificationType.NEW_CUSTOMER -> Color(0xFF8B5CF6)
                                NotificationType.COUPON_EXPIRY -> Color(0xFFF97316)
                                NotificationType.SYSTEM_ALERT -> StatusCancelled
                            }

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(iconTint.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (notif.type) {
                                        NotificationType.NEW_ORDER -> Icons.Outlined.ShoppingBag
                                        NotificationType.LOW_STOCK -> Icons.Outlined.WarningAmber
                                        NotificationType.ORDER_STATUS -> Icons.Outlined.LocalShipping
                                        NotificationType.NEW_CUSTOMER -> Icons.Outlined.PersonAdd
                                        NotificationType.COUPON_EXPIRY -> Icons.Outlined.LocalOffer
                                        NotificationType.SYSTEM_ALERT -> Icons.Outlined.Info
                                    },
                                    contentDescription = null,
                                    tint = iconTint
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = Formatters.formatRelativeTime(notif.timestamp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = notif.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!notif.isRead) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(PinkPrimary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
