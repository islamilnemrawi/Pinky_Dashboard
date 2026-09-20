package com.pinky.dashboard.data.repository

import com.pinky.dashboard.data.local.PinkyDatabase
import com.pinky.dashboard.data.local.entity.*
import com.pinky.dashboard.data.remote.*
import com.pinky.dashboard.domain.model.*
import com.pinky.dashboard.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun isNetworkException(e: Throwable): Boolean {
    val msg = e.message ?: ""
    return e is java.net.UnknownHostException ||
           e is java.net.ConnectException ||
           e is java.net.SocketTimeoutException ||
           e is java.io.InterruptedIOException ||
           msg.contains("Unable to resolve host", ignoreCase = true) ||
           msg.contains("No address associated with hostname", ignoreCase = true) ||
           msg.contains("Failed to connect", ignoreCase = true)
}

private fun isSupabasePermissionError(code: Int, payload: String?): Boolean {
    if (code == 403 || code == 401) return true
    val lower = payload?.lowercase() ?: ""
    return lower.contains("42501") ||
           lower.contains("permission denied") ||
           lower.contains("privileges") ||
           lower.contains("row-level security") ||
           lower.contains("rls")
}

class OrderRepositoryImpl(
    private val database: PinkyDatabase
) : OrderRepository {
    private val orderDao = database.orderDao()

    override fun getOrdersFlow(): Flow<List<Order>> {
        return orderDao.getAllOrdersFlow().map { entities ->
            entities.map { entity ->
                val items = orderDao.getOrderItems(entity.id).map { itemEntity ->
                    OrderItem(
                        id = itemEntity.id,
                        productId = itemEntity.productId,
                        productName = itemEntity.productName,
                        variantName = itemEntity.variantName,
                        color = itemEntity.color,
                        size = itemEntity.size,
                        quantity = itemEntity.quantity,
                        unitPrice = itemEntity.unitPrice,
                        lineTotal = itemEntity.lineTotal,
                        imageUrl = itemEntity.imageUrl
                    )
                }
                Order(
                    id = entity.id,
                    orderNumber = entity.orderNumber,
                    customerName = entity.customerName,
                    customerPhone = entity.customerPhone,
                    customerAddress = entity.customerAddress,
                    governorate = entity.governorate,
                    shippingCenter = entity.shippingCenter,
                    items = items,
                    subtotalAmount = entity.subtotalAmount,
                    shippingFee = entity.shippingFee,
                    discountAmount = entity.discountAmount,
                    totalAmount = entity.totalAmount,
                    paymentMethod = PaymentMethod.fromString(entity.paymentMethod),
                    paymentStatus = PaymentStatus.valueOf(entity.paymentStatus),
                    status = OrderStatus.fromString(entity.status),
                    createdAt = entity.createdAt,
                    notes = entity.notes
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getOrderById(orderId: String): Order? = withContext(Dispatchers.IO) {
        val entity = orderDao.getOrderById(orderId) ?: return@withContext null
        val items = orderDao.getOrderItems(entity.id).map { itemEntity ->
            OrderItem(
                id = itemEntity.id,
                productId = itemEntity.productId,
                productName = itemEntity.productName,
                variantName = itemEntity.variantName,
                color = itemEntity.color,
                size = itemEntity.size,
                quantity = itemEntity.quantity,
                unitPrice = itemEntity.unitPrice,
                lineTotal = itemEntity.lineTotal,
                imageUrl = itemEntity.imageUrl
            )
        }
        Order(
            id = entity.id,
            orderNumber = entity.orderNumber,
            customerName = entity.customerName,
            customerPhone = entity.customerPhone,
            customerAddress = entity.customerAddress,
            governorate = entity.governorate,
            shippingCenter = entity.shippingCenter,
            items = items,
            subtotalAmount = entity.subtotalAmount,
            shippingFee = entity.shippingFee,
            discountAmount = entity.discountAmount,
            totalAmount = entity.totalAmount,
            paymentMethod = PaymentMethod.fromString(entity.paymentMethod),
            paymentStatus = PaymentStatus.valueOf(entity.paymentStatus),
            status = OrderStatus.fromString(entity.status),
            createdAt = entity.createdAt,
            notes = entity.notes
        )
    }

    override suspend fun updateOrderStatus(
        orderId: String,
        newStatus: OrderStatus,
        note: String?
    ): Result<Order> = withContext(Dispatchers.IO) {
        try {
            // 1. Update in Supabase
            val response = SupabaseClient.service.updateOrderStatus(
                "eq.$orderId",
                mapOf("status" to newStatus.arabicLabel)
            )
            if (!response.isSuccessful) {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                return@withContext Result.failure(Exception("Supabase updateOrderStatus failed: $errorMsg"))
            }

            // 2. Update local Cache
            orderDao.updateOrderStatus(orderId, newStatus.arabicLabel)
            val updated = getOrderById(orderId) ?: throw IllegalStateException("Order not found")
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncOrders(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = SupabaseClient.service.getOrders()
            if (response.isSuccessful) {
                val orders = response.body() ?: emptyList()
                val orderEntities = orders.map { dto ->
                    OrderEntity(
                        id = dto.id,
                        orderNumber = dto.orderNumber,
                        customerName = dto.customerName,
                        customerPhone = dto.phone,
                        customerAddress = dto.address ?: "",
                        governorate = dto.governorate ?: "",
                        shippingCenter = dto.center ?: "",
                        subtotalAmount = dto.subtotal ?: 0.0,
                        shippingFee = dto.shippingCost ?: 0.0,
                        discountAmount = dto.discountAmount ?: 0.0,
                        totalAmount = dto.total ?: 0.0,
                        paymentMethod = dto.paymentMethod ?: "",
                        paymentStatus = if (dto.status == "تم التسليم" || dto.status == "DELIVERED") "PAID" else "PENDING",
                        status = dto.status ?: "NEW",
                        createdAt = dto.createdAt?.toLongOrNull() ?: System.currentTimeMillis(),
                        notes = dto.notes ?: "",
                        isSynced = true
                    )
                }
                orderDao.insertOrders(orderEntities)

                val itemEntities = orders.flatMap { dto ->
                    dto.items?.map { item ->
                        OrderItemEntity(
                            id = item.id ?: java.util.UUID.randomUUID().toString(),
                            orderId = dto.id,
                            productId = item.productId ?: "",
                            productName = item.productName ?: "",
                            variantName = item.variantName ?: "",
                            color = item.color ?: "",
                            size = item.size ?: "",
                            quantity = item.quantity ?: 1,
                            unitPrice = item.unitPrice ?: 0.0,
                            lineTotal = item.lineTotal ?: 0.0,
                            imageUrl = item.imageUrl ?: ""
                        )
                    } ?: emptyList()
                }
                if (itemEntities.isNotEmpty()) {
                    orderDao.insertOrderItems(itemEntities)
                }
                android.util.Log.d("SupabaseSync", "Table: orders | Operation: syncOrders | Status: Success | Count: ${orders.size}")
                Result.success(Unit)
            } else {
                val errorBodyStr = response.errorBody()?.string() ?: ""
                val errMsg = "Table: orders | Operation: syncOrders | Error: HTTP ${response.code()} ${response.message()} | Payload: $errorBodyStr"
                if (isSupabasePermissionError(response.code(), errorBodyStr)) {
                    android.util.Log.w("SupabaseSync", "Table: orders | Operation: syncOrders | Status: Restricted (HTTP ${response.code()} / RLS). Using local cached data.")
                    Result.success(Unit)
                } else {
                    android.util.Log.e("SupabaseError", errMsg)
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            if (isNetworkException(e)) {
                android.util.Log.w("SupabaseSync", "syncOrders offline: ${e.message}")
            } else {
                android.util.Log.e("SupabaseError", "Table: orders | Operation: syncOrders | Exception: ${e.message}", e)
            }
            Result.failure(e)
        }
    }

    override suspend fun addOrder(order: Order): Result<Order> = withContext(Dispatchers.IO) {
        try {
            // 1. Save to Supabase (Natively including items in the JSONB column!)
            val orderItemsDto = order.items.map { item ->
                SupabaseOrderItemDto(
                    id = item.id,
                    orderId = order.id,
                    productId = item.productId,
                    productName = item.productName,
                    variantName = item.variantName,
                    color = item.color,
                    size = item.size,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    lineTotal = item.lineTotal,
                    imageUrl = item.imageUrl
                )
            }

            val dto = SupabaseOrderDto(
                id = order.id,
                orderNumber = order.orderNumber,
                customerName = order.customerName,
                phone = order.customerPhone,
                address = order.customerAddress,
                governorate = order.governorate,
                center = order.shippingCenter,
                subtotal = order.subtotalAmount,
                shippingCost = order.shippingFee,
                discountAmount = order.discountAmount,
                total = order.totalAmount,
                paymentMethod = order.paymentMethod.arabicLabel,
                status = order.status.arabicLabel,
                createdAt = order.createdAt.toString(),
                notes = order.notes,
                items = orderItemsDto
            )

            val isPlaceholderKey = SupabaseClient.supabaseAnonKey.contains("placeholder")
            if (!isPlaceholderKey) {
                val orderResponse = SupabaseClient.service.upsertOrder(dto)
                if (!orderResponse.isSuccessful) {
                    val errorMsg = orderResponse.errorBody()?.string() ?: orderResponse.message()
                    return@withContext Result.failure(Exception("Supabase addOrder failed: $errorMsg"))
                }
            }

            // 2. Save to local Cache
            val entity = OrderEntity(
                id = order.id,
                orderNumber = order.orderNumber,
                customerName = order.customerName,
                customerPhone = order.customerPhone,
                customerAddress = order.customerAddress,
                governorate = order.governorate,
                shippingCenter = order.shippingCenter,
                subtotalAmount = order.subtotalAmount,
                shippingFee = order.shippingFee,
                discountAmount = order.discountAmount,
                totalAmount = order.totalAmount,
                paymentMethod = order.paymentMethod.arabicLabel,
                paymentStatus = order.paymentStatus.name,
                status = order.status.arabicLabel,
                createdAt = order.createdAt,
                notes = order.notes,
                isSynced = true
            )
            orderDao.insertOrder(entity)

            val itemEntities = order.items.map { item ->
                OrderItemEntity(
                    id = item.id,
                    orderId = order.id,
                    productId = item.productId,
                    productName = item.productName,
                    variantName = item.variantName,
                    color = item.color,
                    size = item.size,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    lineTotal = item.lineTotal,
                    imageUrl = item.imageUrl
                )
            }
            orderDao.insertOrderItems(itemEntities)

            // Update or insert customer locally & remotely
            val existingCustomer = database.customerDao().getCustomerByPhone(order.customerPhone)
            val customer = CustomerEntity(
                id = existingCustomer?.id ?: "cust_${System.currentTimeMillis()}",
                name = order.customerName,
                phone = order.customerPhone,
                email = existingCustomer?.email,
                address = order.customerAddress,
                governorate = order.governorate,
                totalOrders = (existingCustomer?.totalOrders ?: 0) + 1,
                totalSpend = (existingCustomer?.totalSpend ?: 0.0) + order.totalAmount,
                firstOrderDate = existingCustomer?.firstOrderDate ?: order.createdAt,
                lastOrderDate = order.createdAt
            )
            database.customerDao().insertCustomer(customer)

            try {
                if (!isPlaceholderKey) {
                    val customerDto = SupabaseCustomerDto(
                        id = customer.id,
                        name = customer.name,
                        phone = customer.phone,
                        email = customer.email,
                        address = customer.address,
                        governorate = customer.governorate,
                        totalOrders = customer.totalOrders,
                        totalSpend = customer.totalSpend,
                        firstOrderDate = customer.firstOrderDate.toString(),
                        lastOrderDate = customer.lastOrderDate.toString()
                    )
                    SupabaseClient.service.upsertCustomer(customerDto)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ProductRepositoryImpl(
    private val database: PinkyDatabase
) : ProductRepository {
    private val productDao = database.productDao()

    override fun getProductsFlow(): Flow<List<Product>> {
        return productDao.getAllProductsFlow().map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getProductById(productId: String): Product? = withContext(Dispatchers.IO) {
        productDao.getProductById(productId)?.toDomain()
    }

    override suspend fun saveProduct(product: Product): Result<Product> = withContext(Dispatchers.IO) {
        try {
            // 1. Save to Supabase (using products table via SupabaseProductAdminDto)
            val dto = SupabaseProductAdminDto(
                id = product.id,
                name = product.name,
                description = product.description,
                image = product.imageUrl,
                category = product.categoryName,
                price = product.price,
                oldPrice = product.oldPrice,
                wholesalePrice = product.wholesalePrice,
                code = product.internalCode,
                stock = product.stock,
                discount = product.discountPercent,
                featured = product.isFeatured,
                active = product.isActive,
                createdAt = product.createdAt.toString(),
                colors = product.colors,
                sizes = product.sizes,
                gallery = product.gallery
            )
            val response = SupabaseClient.service.upsertAdminProduct(dto)
            if (!response.isSuccessful) {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                return@withContext Result.failure(Exception("Supabase saveProduct failed: $errorMsg"))
            }

            // 2. Save to local Cache
            val entity = ProductEntity(
                id = product.id,
                name = product.name,
                description = product.description,
                imageUrl = product.imageUrl,
                galleryJson = product.gallery.joinToString(","),
                categoryId = product.categoryId,
                categoryName = product.categoryName,
                price = product.price,
                oldPrice = product.oldPrice,
                discountPercent = product.discountPercent,
                wholesalePrice = product.wholesalePrice,
                internalCode = product.internalCode,
                stock = product.stock,
                colorsJson = product.colors.joinToString(","),
                sizesJson = product.sizes.joinToString(","),
                isActive = product.isActive,
                isFeatured = product.isFeatured,
                createdAt = product.createdAt
            )
            productDao.insertProduct(entity)
            Result.success(product)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProduct(productId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Delete from Supabase products table
            val response = SupabaseClient.service.deleteAdminProduct("eq.$productId")
            if (!response.isSuccessful) {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                return@withContext Result.failure(Exception("Supabase deleteProduct failed: $errorMsg"))
            }

            // 2. Delete from local Cache
            productDao.deleteProduct(productId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleProductActive(productId: String, isActive: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Patch in Supabase products table
            val response = SupabaseClient.service.patchAdminProduct("eq.$productId", mapOf("active" to isActive))
            if (!response.isSuccessful) {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                return@withContext Result.failure(Exception("Supabase toggleProductActive failed: $errorMsg"))
            }

            // 2. Update local Cache
            productDao.updateActiveStatus(productId, isActive)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncProducts(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // If authenticated, we fetch from admin table products to get full admin details
            // otherwise, read-only fallback from catalog_products view.
            val isPlaceholderKey = SupabaseClient.supabaseAnonKey.contains("placeholder")
            if (isPlaceholderKey) {
                android.util.Log.d("SupabaseSync", "Using sandbox placeholder, skipping remote syncProducts.")
                return@withContext Result.success(Unit)
            }

            if (SupabaseClient.isAuthenticated) {
                val response = SupabaseClient.service.getAdminProducts()
                if (response.isSuccessful) {
                    val dtoList = response.body() ?: emptyList()
                    val entities = dtoList.map { dto ->
                        ProductEntity(
                            id = dto.id,
                            name = dto.name,
                            description = dto.description ?: "",
                            imageUrl = dto.image ?: "",
                            galleryJson = dto.gallery?.joinToString(",") ?: "",
                            categoryId = "",
                            categoryName = dto.category ?: "",
                            price = dto.price,
                            oldPrice = dto.oldPrice,
                            discountPercent = dto.discount,
                            wholesalePrice = dto.wholesalePrice,
                            internalCode = dto.code,
                            stock = dto.stock ?: 0,
                            colorsJson = dto.colors?.joinToString(",") ?: "",
                            sizesJson = dto.sizes?.joinToString(",") ?: "",
                            isActive = dto.active ?: true,
                            isFeatured = dto.featured ?: false,
                            createdAt = dto.createdAt?.toLongOrNull() ?: System.currentTimeMillis()
                        )
                    }
                    productDao.insertProducts(entities)
                    Result.success(Unit)
                } else {
                    val errorBodyStr = response.errorBody()?.string() ?: ""
                    val errMsg = "Table: products | Operation: syncProducts(Admin) | Error: HTTP ${response.code()} ${response.message()} | Payload: $errorBodyStr"
                    if (isSupabasePermissionError(response.code(), errorBodyStr)) {
                        android.util.Log.w("SupabaseSync", "Table: products | Operation: syncProducts | Status: Restricted (HTTP ${response.code()} / RLS). Using local cached data.")
                        Result.success(Unit)
                    } else {
                        android.util.Log.e("SupabaseError", errMsg)
                        Result.failure(Exception(errMsg))
                    }
                }
            } else {
                val response = SupabaseClient.service.getProducts()
                if (response.isSuccessful) {
                    val dtoList = response.body() ?: emptyList()
                    val entities = dtoList.map { dto ->
                        ProductEntity(
                            id = dto.id,
                            name = dto.name,
                            description = dto.description ?: "",
                            imageUrl = dto.image ?: "",
                            galleryJson = "",
                            categoryId = "",
                            categoryName = dto.category ?: "",
                            price = dto.price,
                            oldPrice = dto.oldPrice,
                            discountPercent = dto.discount,
                            wholesalePrice = null,
                            internalCode = null,
                            stock = 0,
                            colorsJson = "",
                            sizesJson = "",
                            isActive = dto.active ?: true,
                            isFeatured = dto.featured ?: false,
                            createdAt = dto.createdAt?.toLongOrNull() ?: System.currentTimeMillis()
                        )
                    }
                    productDao.insertProducts(entities)
                    Result.success(Unit)
                } else {
                    val errorBodyStr = response.errorBody()?.string() ?: ""
                    val errMsg = "Table: products | Operation: syncProducts(View) | Error: HTTP ${response.code()} ${response.message()} | Payload: $errorBodyStr"
                    if (isSupabasePermissionError(response.code(), errorBodyStr)) {
                        android.util.Log.w("SupabaseSync", "Table: products | Operation: syncProducts | Status: Restricted (HTTP ${response.code()} / RLS). Using local cached data.")
                        Result.success(Unit)
                    } else {
                        android.util.Log.e("SupabaseError", errMsg)
                        Result.failure(Exception(errMsg))
                    }
                }
            }
        } catch (e: Exception) {
            if (isNetworkException(e)) {
                android.util.Log.w("SupabaseSync", "syncProducts offline: ${e.message}")
            } else {
                android.util.Log.e("SupabaseError", "Table: products | Operation: syncProducts | Exception: ${e.message}", e)
            }
            Result.failure(e)
        }
    }

    private fun ProductEntity.toDomain(): Product {
        return Product(
            id = id,
            name = name,
            description = description,
            imageUrl = imageUrl,
            gallery = if (galleryJson.isBlank()) emptyList() else galleryJson.split(",").filter { it.isNotBlank() },
            categoryId = categoryId,
            categoryName = categoryName,
            price = price,
            oldPrice = oldPrice,
            discountPercent = discountPercent,
            wholesalePrice = wholesalePrice,
            internalCode = internalCode,
            stock = stock,
            colors = if (colorsJson.isBlank()) emptyList() else colorsJson.split(",").filter { it.isNotBlank() },
            sizes = if (sizesJson.isBlank()) emptyList() else sizesJson.split(",").filter { it.isNotBlank() },
            isActive = isActive,
            isFeatured = isFeatured,
            createdAt = createdAt
        )
    }
}

class CategoryRepositoryImpl(
    private val database: PinkyDatabase
) : CategoryRepository {
    private val categoryDao = database.categoryDao()

    override fun getCategoriesFlow(): Flow<List<Category>> {
        return categoryDao.getAllCategoriesFlow().map { entities ->
            entities.map { Category(id = it.id, name = it.name, imageUrl = it.imageUrl, sortOrder = it.sortOrder, isActive = it.isActive, productsCount = it.productsCount) }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun saveCategory(category: Category): Result<Category> = withContext(Dispatchers.IO) {
        try {
            // 1. Save to Supabase
            val dto = SupabaseCategoryDto(
                id = category.id,
                name = category.name,
                slug = category.slug,
                image = category.imageUrl,
                sortOrder = category.sortOrder,
                active = category.isActive
            )
            val response = SupabaseClient.service.upsertCategory(dto)
            if (!response.isSuccessful) {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                return@withContext Result.failure(Exception("Supabase saveCategory failed: $errorMsg"))
            }

            // 2. Save to local Cache
            val entity = CategoryEntity(
                id = category.id,
                name = category.name,
                imageUrl = category.imageUrl,
                sortOrder = category.sortOrder,
                isActive = category.isActive,
                productsCount = category.productsCount
            )
            categoryDao.insertCategory(entity)
            Result.success(category)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Delete from Supabase
            val response = SupabaseClient.service.deleteCategory("eq.$categoryId")
            if (!response.isSuccessful) {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                return@withContext Result.failure(Exception("Supabase deleteCategory failed: $errorMsg"))
            }

            // 2. Delete from local Cache
            categoryDao.deleteCategory(categoryId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearLocalCategories() = withContext(Dispatchers.IO) {
        categoryDao.deleteAllCategories()
    }

    private fun extractFromJson(json: String, key: String): String {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        val match = pattern.find(json)
        return match?.groupValues?.get(1) ?: "N/A"
    }

    override suspend fun syncCategories(): Result<Unit> = withContext(Dispatchers.IO) {
        val hasSession = SupabaseClient.userToken != null
        android.util.Log.i("SupabaseDiagnostic", "CATEGORIES SYNC START | Has Auth Session: $hasSession")
        try {
            val response = SupabaseClient.service.getCategories()
            android.util.Log.i("SupabaseDiagnostic", "CATEGORIES HTTP STATUS: ${response.code()}")
            
            if (response.isSuccessful) {
                val dtoList = response.body() ?: emptyList()
                android.util.Log.i("SupabaseDiagnostic", "CATEGORIES ROW COUNT: ${dtoList.size}")
                android.util.Log.i("SupabaseDiagnostic", "CATEGORIES NAMES: ${dtoList.joinToString { it.name }}")
                val entities = dtoList.map { dto ->
                    CategoryEntity(
                        id = dto.id,
                        name = dto.name,
                        imageUrl = dto.image ?: "",
                        sortOrder = dto.sortOrder ?: 0,
                        isActive = dto.active ?: true,
                        productsCount = 0
                    )
                }
                categoryDao.deleteAllCategories()
                categoryDao.insertCategories(entities)
                android.util.Log.d("SupabaseSync", "Table: categories | Operation: syncCategories | Status: Success | Count: ${entities.size}")
                Result.success(Unit)
            } else {
                val errorBodyStr = response.errorBody()?.string() ?: ""
                android.util.Log.i("SupabaseDiagnostic", "Supabase categories error: ${response.code()} | Payload: $errorBodyStr")
                val errCode = extractFromJson(errorBodyStr, "code")
                val errMsg = extractFromJson(errorBodyStr, "message")
                android.util.Log.w("SupabaseDiagnostic", "CATEGORIES SUPABASE ERROR CODE: $errCode")
                android.util.Log.w("SupabaseDiagnostic", "CATEGORIES SUPABASE ERROR MESSAGE: $errMsg")
                
                val fullErrMsg = "Table: categories | Operation: syncCategories | Error: HTTP ${response.code()} ${response.message()} | Payload: $errorBodyStr"
                android.util.Log.w("SupabaseError", fullErrMsg)
                Result.failure(Exception("خطأ في جلب الأقسام من الخادم (رمز الخطأ: $errCode): $errMsg"))
            }
        } catch (e: Exception) {
            android.util.Log.w("SupabaseDiagnostic", "CATEGORIES SUPABASE ERROR CODE: EXCEPTION")
            android.util.Log.w("SupabaseDiagnostic", "CATEGORIES SUPABASE ERROR MESSAGE: ${e.message ?: "Unknown Exception"}")
            if (isNetworkException(e)) {
                android.util.Log.w("SupabaseSync", "syncCategories offline: ${e.message}")
            } else {
                android.util.Log.w("SupabaseError", "Table: categories | Operation: syncCategories | Exception: ${e.message}", e)
            }
            Result.failure(e)
        }
    }
}

class OfferRepositoryImpl(
    private val database: PinkyDatabase
) : OfferRepository {
    private val offerDao = database.offerDao()

    override fun getOffersFlow(): Flow<List<Offer>> {
        return offerDao.getAllOffersFlow().map { entities ->
            entities.map {
                Offer(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    discountText = it.discountText,
                    imageUrl = it.imageUrl,
                    buttonText = it.buttonText,
                    targetCategoryId = it.targetCategoryId,
                    targetCategoryName = it.targetCategoryName,
                    sortOrder = it.sortOrder,
                    isActive = it.isActive,
                    isBanner = it.id.startsWith("ban_"),
                    startDate = it.startDate,
                    endDate = it.endDate
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun saveOffer(offer: Offer): Result<Offer> = withContext(Dispatchers.IO) {
        try {
            val isPlaceholderKey = SupabaseClient.supabaseAnonKey.contains("placeholder")
            if (!isPlaceholderKey) {
                if (offer.isBanner || offer.id.startsWith("ban_")) {
                    val bannerDto = SupabaseBannerDto(
                        id = offer.id,
                        title = offer.title,
                        subtitle = offer.description,
                        image = offer.imageUrl,
                        buttonText = offer.buttonText,
                        buttonAction = offer.targetCategoryId ?: offer.targetCategoryName,
                        sortOrder = offer.sortOrder,
                        active = offer.isActive,
                        createdAt = null
                    )
                    val response = SupabaseClient.service.upsertBanner(bannerDto)
                    if (!response.isSuccessful) {
                        android.util.Log.e("SupabaseError", "upsertBanner failed: ${response.errorBody()?.string()}")
                    }
                } else {
                    val dto = SupabaseOfferDto(
                        id = offer.id,
                        title = offer.title,
                        description = offer.description,
                        discount = offer.discountText,
                        image = offer.imageUrl,
                        active = offer.isActive,
                        startsAt = offer.startDate.toString(),
                        endsAt = offer.endDate.toString(),
                        createdAt = null,
                        buttonText = offer.buttonText,
                        targetCategory = offer.targetCategoryId ?: offer.targetCategoryName,
                        sortOrder = offer.sortOrder,
                        isActive = offer.isActive
                    )
                    val response = SupabaseClient.service.upsertOffer(dto)
                    if (!response.isSuccessful) {
                        android.util.Log.e("SupabaseError", "upsertOffer failed: ${response.errorBody()?.string()}")
                    }
                }
            }

            // 2. Save to local Cache
            val entity = OfferEntity(
                id = offer.id,
                title = offer.title,
                description = offer.description,
                discountText = offer.discountText,
                imageUrl = offer.imageUrl,
                buttonText = offer.buttonText,
                targetCategoryId = offer.targetCategoryId,
                targetCategoryName = offer.targetCategoryName,
                sortOrder = offer.sortOrder,
                isActive = offer.isActive,
                startDate = offer.startDate,
                endDate = offer.endDate
            )
            offerDao.insertOffer(entity)
            Result.success(offer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteOffer(offerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val isPlaceholderKey = SupabaseClient.supabaseAnonKey.contains("placeholder")
            if (!isPlaceholderKey) {
                try {
                    SupabaseClient.service.deleteOffer("eq.$offerId")
                    SupabaseClient.service.deleteBanner("eq.$offerId")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Delete from local Cache
            offerDao.deleteOffer(offerId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncOffers(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val isPlaceholderKey = SupabaseClient.supabaseAnonKey.contains("placeholder")
            if (isPlaceholderKey) {
                android.util.Log.d("SupabaseSync", "Using sandbox placeholder, skipping remote syncOffers.")
                return@withContext Result.success(Unit)
            }

            val offersResp = SupabaseClient.service.getOffers()
            val bannersResp = SupabaseClient.service.getBanners()

            val entities = mutableListOf<OfferEntity>()

            if (offersResp.isSuccessful) {
                offersResp.body()?.forEach { dto ->
                    entities.add(
                        OfferEntity(
                            id = dto.id,
                            title = dto.title,
                            description = dto.description ?: "",
                            discountText = dto.discount ?: "",
                            imageUrl = dto.image ?: "",
                            buttonText = dto.buttonText ?: "تسوقي الآن",
                            targetCategoryId = dto.targetCategory,
                            targetCategoryName = dto.targetCategory ?: "",
                            sortOrder = dto.sortOrder ?: 0,
                            isActive = dto.active ?: dto.isActive ?: true,
                            startDate = dto.startsAt?.toLongOrNull() ?: System.currentTimeMillis(),
                            endDate = dto.endsAt?.toLongOrNull() ?: (System.currentTimeMillis() + 30L*24*60*60*1000)
                        )
                    )
                }
            }

            if (bannersResp.isSuccessful) {
                bannersResp.body()?.forEach { dto ->
                    val finalId = if (dto.id.startsWith("ban_")) dto.id else "ban_${dto.id}"
                    entities.add(
                        OfferEntity(
                            id = finalId,
                            title = dto.title,
                            description = dto.subtitle ?: "",
                            discountText = "",
                            imageUrl = dto.image ?: "",
                            buttonText = dto.buttonText ?: "تسوقي الآن",
                            targetCategoryId = dto.buttonAction,
                            targetCategoryName = dto.buttonAction ?: "",
                            sortOrder = dto.sortOrder ?: 0,
                            isActive = dto.active ?: true,
                            startDate = System.currentTimeMillis(),
                            endDate = System.currentTimeMillis() + 365L*24*60*60*1000
                        )
                    )
                }
            }

            if (entities.isNotEmpty()) {
                offerDao.insertOffers(entities)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (isNetworkException(e)) {
                android.util.Log.w("SupabaseSync", "syncOffers offline: ${e.message}")
            } else {
                android.util.Log.e("SupabaseError", "Table: offers | Operation: syncOffers | Exception: ${e.message}", e)
            }
            Result.failure(e)
        }
    }
}

class ShippingRepositoryImpl(
    private val database: PinkyDatabase
) : ShippingRepository {
    private val shippingDao = database.shippingDao()

    override suspend fun clearLocalShipping() = withContext(Dispatchers.IO) {
        shippingDao.deleteAllGovernorates()
        shippingDao.deleteAllCenters()
    }

    override fun getGovernoratesFlow(): Flow<List<ShippingGovernorate>> {
        return shippingDao.getAllGovernoratesFlow().map { entities ->
            entities.map { ShippingGovernorate(it.id, it.name, it.deliveryPrice, it.estimatedDays, it.isActive) }
        }.flowOn(Dispatchers.IO)
    }

    override fun getShippingCentersFlow(): Flow<List<ShippingCenter>> {
        return shippingDao.getAllCentersFlow().map { entities ->
            entities.map { ShippingCenter(it.id, it.governorateId, it.name, it.deliveryPrice, it.contactNumber, it.isActive) }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun saveGovernorate(gov: ShippingGovernorate): Result<ShippingGovernorate> = withContext(Dispatchers.IO) {
        try {
            // 1. Save to Supabase
            val dto = SupabaseGovernorateDto(
                governorate = gov.name,
                price = gov.deliveryPrice,
                centersEnabled = true,
                active = gov.isActive
            )
            try {
                val response = SupabaseClient.service.upsertGovernorate(dto)
                if (!response.isSuccessful) {
                    println("Supabase saveGovernorate failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Save to local Cache
            val entity = ShippingGovernorateEntity(gov.id, gov.name, gov.deliveryPrice, gov.estimatedDays, gov.isActive)
            shippingDao.insertGovernorate(entity)
            Result.success(gov)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveShippingCenter(center: ShippingCenter): Result<ShippingCenter> = withContext(Dispatchers.IO) {
        try {
            // 1. Save to Supabase
            val dto = SupabaseShippingCenterDto(
                id = center.id,
                governorate = center.governorateId,
                center = center.name,
                price = center.deliveryPrice,
                active = center.isActive
            )
            try {
                val response = SupabaseClient.service.upsertShippingCenter(dto)
                if (!response.isSuccessful) {
                    println("Supabase saveShippingCenter failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Save to local Cache
            val entity = ShippingCenterEntity(center.id, center.governorateId, center.name, center.deliveryPrice, center.contactNumber, center.isActive)
            shippingDao.insertCenter(entity)
            Result.success(center)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteShippingCenter(centerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Delete from Supabase
            try {
                val response = SupabaseClient.service.deleteShippingCenter("eq.$centerId")
                if (!response.isSuccessful) {
                    println("Supabase deleteShippingCenter failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Delete from local Cache
            shippingDao.deleteCenter(centerId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncShipping(): Result<Unit> = withContext(Dispatchers.IO) {
        android.util.Log.i("SupabaseDiagnostic", "Supabase shipping request started")
        try {
            val govResponse = SupabaseClient.service.getGovernorates()
            android.util.Log.i("SupabaseDiagnostic", "Supabase governorates HTTP status: ${govResponse.code()}")
            
            val govResult = if (govResponse.isSuccessful) {
                val govs = govResponse.body() ?: emptyList()
                android.util.Log.i("SupabaseDiagnostic", "Supabase governorates rows returned: ${govs.size}")
                val entities = govs.map { dto ->
                    ShippingGovernorateEntity(
                        id = dto.governorate,
                        name = dto.governorate,
                        deliveryPrice = dto.price,
                        estimatedDays = "1-3 أيام",
                        isActive = dto.active ?: true
                    )
                }
                shippingDao.deleteAllGovernorates()
                shippingDao.insertGovernorates(entities)
                android.util.Log.d("SupabaseSync", "Table: shipping_governorates | Operation: syncShipping | Status: Success | Count: ${entities.size}")
                Result.success(govs.size)
            } else {
                val errorBodyStr = govResponse.errorBody()?.string() ?: ""
                val errMsg = "Table: shipping_governorates | Operation: syncShipping | Error: HTTP ${govResponse.code()} | Payload: $errorBodyStr"
                if (isSupabasePermissionError(govResponse.code(), errorBodyStr)) {
                    android.util.Log.w("SupabaseDiagnostic", "Supabase governorates HTTP warning: ${govResponse.code()} | Payload: $errorBodyStr")
                    android.util.Log.w("SupabaseSync", "Table: shipping_governorates | Status: Restricted (HTTP ${govResponse.code()} / RLS). Using local cached data.")
                    Result.success(0)
                } else {
                    android.util.Log.e("SupabaseDiagnostic", "Supabase governorates HTTP error: ${govResponse.code()} | Payload: $errorBodyStr")
                    android.util.Log.e("SupabaseError", errMsg)
                    Result.failure<Int>(Exception(errMsg))
                }
            }

            val centerResponse = SupabaseClient.service.getShippingCenters()
            android.util.Log.i("SupabaseDiagnostic", "Supabase centers HTTP status: ${centerResponse.code()}")

            val centerResult = if (centerResponse.isSuccessful) {
                val centers = centerResponse.body() ?: emptyList()
                android.util.Log.i("SupabaseDiagnostic", "Supabase centers rows returned: ${centers.size}")
                val entities = centers.map { dto ->
                    ShippingCenterEntity(
                        id = dto.id,
                        governorateId = dto.governorate,
                        name = dto.center,
                        deliveryPrice = dto.price,
                        contactNumber = null,
                        isActive = dto.active ?: true
                    )
                }
                shippingDao.deleteAllCenters()
                shippingDao.insertCenters(entities)
                android.util.Log.d("SupabaseSync", "Table: shipping_centers | Operation: syncShipping | Status: Success | Count: ${entities.size}")
                Result.success(centers.size)
            } else {
                val errorBodyStr = centerResponse.errorBody()?.string() ?: ""
                val errMsg = "Table: shipping_centers | Operation: syncShipping | Error: HTTP ${centerResponse.code()} | Payload: $errorBodyStr"
                if (isSupabasePermissionError(centerResponse.code(), errorBodyStr)) {
                    android.util.Log.w("SupabaseDiagnostic", "Supabase centers HTTP warning: ${centerResponse.code()} | Payload: $errorBodyStr")
                    android.util.Log.w("SupabaseSync", "Table: shipping_centers | Status: Restricted (HTTP ${centerResponse.code()} / RLS). Using local cached data.")
                    Result.success(0)
                } else {
                    android.util.Log.e("SupabaseDiagnostic", "Supabase centers HTTP error: ${centerResponse.code()} | Payload: $errorBodyStr")
                    android.util.Log.e("SupabaseError", errMsg)
                    Result.failure<Int>(Exception(errMsg))
                }
            }

            if (govResult.isSuccess && centerResult.isSuccess) {
                Result.success(Unit)
            } else {
                val govErr = govResult.exceptionOrNull()?.message ?: ""
                val centerErr = centerResult.exceptionOrNull()?.message ?: ""
                Result.failure(Exception("$govErr | $centerErr"))
            }
        } catch (e: Exception) {
            if (isNetworkException(e)) {
                android.util.Log.w("SupabaseDiagnostic", "Supabase shipping network warning (offline): ${e.message}")
                android.util.Log.w("SupabaseSync", "syncShipping offline: ${e.message}")
            } else {
                android.util.Log.e("SupabaseDiagnostic", "Supabase shipping exception: ${e.message}", e)
                android.util.Log.e("SupabaseSync", "syncShipping exception: ${e.message}", e)
            }
            Result.failure(e)
        }
    }
}

class CustomerRepositoryImpl(
    private val database: PinkyDatabase
) : CustomerRepository {
    private val customerDao = database.customerDao()
    private val orderDao = database.orderDao()

    override fun getCustomersFlow(): Flow<List<Customer>> {
        return customerDao.getAllCustomersFlow().map { entities ->
            entities.map {
                Customer(
                    id = it.id,
                    name = it.name,
                    phone = it.phone,
                    email = it.email,
                    address = it.address,
                    governorate = it.governorate,
                    totalOrders = it.totalOrders,
                    totalSpend = it.totalSpend,
                    firstOrderDate = it.firstOrderDate,
                    lastOrderDate = it.lastOrderDate
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getCustomerById(customerId: String): Customer? = withContext(Dispatchers.IO) {
        val it = customerDao.getCustomerById(customerId) ?: return@withContext null
        Customer(it.id, it.name, it.phone, it.email, it.address, it.governorate, it.totalOrders, it.totalSpend, it.firstOrderDate, it.lastOrderDate)
    }

    override suspend fun getCustomerOrders(customerPhone: String): List<Order> = withContext(Dispatchers.IO) {
        orderDao.getOrdersByCustomerPhone(customerPhone).map { entity ->
            val items = orderDao.getOrderItems(entity.id).map { item ->
                OrderItem(item.id, item.productId, item.productName, item.variantName, item.color, item.size, item.quantity, item.unitPrice, item.lineTotal, item.imageUrl)
            }
            Order(
                id = entity.id,
                orderNumber = entity.orderNumber,
                customerName = entity.customerName,
                customerPhone = entity.customerPhone,
                customerAddress = entity.customerAddress,
                governorate = entity.governorate,
                shippingCenter = entity.shippingCenter,
                items = items,
                subtotalAmount = entity.subtotalAmount,
                shippingFee = entity.shippingFee,
                discountAmount = entity.discountAmount,
                totalAmount = entity.totalAmount,
                paymentMethod = PaymentMethod.fromString(entity.paymentMethod),
                paymentStatus = PaymentStatus.valueOf(entity.paymentStatus),
                status = OrderStatus.fromString(entity.status),
                createdAt = entity.createdAt,
                notes = entity.notes
            )
        }
    }

    override suspend fun syncCustomers(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = try {
                SupabaseClient.service.getCustomers()
            } catch (e: Exception) {
                null
            }
            if (response != null && response.isSuccessful) {
                val dtoList = response.body() ?: emptyList()
                val entities = dtoList.map { dto ->
                    CustomerEntity(
                        id = dto.id,
                        name = dto.name,
                        phone = dto.phone,
                        email = dto.email,
                        address = dto.address ?: "",
                        governorate = dto.governorate ?: "",
                        totalOrders = dto.totalOrders ?: 0,
                        totalSpend = dto.totalSpend ?: 0.0,
                        firstOrderDate = dto.firstOrderDate?.toLongOrNull() ?: System.currentTimeMillis(),
                        lastOrderDate = dto.lastOrderDate?.toLongOrNull() ?: System.currentTimeMillis()
                    )
                }
                customerDao.insertCustomers(entities)
                android.util.Log.d("SupabaseSync", "Table: customers | Operation: syncCustomers | Status: Success | Count: ${entities.size}")
                Result.success(Unit)
            } else {
                deriveCustomersFromLocalOrders()
            }
        } catch (e: Exception) {
            deriveCustomersFromLocalOrders()
        }
    }

    private suspend fun deriveCustomersFromLocalOrders(): Result<Unit> {
        return try {
            val ordersList = orderDao.getAllOrdersFlow().first()
            val derived = ordersList.groupBy { it.customerPhone }.map { (phone, ordersForCustomer) ->
                val firstOrder = ordersForCustomer.minByOrNull { it.createdAt }
                val lastOrder = ordersForCustomer.maxByOrNull { it.createdAt }
                val totalSpend = ordersForCustomer.sumOf { it.totalAmount }
                CustomerEntity(
                    id = "cust_${phone}",
                    name = firstOrder?.customerName ?: "عميل غير معروف",
                    phone = phone,
                    email = "",
                    address = firstOrder?.customerAddress ?: "",
                    governorate = firstOrder?.governorate ?: "",
                    totalOrders = ordersForCustomer.size,
                    totalSpend = totalSpend,
                    firstOrderDate = firstOrder?.createdAt ?: System.currentTimeMillis(),
                    lastOrderDate = lastOrder?.createdAt ?: System.currentTimeMillis()
                )
            }
            if (derived.isNotEmpty()) {
                customerDao.insertCustomers(derived)
            }
            android.util.Log.d("SupabaseSync", "Table: customers (derived) | Operation: syncCustomers | Status: Success | Count: ${derived.size}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseError", "Table: customers (derived) | Operation: syncCustomers | Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}

class AuthRepositoryImpl(private val context: android.content.Context) : AuthRepository {
    private val prefs = context.getSharedPreferences("pinky_auth_prefs", android.content.Context.MODE_PRIVATE)
    private val _currentUser = MutableStateFlow<AdminUser?>(null)

    init {
        // We explicitly wait for the ViewModel to trigger initializeAuthSession synchronously
        // before launching any network or synchronization requests to eliminate race conditions.
        SupabaseClient.isSessionReady = false
    }

    override fun getCurrentUserFlow(): Flow<AdminUser?> = _currentUser.asStateFlow()

    override suspend fun initializeAuthSession(): Boolean = withContext(Dispatchers.IO) {
        android.util.Log.i("SupabaseAuth", "AUTH_INIT_STARTED")
        val isPlaceholderKey = SupabaseClient.supabaseAnonKey.contains("placeholder")
        if (isPlaceholderKey) {
            SupabaseClient.isSessionReady = true
            SupabaseClient.isAuthenticated = true
            val roleStr = prefs.getString("user_role", UserRole.OWNER.name) ?: UserRole.OWNER.name
            val role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.OWNER }
            val email = prefs.getString("user_email", "ilnemrawy@gmail.com") ?: "ilnemrawy@gmail.com"
            val user = AdminUser(
                id = "sandbox_user_id",
                name = if (role == UserRole.OWNER) "إسلام النمراوي (Sandbox)" else "مسؤول تجريبي (Sandbox)",
                email = email,
                role = role,
                permissions = Permission.defaultPermissionsFor(role)
            )
            _currentUser.value = user
            android.util.Log.i("SupabaseAuth", "AUTH_SESSION_RESTORED: user_id=sandbox_user_id (Sandbox)")
            return@withContext true
        }

        val token = prefs.getString("user_token", null)
        val refreshToken = prefs.getString("refresh_token", null)
        val expiresAt = prefs.getLong("expires_at", 0L)
        val id = prefs.getString("user_id", null)
        val name = prefs.getString("user_name", null)
        val email = prefs.getString("user_email", null)
        val roleStr = prefs.getString("user_role", null)

        if (token != null && id != null && name != null && email != null && roleStr != null) {
            val role = try {
                UserRole.valueOf(roleStr)
            } catch (e: Exception) {
                UserRole.ADMIN
            }

            // Sync state to client
            SupabaseClient.accessToken = token
            SupabaseClient.refreshToken = refreshToken
            SupabaseClient.expiresAt = expiresAt
            SupabaseClient.userId = id
            SupabaseClient.isAuthenticated = true
            SupabaseClient.isSessionReady = true

            val user = AdminUser(
                id = id,
                name = name,
                email = email,
                role = role,
                permissions = Permission.defaultPermissionsFor(role)
            )
            _currentUser.value = user
            android.util.Log.i("SupabaseAuth", "AUTH_SESSION_RESTORED: user_id=$id")
            android.util.Log.i("SupabaseAuth", "AUTH_USER_ID=$id")
            android.util.Log.i("SupabaseAuth", "AUTHENTICATED=true")

            // Auto-refresh token if it's near expiry right now
            refreshAuthSessionIfNeeded()

            return@withContext true
        } else {
            // Check if OWNER_PASSWORD is provided and try silent auto-login
            val ownerPass = try { com.pinky.dashboard.BuildConfig.OWNER_PASSWORD } catch (e: Throwable) { "" }
            if (ownerPass.isNotBlank() && !ownerPass.contains("placeholder")) {
                val candidates = listOf("ilnemrawy@gmail.com", "ilnemrawi@gmail.com", "islamilnemrawi222@gmail.com")
                var success = false
                var lastError = ""
                for (email in candidates) {
                    android.util.Log.i("SupabaseAuth", "Attempting silent auto-login for $email")
                    val result = login(email, ownerPass)
                    if (result.isSuccess) {
                        android.util.Log.i("SupabaseAuth", "Silent auto-login successful for $email")
                        success = true
                        break
                    } else {
                        lastError = result.exceptionOrNull()?.message ?: ""
                    }
                }
                if (success) {
                    return@withContext true
                } else {
                    if (lastError.contains("UnknownHostException") || lastError.contains("تعذر الاتصال") || lastError.contains("Unable to resolve host") || lastError.contains("Connection")) {
                        android.util.Log.w("SupabaseAuth", "Silent auto-login offline or network warning: $lastError")
                    } else {
                        android.util.Log.e("SupabaseAuth", "Silent auto-login failed: $lastError")
                    }
                }
            }
        }

        SupabaseClient.isSessionReady = true
        SupabaseClient.isAuthenticated = false
        android.util.Log.i("SupabaseAuth", "AUTHENTICATED=false")
        return@withContext false
    }

    override suspend fun refreshAuthSessionIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        val result = SupabaseClient.performTokenRefresh()
        if (result) {
            val token = SupabaseClient.accessToken
            val expiresAt = SupabaseClient.expiresAt
            val id = SupabaseClient.userId
            if (token != null && id != null) {
                // Keep repository user model up to date in case credentials refresh
                val current = _currentUser.value
                if (current != null) {
                    _currentUser.value = current.copy(id = id)
                }
            }
        }
        return@withContext result
    }

    override suspend fun login(email: String, pass: String): Result<AdminUser> = withContext(Dispatchers.IO) {
        val url = SupabaseClient.supabaseUrl
        val key = SupabaseClient.supabaseAnonKey
        val isConfigMissing = url.isBlank() || key.isBlank() || url.contains("placeholder") || key.contains("placeholder")

        if (isConfigMissing) {
            return@withContext Result.failure(Exception("[Missing Configuration] إعدادات السحابة غير مكتملة أو تحتوي على قيم تجريبية (Placeholder). يرجى تكوين المتغيرات بشكل صحيح."))
        }

        try {
            val response = SupabaseClient.service.login(SupabaseLoginRequest(email, pass))
            if (response.isSuccessful) {
                val loginData = response.body()
                if (loginData != null) {
                    val computedExpiresAt = System.currentTimeMillis() + (loginData.expiresIn * 1000L)

                    // 1. Store the session centrally in SupabaseClient
                    SupabaseClient.accessToken = loginData.accessToken
                    SupabaseClient.refreshToken = loginData.refreshToken
                    SupabaseClient.userId = loginData.user.id
                    SupabaseClient.expiresAt = computedExpiresAt
                    SupabaseClient.isAuthenticated = true
                    SupabaseClient.isSessionReady = true

                    // 2. Fetch the staff profile matching this email
                    var staffName = "مسؤول بينكي"
                    var staffRole = UserRole.ADMIN
                    
                    if (email.equals("ilnemrawi@gmail.com", ignoreCase = true) || email.equals("ilnemrawy@gmail.com", ignoreCase = true) || email.equals("islamilnemrawi222@gmail.com", ignoreCase = true)) {
                        staffRole = UserRole.OWNER
                        staffName = "إسلام النمراوي (المالك)"
                    }

                    var dbPermissions = Permission.defaultPermissionsFor(staffRole)
                    try {
                        val profileResponse = SupabaseClient.service.getStaffProfileByEmail("eq.$email")
                        if (profileResponse.isSuccessful) {
                            val profiles = profileResponse.body()
                            if (!profiles.isNullOrEmpty()) {
                                val profile = profiles[0]
                                staffName = profile.name ?: email.substringBefore("@")
                                staffRole = when ((profile.role ?: "employee").lowercase()) {
                                    "owner" -> UserRole.OWNER
                                    "employee" -> UserRole.EMPLOYEE
                                    else -> UserRole.ADMIN
                                }
                                profile.permissions?.let { permMap ->
                                    val parsed = permMap.filterValues { it }.keys.mapNotNull {
                                        try { Permission.valueOf(it) } catch (e: Exception) { null }
                                    }.toSet()
                                    if (parsed.isNotEmpty()) {
                                        dbPermissions = parsed
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SupabaseAuth", "Failed to fetch staff profile: ${e.message}")
                    }

                    val user = AdminUser(
                        id = loginData.user.id,
                        name = staffName,
                        email = email,
                        role = staffRole,
                        permissions = dbPermissions
                    )
                    
                    // Persist session to SharedPreferences
                    prefs.edit()
                        .putString("user_token", loginData.accessToken)
                        .putString("refresh_token", loginData.refreshToken)
                        .putLong("expires_at", computedExpiresAt)
                        .putString("user_id", user.id)
                        .putString("user_name", user.name)
                        .putString("user_email", user.email)
                        .putString("user_role", user.role.name)
                        .apply()

                    _currentUser.value = user
                    android.util.Log.i("SupabaseAuth", "AUTH_USER_ID=${user.id}")
                    android.util.Log.i("SupabaseAuth", "AUTHENTICATED=true")
                    Result.success(user)
                } else {
                    Result.failure(Exception("[Supabase/API Failure] استجابة الخادم فارغة."))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: ""
                val isInvalidCredentials = errorMsg.contains("invalid_grant") || errorMsg.contains("Invalid login credentials") || errorMsg.contains("invalid_credentials")
                if (isInvalidCredentials) {
                    android.util.Log.w("SupabaseAuth", "Login warning (invalid credentials): $errorMsg")
                    Result.failure(Exception("[Invalid Credentials] اسم المستخدم أو كلمة المرور غير صحيحة."))
                } else {
                    android.util.Log.e("SupabaseAuth", "Login failed: $errorMsg")
                    Result.failure(Exception("[Supabase/API Failure] خطأ من خادم Supabase (كود ${response.code()}): $errorMsg"))
                }
            }
        } catch (e: Exception) {
            if (e is java.net.UnknownHostException || e.message?.contains("Unable to resolve host") == true) {
                android.util.Log.w("SupabaseAuth", "Network connectivity warning: ${e.message}")
                Result.failure(Exception("[Network/Connection Failure] تعذر الاتصال بالسحابة (خطأ في حل عنوان الخادم أو انقطاع الإنترنت). يرجى التأكد من اتصالك بالإنترنت وصحة الـ DNS للعنوان: $url"))
            } else if (e is java.io.IOException) {
                Result.failure(Exception("[Network/Connection Failure] فشل في اتصال الشبكة بـ ($url): ${e.localizedMessage}"))
            } else {
                android.util.Log.e("SupabaseAuth", "Login exception: ${e.message}")
                Result.failure(Exception("[Supabase/API Failure] حدث خطأ غير متوقع أثناء محاولة الدخول: ${e.localizedMessage}"))
            }
        }
    }

    override suspend fun logout() {
        SupabaseClient.clearSession()
        _currentUser.value = null
        prefs.edit().clear().apply()
        android.util.Log.i("SupabaseAuth", "AUTHENTICATED=false")
    }

    override suspend fun switchRoleForTesting(role: UserRole) {
        val current = _currentUser.value ?: AdminUser(
            id = "admin_test",
            name = "مسؤول بينكي",
            email = "admin@pinky.eg",
            role = role,
            permissions = Permission.defaultPermissionsFor(role)
        )
        _currentUser.value = current.copy(
            role = role,
            permissions = Permission.defaultPermissionsFor(role)
        )
    }
}

class AnalyticsRepositoryImpl(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val authRepository: AuthRepository
) : AnalyticsRepository {

    override fun getAnalyticsFlow(timeframeDays: Int): Flow<AnalyticsData> {
        return combine(
            orderRepository.getOrdersFlow(),
            productRepository.getProductsFlow(),
            authRepository.getCurrentUserFlow()
        ) { orders, products, currentUser ->
            val cutoff = System.currentTimeMillis() - (timeframeDays.toLong() * 24 * 60 * 60 * 1000)
            val filteredOrders = orders.filter { it.createdAt >= cutoff && it.status != OrderStatus.CANCELLED }

            val totalSales = filteredOrders.sumOf { it.totalAmount }
            val totalOrders = filteredOrders.size
            val totalProductsSold = filteredOrders.sumOf { o -> o.items.sumOf { it.quantity } }

            // Calculate profit only if current user has permission!
            val canViewProfit = currentUser?.canViewProfit == true
            val totalProfit = if (canViewProfit) {
                // Real profit: (Price - WholesalePrice) * qty
                val wholesaleMap = products.associate { it.id to (it.wholesalePrice ?: (it.price * 0.7)) }
                filteredOrders.sumOf { o ->
                    o.items.sumOf { item ->
                        val cost = wholesaleMap[item.productId] ?: (item.unitPrice * 0.7)
                        (item.unitPrice - cost) * item.quantity
                    }
                }
            } else {
                null
            }

            // Group daily metrics
            val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
            val dailyGrouped = filteredOrders.groupBy {
                dateFormat.format(Date(it.createdAt))
            }
            val dailyMetrics = dailyGrouped.map { (day, dayOrders) ->
                DailyMetric(
                    dayLabel = day,
                    salesAmount = dayOrders.sumOf { it.totalAmount },
                    ordersCount = dayOrders.size
                )
            }.takeLast(7)

            // Top products
            val productSalesMap = mutableMapOf<String, Pair<Int, Double>>()
            for (order in filteredOrders) {
                for (item in order.items) {
                    val current = productSalesMap[item.productId] ?: (0 to 0.0)
                    productSalesMap[item.productId] = (current.first + item.quantity) to (current.second + item.lineTotal)
                }
            }
            val topProducts = productSalesMap.entries.sortedByDescending { it.value.second }.take(5).map { entry ->
                val p = products.firstOrNull { it.id == entry.key }
                TopProductMetric(
                    productId = entry.key,
                    productName = p?.name ?: "منتج متجر بينكي",
                    unitsSold = entry.value.first,
                    totalRevenue = entry.value.second,
                    imageUrl = p?.imageUrl
                )
            }

            // Orders by status
            val statusMap = orders.groupBy { it.status }.mapValues { it.value.size }

            // Sales by governorate
            val govMap = filteredOrders.groupBy { it.governorate }.mapValues { it.value.sumOf { o -> o.totalAmount } }

            AnalyticsData(
                totalSales = totalSales,
                totalOrders = totalOrders,
                totalProductsSold = totalProductsSold,
                totalProfit = totalProfit,
                dailyMetrics = dailyMetrics,
                topSellingProducts = topProducts,
                ordersByStatus = statusMap,
                salesByGovernorate = govMap
            )
        }
    }
}
