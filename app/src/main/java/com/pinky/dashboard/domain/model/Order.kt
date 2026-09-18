package com.pinky.dashboard.domain.model

enum class OrderStatus(val arabicLabel: String) {
    NEW("جديد"),
    CONFIRMING("جاري التأكيد"),
    PREPARING("جاري التجهيز"),
    SHIPPED("تم الشحن"),
    DELIVERED("تم التسليم"),
    CANCELLED("ملغي");

    companion object {
        fun fromString(value: String): OrderStatus {
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || it.arabicLabel == value 
            } ?: NEW
        }
    }
}

enum class PaymentMethod(val arabicLabel: String) {
    COD("الدفع عند الاستلام"),
    INSTAPAY("إنستاباي (InstaPay)"),
    VODAFONE_CASH("فودافون كاش / محافظ إلكترونية"),
    CREDIT_CARD("بطاقة بنكية / فيزا"),
    INSTAPAY_WALLET("إنستاباي / محفظة إلكترونية"),
    ONLINE_CARD("دفع إلكتروني");

    companion object {
        fun fromString(value: String): PaymentMethod {
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || it.arabicLabel == value 
            } ?: COD
        }
    }
}

enum class PaymentStatus(val arabicLabel: String) {
    PENDING("معلق"),
    PAID("مدفوع"),
    REFUNDED("مسترجع"),
    FAILED("فشل الدفع")
}

data class OrderItem(
    val id: String,
    val productId: String,
    val productName: String,
    val variantName: String? = null,
    val color: String? = null,
    val size: String? = null,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double = unitPrice * quantity,
    val imageUrl: String? = null
)

data class OrderStatusHistory(
    val status: OrderStatus,
    val timestamp: Long,
    val note: String? = null,
    val updatedBy: String? = null
)

data class Order(
    val id: String,
    val orderNumber: String,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val governorate: String,
    val shippingCenter: String = "المركز الرئيسي",
    val items: List<OrderItem>,
    val subtotalAmount: Double,
    val shippingFee: Double,
    val discountAmount: Double = 0.0,
    val totalAmount: Double = subtotalAmount + shippingFee - discountAmount,
    val paymentMethod: PaymentMethod = PaymentMethod.COD,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val status: OrderStatus = OrderStatus.NEW,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = "",
    val statusHistory: List<OrderStatusHistory> = emptyList()
)
