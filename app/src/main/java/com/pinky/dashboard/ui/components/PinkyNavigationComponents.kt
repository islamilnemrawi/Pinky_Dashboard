package com.pinky.dashboard.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinky.dashboard.domain.model.AdminUser
import com.pinky.dashboard.domain.model.UserRole
import com.pinky.dashboard.ui.navigation.DashboardSection
import com.pinky.dashboard.ui.theme.CharcoalSurfaceVariant
import com.pinky.dashboard.ui.theme.PinkPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinkyTopBar(
    currentSection: DashboardSection,
    currentUser: AdminUser?,
    unreadNotificationsCount: Int,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSwitchRole: (UserRole) -> Unit,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onQuickActionClick: () -> Unit,
    onAiClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    isRefreshing: Boolean = false,
    lastSyncTimestamp: String? = null,
    onRefreshClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showRoleMenu by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = modifier
            .testTag("pinky_top_bar")
            .background(MaterialTheme.colorScheme.surface),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.testTag("top_bar_menu_btn")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = "القائمة الجانبية للأقسام",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pinky Logo badge
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(PinkPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "P",
                        color = Color(0xFF381528),
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Pinky",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PinkPrimary
                            )
                        )
                        Text(
                            text = " Dashboard",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    Text(
                        text = currentSection.arabicTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            // Last Sync Timestamp
            if (!lastSyncTimestamp.isNullOrBlank()) {
                Text(
                    text = "تحديث: $lastSyncTimestamp",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = PinkPrimary
                    ),
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            // Circular Refresh Button with rotation animation
            val transition = rememberInfiniteTransition(label = "rotation")
            val rotationAngle by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            IconButton(
                onClick = onRefreshClick,
                enabled = !isRefreshing,
                modifier = Modifier.testTag("top_bar_refresh_btn")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "تحديث البيانات",
                    tint = PinkPrimary,
                    modifier = if (isRefreshing) Modifier.rotate(rotationAngle) else Modifier
                )
            }

            // Notifications Bell
            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier.testTag("top_bar_notif_btn")
            ) {
                if (unreadNotificationsCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text("$unreadNotificationsCount", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "الإشعارات",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "الإشعارات",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))
        }
    )
}

