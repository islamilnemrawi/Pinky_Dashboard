package com.pinky.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pinky.dashboard.core.util.Formatters
import com.pinky.dashboard.domain.model.*
import com.pinky.dashboard.ui.navigation.DashboardSection
import com.pinky.dashboard.ui.theme.PinkPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchDialog(
    orders: List<Order>,
    products: List<Product>,
    customers: List<Customer>,
    categories: List<Category>,
    coupons: List<Coupon>,
    onDismiss: () -> Unit,
    onSelectOrder: (Order) -> Unit,
    onNavigateToSection: (DashboardSection) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredOrders = remember(searchQuery, orders) {
        if (searchQuery.isBlank()) emptyList()
        else orders.filter {
            it.orderNumber.contains(searchQuery, ignoreCase = true) ||
            it.customerName.contains(searchQuery, ignoreCase = true) ||
            it.customerPhone.contains(searchQuery)
        }.take(5)
    }

    val filteredProducts = remember(searchQuery, products) {
        if (searchQuery.isBlank()) emptyList()
        else products.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.internalCode?.contains(searchQuery, ignoreCase = true) == true ||
            it.categoryName.contains(searchQuery, ignoreCase = true)
        }.take(5)
    }

    val filteredCustomers = remember(searchQuery, customers) {
        if (searchQuery.isBlank()) emptyList()
        else customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery)
        }.take(5)
    }

    val filteredCategories = remember(searchQuery, categories) {
        if (searchQuery.isBlank()) emptyList()
        else categories.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }.take(5)
    }

    val filteredCoupons = remember(searchQuery, coupons) {
        if (searchQuery.isBlank()) emptyList()
        else coupons.filter {
            it.code.contains(searchQuery, ignoreCase = true)
        }.take(5)
    }

    val totalResults = filteredOrders.size + filteredProducts.size + filteredCustomers.size + filteredCategories.size + filteredCoupons.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("global_search_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Search Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "البحث الشامل في المتجر",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "إغلاق")
                    }
                }

                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_search_input"),
                    placeholder = { Text("ابحث برقم الطلب، اسم العميل، المنتج، الكوبون أو القسم...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = PinkPrimary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Clear, contentDescription = null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Results or Empty State
                if (searchQuery.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.ManageSearch,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "اكتب كلمة للبحث في كافة سجلات وعناصر متجر بينكي",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (totalResults == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لم يتم العثور على أية نتائج مطابقة لـ \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Orders
                        if (filteredOrders.isNotEmpty()) {
                            item {
                                SearchCategoryHeader("الطلبات (${filteredOrders.size})", Icons.Outlined.ShoppingBag)
                            }
                            items(filteredOrders) { order ->
                                SearchResultCard(
                                    title = "${order.orderNumber} - ${order.customerName}",
                                    subtitle = "${order.governorate} • ${Formatters.formatCurrencyEgp(order.totalAmount)}",
                                    badgeText = order.status.arabicLabel,
                                    onClick = {
                                        onSelectOrder(order)
                                        onDismiss()
                                    }
                                )
                            }
                        }

                        // Products
                        if (filteredProducts.isNotEmpty()) {
                            item {
                                SearchCategoryHeader("المنتجات (${filteredProducts.size})", Icons.Outlined.Inventory2)
                            }
                            items(filteredProducts) { product ->
                                SearchResultCard(
                                    title = product.name,
                                    subtitle = "${product.categoryName} • ${Formatters.formatCurrencyEgp(product.price)} • المخزون: ${product.stock}",
                                    badgeText = if (product.isActive) "نشط" else "معطل",
                                    onClick = {
                                        onNavigateToSection(DashboardSection.PRODUCTS)
                                        onDismiss()
                                    }
                                )
                            }
                        }

                        // Customers
                        if (filteredCustomers.isNotEmpty()) {
                            item {
                                SearchCategoryHeader("العملاء (${filteredCustomers.size})", Icons.Outlined.People)
                            }
                            items(filteredCustomers) { customer ->
                                SearchResultCard(
                                    title = customer.name,
                                    subtitle = "هاتف: ${customer.phone} • طلبات: ${customer.totalOrders}",
                                    badgeText = Formatters.formatCurrencyEgp(customer.totalSpend),
                                    onClick = {
                                        onNavigateToSection(DashboardSection.CUSTOMERS)
                                        onDismiss()
                                    }
                                )
                            }
                        }

                        // Coupons
                        if (filteredCoupons.isNotEmpty()) {
                            item {
                                SearchCategoryHeader("الكوبونات (${filteredCoupons.size})", Icons.Outlined.Discount)
                            }
                            items(filteredCoupons) { coupon ->
                                SearchResultCard(
                                    title = "كوبون: ${coupon.code}",
                                    subtitle = "الخصم: ${coupon.discountValue}${if (coupon.discountType == DiscountType.PERCENTAGE) "%" else " ج.م"}",
                                    badgeText = if (coupon.isActive) "ساري" else "غير مفعّل",
                                    onClick = {
                                        onNavigateToSection(DashboardSection.OFFERS_COUPONS)
                                        onDismiss()
                                    }
                                )
                            }
                        }

                        // Categories
                        if (filteredCategories.isNotEmpty()) {
                            item {
                                SearchCategoryHeader("الأقسام (${filteredCategories.size})", Icons.Outlined.Category)
                            }
                            items(filteredCategories) { cat ->
                                SearchResultCard(
                                    title = cat.name,
                                    subtitle = "عدد المنتجات: ${cat.productsCount}",
                                    badgeText = if (cat.isActive) "مفعّل" else "معطّل",
                                    onClick = {
                                        onNavigateToSection(DashboardSection.CATEGORIES)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchCategoryHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(18.dp))
        Text(title, fontWeight = FontWeight.Bold, color = PinkPrimary, fontSize = 13.sp)
    }
}

@Composable
private fun SearchResultCard(
    title: String,
    subtitle: String,
    badgeText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = PinkPrimary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = PinkPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
