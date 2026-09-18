package com.pinky.dashboard.data.local.dao

import androidx.room.*
import com.pinky.dashboard.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrdersFlow(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItems(orderId: String): List<OrderItemEntity>

    @Query("SELECT * FROM order_items WHERE orderId IN (:orderIds)")
    suspend fun getItemsForOrders(orderIds: List<String>): List<OrderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String)

    @Query("SELECT * FROM orders WHERE customerPhone = :phone ORDER BY createdAt DESC")
    suspend fun getOrdersByCustomerPhone(phone: String): List<OrderEntity>

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun getOrderCount(): Int
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun getAllProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProduct(productId: String)

    @Query("UPDATE products SET isActive = :isActive WHERE id = :productId")
    suspend fun updateActiveStatus(productId: String, isActive: Boolean)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
}

@Dao
interface OfferDao {
    @Query("SELECT * FROM offers ORDER BY sortOrder ASC")
    fun getAllOffersFlow(): Flow<List<OfferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffer(offer: OfferEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffers(offers: List<OfferEntity>)

    @Query("DELETE FROM offers WHERE id = :offerId")
    suspend fun deleteOffer(offerId: String)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY totalSpend DESC")
    fun getAllCustomersFlow(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE phone = :phone LIMIT 1")
    suspend fun getCustomerByPhone(phone: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)
}

@Dao
interface ShippingDao {
    @Query("SELECT * FROM shipping_governorates ORDER BY name ASC")
    fun getAllGovernoratesFlow(): Flow<List<ShippingGovernorateEntity>>

    @Query("SELECT * FROM shipping_centers ORDER BY name ASC")
    fun getAllCentersFlow(): Flow<List<ShippingCenterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGovernorate(gov: ShippingGovernorateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGovernorates(govs: List<ShippingGovernorateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCenter(center: ShippingCenterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCenters(centers: List<ShippingCenterEntity>)

    @Query("DELETE FROM shipping_centers WHERE id = :centerId")
    suspend fun deleteCenter(centerId: String)

    @Query("DELETE FROM shipping_governorates")
    suspend fun deleteAllGovernorates()

    @Query("DELETE FROM shipping_centers")
    suspend fun deleteAllCenters()
}
