package com.pinky.dashboard.domain.repository

import com.pinky.dashboard.domain.model.*
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun getOrdersFlow(): Flow<List<Order>>
    suspend fun getOrderById(orderId: String): Order?
    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus, note: String? = null): Result<Order>
    suspend fun syncOrders(): Result<Unit>
    suspend fun addOrder(order: Order): Result<Order>
}

interface ProductRepository {
    fun getProductsFlow(): Flow<List<Product>>
    suspend fun getProductById(productId: String): Product?
    suspend fun saveProduct(product: Product): Result<Product>
    suspend fun deleteProduct(productId: String): Result<Unit>
    suspend fun toggleProductActive(productId: String, isActive: Boolean): Result<Unit>
    suspend fun syncProducts(): Result<Unit>
}

interface CategoryRepository {
    fun getCategoriesFlow(): Flow<List<Category>>
    suspend fun saveCategory(category: Category): Result<Category>
    suspend fun deleteCategory(categoryId: String): Result<Unit>
    suspend fun syncCategories(): Result<Unit>
    suspend fun clearLocalCategories()
}

interface OfferRepository {
    fun getOffersFlow(): Flow<List<Offer>>
    suspend fun saveOffer(offer: Offer): Result<Offer>
    suspend fun deleteOffer(offerId: String): Result<Unit>
    suspend fun syncOffers(): Result<Unit>
}

interface ShippingRepository {
    fun getGovernoratesFlow(): Flow<List<ShippingGovernorate>>
    fun getShippingCentersFlow(): Flow<List<ShippingCenter>>
    suspend fun saveGovernorate(gov: ShippingGovernorate): Result<ShippingGovernorate>
    suspend fun saveShippingCenter(center: ShippingCenter): Result<ShippingCenter>
    suspend fun deleteShippingCenter(centerId: String): Result<Unit>
    suspend fun syncShipping(): Result<Unit>
    suspend fun clearLocalShipping()
}

interface CustomerRepository {
    fun getCustomersFlow(): Flow<List<Customer>>
    suspend fun getCustomerById(customerId: String): Customer?
    suspend fun getCustomerOrders(customerPhone: String): List<Order>
    suspend fun syncCustomers(): Result<Unit>
}

interface AuthRepository {
    fun getCurrentUserFlow(): Flow<AdminUser?>
    suspend fun login(email: String, pass: String): Result<AdminUser>
    suspend fun logout()
    suspend fun switchRoleForTesting(role: UserRole)
    suspend fun initializeAuthSession(): Boolean
    suspend fun refreshAuthSessionIfNeeded(): Boolean
}

interface AnalyticsRepository {
    fun getAnalyticsFlow(timeframeDays: Int = 30): Flow<AnalyticsData>
}
