package com.pinky.dashboard.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinky.dashboard.core.sync.SyncInterval
import com.pinky.dashboard.data.remote.SupabaseConfig
import com.pinky.dashboard.domain.model.AdminUser
import com.pinky.dashboard.domain.model.UserRole
import com.pinky.dashboard.ui.theme.PinkPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUser: AdminUser?,
    isDarkTheme: Boolean,
    syncInterval: SyncInterval,
    isSyncing: Boolean,
    onToggleTheme: () -> Unit,
    onUpdateSyncInterval: (SyncInterval) -> Unit,
    onManualSync: () -> Unit,
    onTriggerTestNotification: () -> Unit,
    onSwitchRole: (UserRole) -> Unit,
    onSeedData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showIntervalDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Store Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(PinkPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "P",
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            color = Color(0xFF381528)
                        )
                    }

                    Column {
                        Text(
                            text = "متجر بينكي (Pinky Store)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "لوحة التحكم الإدارية الرسمية • جمهورية مصر العربية 🇪🇬",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "العملة الأساسية: الجنيه المصري (EGP)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = PinkPrimary
                        )
                    }
                }
            }
        }

        // Active User & Permissions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "الحساب الحالي ومستوى الصلاحيات",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(currentUser?.name ?: "مسؤول بينكي", fontWeight = FontWeight.Bold)
                            Text(currentUser?.email ?: "admin@pinky.eg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PinkPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = currentUser?.role?.arabicLabel ?: "المالك",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = PinkPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = "اختبار الصلاحيات الحية (انقر لتجربة الأدوار المختلفة):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserRole.entries.forEach { role ->
                            val isSelected = currentUser?.role == role
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSwitchRole(role) },
                                label = { Text(role.arabicLabel, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PinkPrimary,
                                    selectedLabelColor = Color(0xFF381528)
                                ),
                                modifier = Modifier.weight(1f).testTag("role_switch_${role.name}")
                            )
                        }
                    }
                }
            }
        }

        // Supabase Connection Status
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "حالة الاتصال بقاعدة بيانات سوبابيز (Supabase)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34D399))
                            )
                            Text(
                                text = "متصل",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF34D399)
                            )
                        }
                    }

                    Text(
                        text = "جداول متجر بينكي المهيأة:\n• ${SupabaseConfig.TABLE_ORDERS} (الطلبات)\n• ${SupabaseConfig.TABLE_PRODUCTS} (المنتجات)\n• ${SupabaseConfig.TABLE_CATEGORIES} (الأقسام)\n• ${SupabaseConfig.TABLE_SHIPPING} (الشحن)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onManualSync,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528)),
                        modifier = Modifier.fillMaxWidth().testTag("sync_now_btn")
                    ) {
                        Icon(Icons.Outlined.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSyncing) "جاري المزامنة الآن..." else "مزامنة البيانات الآن", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Automatic Sync & Polling
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "المزامنة التلقائية والتحقق من الطلبات",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    ExposedDropdownMenuBox(
                        expanded = showIntervalDropdown,
                        onExpandedChange = { showIntervalDropdown = !showIntervalDropdown }
                    ) {
                        OutlinedTextField(
                            value = syncInterval.arabicLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("فترة التحديث التلقائي") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showIntervalDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = showIntervalDropdown,
                            onDismissRequest = { showIntervalDropdown = false }
                        ) {
                            SyncInterval.entries.forEach { interval ->
                                DropdownMenuItem(
                                    text = { Text(interval.arabicLabel) },
                                    onClick = {
                                        onUpdateSyncInterval(interval)
                                        showIntervalDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Push Notifications Test
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "إشعارات الطلبات الفورية 🔔",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "تم تجهيز القناة الخاصة بإشعارات وصول الطلبات الجديدة والتنبيه الصوتي والاهتزاز.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onTriggerTestNotification,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("test_notification_btn")
                    ) {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = PinkPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إرسال إشعار تجريبي لطلب جديد")
                    }
                }
            }
        }

        // Appearance Mode
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("المظهر الليلي الداكن (Dark Mode)", fontWeight = FontWeight.Bold)
                        Text(
                            text = "تصميم فحمى مريح للعين مع لمسات بينكي الوردية",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleTheme() },
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }
        }

        // Seed Data Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تهيئة بيانات المتجر الأساسية", fontWeight = FontWeight.Bold)
                    Text(
                        text = "إضافة أمثلة للأقسام والمنتجات والطلبات والعملاء لاختبار جميع شاشات لوحة التحكم.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onSeedData,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("seed_data_btn")
                    ) {
                        Text("إعادة تعبئة البيانات التجريبية للمتجر")
                    }
                }
            }
        }
    }
}