@Composable
fun PinkyNavigationDrawerContent(
    currentSection: DashboardSection,
    currentUser: AdminUser?,
    unreadNotificationsCount: Int,
    onSelectSection: (DashboardSection) -> Unit,
    onOpenAiChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier.fillMaxWidth(0.85f),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurfaceVariant),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PinkPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "P",
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = Color(0xFF381528)
                        )
                    }

                    Column {
                        Text(
                            text = "Pinky Store Dashboard",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${currentUser?.name ?: "المدير العام"} • ${currentUser?.role?.arabicLabel ?: "المالك"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PinkPrimary
                        )
                    }
                }
            }

            // Pinky AI Quick Banner in Drawer
            Surface(
                onClick = onOpenAiChat,
                shape = RoundedCornerShape(12.dp),
                color = PinkPrimary.copy(alpha = 0.12f),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = PinkPrimary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("مساعد Pinky الذكي", fontWeight = FontWeight.Bold, color = PinkPrimary, fontSize = 13.sp)
                        Text("اسأل الذكاء الاصطناعي عن المبيعات والمخزون", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(16.dp))
                }
            }

            // Navigation Groups
            val coreSections = listOf(
                DashboardSection.HOME,
                DashboardSection.ORDERS,
                DashboardSection.PRODUCTS,
                DashboardSection.CATEGORIES
            )

            val salesMarketingSections = listOf(
                DashboardSection.OFFERS_COUPONS,
                DashboardSection.CUSTOMERS,
                DashboardSection.PRIME
            )

            val operationsSections = listOf(
                DashboardSection.SHIPPING,
                DashboardSection.PAYMENTS
            )

            val managementSections = listOf(
                DashboardSection.STAFF,
                DashboardSection.WEBSITE_EDITOR,
                DashboardSection.REPORTS
            )

            val systemSections = listOf(
                DashboardSection.NOTIFICATIONS,
                DashboardSection.HEALTH_CHECK,
                DashboardSection.SETTINGS
            )

            DrawerSectionGroup(
                title = "الأساسيات",
                sections = coreSections,
                currentSection = currentSection,
                currentUser = currentUser,
                unreadNotificationsCount = unreadNotificationsCount,
                onSelectSection = onSelectSection
            )

            DrawerSectionGroup(
                title = "التسويق والمبيعات",
                sections = salesMarketingSections,
                currentSection = currentSection,
                currentUser = currentUser,
                unreadNotificationsCount = unreadNotificationsCount,
                onSelectSection = onSelectSection
            )

            DrawerSectionGroup(
                title = "العمليات واللوجستيات",
                sections = operationsSections,
                currentSection = currentSection,
                currentUser = currentUser,
                unreadNotificationsCount = unreadNotificationsCount,
                onSelectSection = onSelectSection
            )

            DrawerSectionGroup(
                title = "الإدارة والتحكم",
                sections = managementSections,
                currentSection = currentSection,
                currentUser = currentUser,
                unreadNotificationsCount = unreadNotificationsCount,
                onSelectSection = onSelectSection
            )

            DrawerSectionGroup(
                title = "النظام والمتابعة",
                sections = systemSections,
                currentSection = currentSection,
                currentUser = currentUser,
                unreadNotificationsCount = unreadNotificationsCount,
                onSelectSection = onSelectSection
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DrawerSectionGroup(
    title: String,
    sections: List<DashboardSection>,
    currentSection: DashboardSection,
    currentUser: AdminUser?,
    unreadNotificationsCount: Int,
    onSelectSection: (DashboardSection) -> Unit
) {
    val allowedSections = sections.filter { s ->
        s.requiredPermission == null || currentUser?.hasPermission(s.requiredPermission) == true
    }

    if (allowedSections.isNotEmpty()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        allowedSections.forEach { section ->
            val isSelected = currentSection == section
            NavigationDrawerItem(
                icon = {
                    if (section == DashboardSection.NOTIFICATIONS && unreadNotificationsCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = PinkPrimary) {
                                    Text("$unreadNotificationsCount", color = Color(0xFF381528))
                                }
                            }
                        ) {
                            Icon(section.icon, contentDescription = null)
                        }
                    } else {
                        Icon(section.icon, contentDescription = null)
                    }
                },
                label = { Text(section.arabicTitle, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                selected = isSelected,
                onClick = { onSelectSection(section) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = PinkPrimary.copy(alpha = 0.16f),
                    selectedIconColor = PinkPrimary,
                    selectedTextColor = PinkPrimary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
fun PinkyBottomNav(
    currentSection: DashboardSection,
    onSelectSection: (DashboardSection) -> Unit,
    onOpenDrawer: () -> Unit,
    currentUser: AdminUser?,
    modifier: Modifier = Modifier
) {
    val visibleSections = DashboardSection.entries.filter { section ->
        section.requiredPermission == null || currentUser?.hasPermission(section.requiredPermission) == true
    }

    val primaryTabs = listOf(
        DashboardSection.HOME,
        DashboardSection.ORDERS,
        DashboardSection.PRODUCTS,
        DashboardSection.CUSTOMERS
    ).filter { it in visibleSections }

    NavigationBar(
        modifier = modifier
            .testTag("bottom_nav_bar")
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        primaryTabs.forEach { section ->
            val isSelected = currentSection == section

            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectSection(section) },
                icon = {
                    Icon(
                        imageVector = section.icon,
                        contentDescription = section.arabicTitle
                    )
                },
                label = {
                    Text(
                        text = section.arabicTitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF381528),
                    selectedTextColor = PinkPrimary,
                    indicatorColor = PinkPrimary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_item_${section.route}")
            )
        }

        // "المزيد" / More Drawer Tab
        val isMoreSelected = currentSection !in primaryTabs
        NavigationBarItem(
            selected = isMoreSelected,
            onClick = onOpenDrawer,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Widgets,
                    contentDescription = "المزيد من الأقسام"
                )
            },
            label = {
                Text(
                    text = "الأقسام",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isMoreSelected) FontWeight.Bold else FontWeight.Normal
                    )
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF381528),
                selectedTextColor = PinkPrimary,
                indicatorColor = PinkPrimary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_item_more")
        )
    }
}
