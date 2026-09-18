package com.pinky.dashboard

import com.pinky.dashboard.core.util.Formatters
import com.pinky.dashboard.domain.model.AdminUser
import com.pinky.dashboard.domain.model.Permission
import com.pinky.dashboard.domain.model.UserRole
import org.junit.Assert.*
import org.junit.Test

class PinkyDashboardTest {

    @Test
    fun testCurrencyFormatting() {
        val formatted = Formatters.formatCurrencyEgp(1250.0)
        assertTrue(formatted.contains("1,250") || formatted.contains("1250"))
        assertTrue(formatted.contains("ج.م"))
    }

    @Test
    fun testOwnerPermissionsIncludeProfitAndWholesale() {
        val owner = AdminUser(
            id = "owner_1",
            name = "المالك",
            email = "owner@pinky.eg",
            role = UserRole.OWNER,
            permissions = Permission.defaultPermissionsFor(UserRole.OWNER)
        )

        assertTrue(owner.canViewProfit)
        assertTrue(owner.canViewWholesale)
        assertTrue(owner.canViewInternalCode)
    }

    @Test
    fun testEmployeePermissionsExcludeProfitAndWholesale() {
        val employee = AdminUser(
            id = "emp_1",
            name = "موظف المبيعات",
            email = "emp@pinky.eg",
            role = UserRole.EMPLOYEE,
            permissions = Permission.defaultPermissionsFor(UserRole.EMPLOYEE)
        )

        assertFalse(employee.canViewProfit)
        assertFalse(employee.canViewWholesale)
        assertFalse(employee.canViewInternalCode)
        assertTrue(employee.hasPermission(Permission.MANAGE_ORDERS))
    }
}
