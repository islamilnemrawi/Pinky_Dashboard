package com.pinky.dashboard.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orders",
    indices = [
        Index(value = ["orderNumber"], unique = true),
        Index(value = ["customerPhone"]),
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class OrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: String,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val governorate: String,
    val shippingCenter: String,
    val subtotalAmount: Double,
    val shippingFee: Double,
    val discountAmount: Double,
    val totalAmount: Double,
    val paymentMethod: String,
    val paymentStatus: String,
    val status: String,
    val createdAt: Long,
    val notes: String,
    val isSynced: Boolean = true
)

@Entity(
    tableName = "order_items",
    indices = [
        Index(value = ["orderId"]),
        Index(value = ["productId"])
    ]
)
data class OrderItemEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val productId: String,
    val productName: String,
    val variantName: String?,
    val color: String?,
    val size: String?,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double,
    val imageUrl: String?
)

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["isActive"]),
        Index(value = ["internalCode"]),
        Index(value = ["name"])
    ]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val galleryJson: String, // Comma or JSON separated
    val categoryId: String,
    val categoryName: String,
    val price: Double,
    val oldPrice: Double?,
    val discountPercent: Int?,
    val wholesalePrice: Double?,
    val internalCode: String?,
    val stock: Int,
    val colorsJson: String,
    val sizesJson: String,
    val isActive: Boolean,
    val isFeatured: Boolean,
    val createdAt: Long,
    val isSynced: Boolean = true
)

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["sortOrder"]),
        Index(value = ["isActive"])
    ]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String,
    val sortOrder: Int,
    val isActive: Boolean,
    val productsCount: Int
)

@Entity(
    tableName = "offers",
    indices = [
        Index(value = ["sortOrder"]),
        Index(value = ["isActive"]),
        Index(value = ["startDate"]),
        Index(value = ["endDate"])
    ]
)
data class OfferEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val discountText: String,
    val imageUrl: String,
    val buttonText: String,
    val targetCategoryId: String?,
    val targetCategoryName: String?,
    val sortOrder: Int,
    val isActive: Boolean,
    val startDate: Long,
    val endDate: Long
)

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["phone"], unique = true),
        Index(value = ["name"])
    ]
)
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val email: String?,
    val address: String,
    val governorate: String,
    val totalOrders: Int,
    val totalSpend: Double,
    val firstOrderDate: Long,
    val lastOrderDate: Long
)

@Entity(
    tableName = "shipping_governorates",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class ShippingGovernorateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val deliveryPrice: Double,
    val estimatedDays: String,
    val isActive: Boolean
)

@Entity(
    tableName = "shipping_centers",
    indices = [
        Index(value = ["governorateId"])
    ]
)
data class ShippingCenterEntity(
    @PrimaryKey val id: String,
    val governorateId: String,
    val name: String,
    val deliveryPrice: Double,
    val contactNumber: String?,
    val isActive: Boolean
)
