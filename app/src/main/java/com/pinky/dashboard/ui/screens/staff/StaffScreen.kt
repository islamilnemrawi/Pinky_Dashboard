package com.pinky.dashboard.ui.screens.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import com.pinky.dashboard.domain.model.Permission
import com.pinky.dashboard.domain.model.StaffMember
import com.pinky.dashboard.domain.model.UserRole
import com.pinky.dashboard.ui.components.ConfirmDestructiveDialog
import com.pinky.dashboard.ui.components.EmptyStateCard
import com.pinky.dashboard.ui.theme.PinkPrimary
import com.pinky.dashboard.ui.theme.StatusCancelled

@Composable
fun StaffScreen(
    staffMembers: List<StaffMember>,
    onSaveStaffMember: (StaffMember) -> Unit,
    onDeleteStaffMember: (String) -> Unit,
    onToggleActive: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingStaff by remember { mutableStateOf<StaffMember?>(null) }
    var staffToDelete by remember { mutableStateOf<StaffMember?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingStaff = null
                    showDialog = true
                },
                containerColor = PinkPrimary,
                contentColor = Color(0xFF381528),
                icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
                text = { Text("إضافة موظف جديد", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_staff_fab")
            )
        },
        modifier = modifier.testTag("staff_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                                text = "فريق العمل والصلاحيات",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "التحكم في أدوار موظفي متجر بينكي وحجب تكلفة الجملة وصافي الأرباح لغير المصرح لهم",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (staffMembers.isEmpty()) {
                EmptyStateCard(
                    title = "لا يوجد موظفون مضافون حالياً",
                    message = "أضف أفراد فريق المبيعات والمخزن مع تحديد الصلاحيات المناسبة لكل دور",
                    icon = Icons.Outlined.Group,
                    actionButtonText = "إضافة موظف",
                    onActionClick = {
                        editingStaff = null
                        showDialog = true
                    },
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("staff_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(staffMembers, key = { it.id }) { staff ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("staff_card_${staff.id}"),
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        // Circular Avatar Placeholder
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(PinkPrimary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (staff.name.isNotBlank() && staff.name != "Staff Member") {
                                                Text(
                                                    text = staff.name.trim().take(1).uppercase(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp,
                                                    color = PinkPrimary
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Outlined.Person,
                                                    contentDescription = null,
                                                    tint = PinkPrimary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(staff.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("${staff.email} • ${staff.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when (staff.role) {
                                                UserRole.OWNER -> PinkPrimary
                                                UserRole.ADMIN -> Color(0xFF34D399)
                                                else -> Color(0xFFFBBF24)
                                            }.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = staff.role.arabicLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = when (staff.role) {
                                                    UserRole.OWNER -> PinkPrimary
                                                    UserRole.ADMIN -> Color(0xFF34D399)
                                                    else -> Color(0xFFFBBF24)
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        Switch(
                                            checked = staff.isActive,
                                            onCheckedChange = { onToggleActive(staff.id, it) }
                                        )

                                        IconButton(onClick = {
                                            editingStaff = staff
                                            showDialog = true
                                        }) {
                                            Icon(Icons.Outlined.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        IconButton(onClick = { staffToDelete = staff }) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "حذف", tint = StatusCancelled)
                                        }
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                                Text(
                                    text = "الصلاحيات الممنوحة (${staff.permissions.size}):",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = if (staff.permissions.isEmpty()) "لا توجد صلاحيات مخصصة"
                                    else staff.permissions.joinToString("، ") { it.arabicLabel },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        StaffEditDialog(
            staff = editingStaff,
            onDismiss = { showDialog = false },
            onSave = {
                onSaveStaffMember(it)
                showDialog = false
            }
        )
    }

    staffToDelete?.let { staff ->
        ConfirmDestructiveDialog(
            title = "حذف حساب الموظف؟",
            message = "هل أنت متأكد من حذف حساب \"${staff.name}\" وإلغاء صلاحياته؟",
            confirmButtonText = "نعم، حذف الموظف",
            onConfirm = {
                onDeleteStaffMember(staff.id)
                staffToDelete = null
            },
            onDismiss = { staffToDelete = null }
        )
    }
}

@Composable
fun StaffEditDialog(
    staff: StaffMember?,
    onDismiss: () -> Unit,
    onSave: (StaffMember) -> Unit
) {
    var name by remember { mutableStateOf(staff?.name ?: "") }
    var email by remember { mutableStateOf(staff?.email ?: "") }
    var phone by remember { mutableStateOf(staff?.phone ?: "") }
    var role by remember { mutableStateOf(staff?.role ?: UserRole.EMPLOYEE) }
    var selectedPermissions by remember {
        mutableStateOf(
            staff?.permissions ?: setOf(Permission.VIEW_DASHBOARD, Permission.MANAGE_ORDERS)
        )
    }
    var isActive by remember { mutableStateOf(staff?.isActive ?: true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (staff == null) "إضافة موظف جديد" else "تعديل بيانات الموظف والصلاحيات",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = PinkPrimary
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الموظف *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text("الدور الوظيفي الأساسي:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserRole.entries.forEach { r ->
                            FilterChip(
                                selected = role == r,
                                onClick = { role = r },
                                label = { Text(r.arabicLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PinkPrimary,
                                    selectedLabelColor = Color(0xFF381528)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text("قائمة الصلاحيات الدقيقة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Permission.entries.forEach { perm ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(perm.arabicLabel, style = MaterialTheme.typography.bodyMedium)
                            Checkbox(
                                checked = selectedPermissions.contains(perm),
                                onCheckedChange = { checked ->
                                    selectedPermissions = if (checked) selectedPermissions + perm else selectedPermissions - perm
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val updated = StaffMember(
                                id = staff?.id ?: "staff_${System.currentTimeMillis()}",
                                name = name.ifBlank { "موظف مبيعات" },
                                email = email.ifBlank { "staff@pinkystore.com" },
                                phone = phone.ifBlank { "01000000000" },
                                role = role,
                                permissions = selectedPermissions,
                                isActive = isActive
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("save_staff_btn")
                    ) {
                        Text("حفظ الموظف", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}
