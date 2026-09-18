package com.pinky.dashboard.domain.model

data class ProductVariant(
    val id: String,
    val name: String,
    val color: String? = null,
    val size: String? = null,
    val additionalPrice: Double = 0.0,
    val stock: Int = 0,
    val sku: String? = null
)

data class Product(
    val id: String,
    val name: String,
    val description: String = "",
    val imageUrl: String = "",
    val gallery: List<String> = emptyList(),
    val categoryId: String = "",
    val categoryName: String = "",
    val price: Double,
    val oldPrice: Double? = null,
    val discountPercent: Int? = null,
    val wholesalePrice: Double? = null, // Protected: visible only with VIEW_WHOLESALE_PRICES
    val internalCode: String? = null,   // Protected: visible only with VIEW_INTERNAL_CODES
    val stock: Int = 0,
    val colors: List<String> = emptyList(),
    val sizes: List<String> = emptyList(),
    val variants: List<ProductVariant> = emptyList(),
    val isActive: Boolean = true,
    val isFeatured: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
