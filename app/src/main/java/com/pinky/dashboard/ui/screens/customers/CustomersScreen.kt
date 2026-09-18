package com.pinky.dashboard.ui.screens.customers

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pinky.dashboard.core.util.Formatters
import com.pinky.dashboard.domain.model.Customer
import com.pinky.dashboard.domain.model.Order
import com.pinky.dashboard.ui.components.EmptyStateCard
import com.pinky.dashboard.ui.components.PinkyStatCard
import com.pinky.dashboard.ui.theme.PinkPrimary

@Composable
fun CustomersScreen(
    customers: List<Customer>,
    orders: List<Order>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }

    val filteredCustomers = remember(customers, searchQuery) {
        customers.filter { c ->
            searchQuery.isBlank() ||
                c.name.contains(searchQuery, ignoreCase = true) ||
                c.phone.contains(searchQuery) ||
                c.governorate.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("customers_screen")
    ) {
        // Search
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customers_search_input"),
                placeholder = { Text("بحث باسم العميل، رقم الهاتف، أو المحافظة...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = PinkPrimary) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (filteredCustomers.isEmpty()) {
            EmptyStateCard(
                title = "لا يوجد عملاء مسجلين بعد",
                message = "يتم تسجيل ملفات العملاء وسجل مشترياتهم تلقائياً عند استلام أي طلب في المتجر",
                icon = Icons.Outlined.People,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("customers_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredCustomers, key = { it.id }) { customer ->
                    CustomerCardItem(
                        customer = customer,
                        onClick = { selectedCustomer = customer }
                    )
                }
            }
        }
    }

    selectedCustomer?.let { customer ->
        val customerOrders = remember(customer, orders) {
            orders.filter { it.customerPhone == customer.phone }
        }

        CustomerDetailDialog(
            customer = customer,
            customerOrders = customerOrders,
            onDismiss = { selectedCustomer = null }
        )
    }
}

@Composable
fun CustomerCardItem(
    customer: Customer,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("customer_card_${customer.id}"),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PinkPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = PinkPrimary)
                }

                Column {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${customer.phone} • ${customer.governorate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatCurrencyEgp(customer.totalSpend),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PinkPrimary
                )
                Text(
                    text = "${customer.totalOrders} طلبات",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CustomerDetailDialog(
    customer: Customer,
    customerOrders: List<Order>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("customer_detail_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(customer.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text(customer.phone, color = PinkPrimary, style = MaterialTheme.typography.bodyMedium)
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "إغلاق")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Contact Buttons
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                    context.startActivity(callIntent)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = Color(0xFF381528)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Phone, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("اتصال هاتفي")
                            }

                            Button(
                                onClick = {
                                    val clean = customer.phone.replace("+", "").replace(" ", "")
                                    val phone = if (clean.startsWith("01")) "2$clean" else clean
                                    val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$phone"))
                                    context.startActivity(waIntent)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Chat, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("واتساب")
                            }
                        }
                    }

                    // Stats row
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PinkyStatCard(
                                title = "إجمالي المشتريات",
                                value = Formatters.formatCurrencyEgp(customer.totalSpend),
                                icon = Icons.Outlined.Payments,
                                modifier = Modifier.weight(1f)
                            )
                            PinkyStatCard(
                                title = "عدد الطلبات",
                                value = "${customer.totalOrders}",
                                icon = Icons.Outlined.ShoppingBag,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Text(
                            text = "العنوان: ${customer.governorate} - ${customer.address}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        Text(
                            text = "سجل طلبات العميل (${customerOrders.size}):",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    items(customerOrders, key = { it.id }) { order ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("#${order.orderNumber}", fontWeight = FontWeight.Bold, color = PinkPrimary)
                                    Text(Formatters.formatOrderDate(order.createdAt), style = MaterialTheme.typography.bodySmall)
                                    Text("الحالة: ${order.status.arabicLabel}", style = MaterialTheme.typography.bodySmall)
                                }

                                Text(
                                    text = Formatters.formatCurrencyEgp(order.totalAmount),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
