package com.pinky.dashboard.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pinky.dashboard.ui.navigation.DashboardSection
import com.pinky.dashboard.ui.theme.PinkPrimary

data class QuickActionItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val targetSection: DashboardSection
)

@Composable
fun QuickActionsDialog(
    onDismiss: () -> Unit,
    onSelectAction: (DashboardSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        QuickActionItem("إضافة منتج", "إدراج منتج جديد بالمخزن", Icons.Outlined.AddBox, DashboardSection.PRODUCTS),
        QuickActionItem("إضافة عرض", "إنشاء بنر أو تخفيض", Icons.Outlined.LocalOffer, DashboardSection.OFFERS_COUPONS),
        QuickActionItem("إدارة الطلبات", "متابعة أحدث أوردرات المتجر", Icons.Outlined.ShoppingBag, DashboardSection.ORDERS),
        QuickActionItem("تعديل الشحن", "تحديث أسعار المحافظات", Icons.Outlined.LocalShipping, DashboardSection.SHIPPING),
        QuickActionItem("إضافة موظف", "تعيين عضو فريق وصلاحيات", Icons.Outlined.PersonAdd, DashboardSection.STAFF),
        QuickActionItem("تحرير الموقع", "التعديل البصري للمتجر", Icons.Outlined.Web, DashboardSection.WEBSITE_EDITOR)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("quick_actions_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Bolt, contentDescription = null, tint = PinkPrimary)
                        Text(
                            text = "الإجراءات السريعة",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "إغلاق")
                    }
                }

                Text(
                    text = "الوصول المباشر لأكثر العمليات الإدارية استخداماً في متجر بينكي:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(actions) { action ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectAction(action.targetSection)
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(action.icon, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(24.dp))
                                Text(action.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(action.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
