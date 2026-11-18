package com.angadi.app.domain.model

enum class OrderStatus(val label: String) {
    PLACED("Order placed"),
    CONFIRMED("Order confirmed"),
    PREPARING("Preparing order"),
    PACKED("Packed"),
    OUT_FOR_DELIVERY("Out for delivery"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    /** Orders can be cancelled until they are packed. */
    val canCancel: Boolean get() = this == PLACED || this == CONFIRMED || this == PREPARING
    val isFinished: Boolean get() = this == DELIVERED || this == CANCELLED

    fun next(): OrderStatus? = when (this) {
        PLACED -> CONFIRMED
        CONFIRMED -> PREPARING
        PREPARING -> PACKED
        PACKED -> OUT_FOR_DELIVERY
        OUT_FOR_DELIVERY -> DELIVERED
        DELIVERED, CANCELLED -> null
    }

    companion object {
        /** The ordered steps shown in the progress timeline. */
        val timeline = listOf(PLACED, CONFIRMED, PREPARING, PACKED, OUT_FOR_DELIVERY, DELIVERED)
        fun fromName(name: String): OrderStatus = entries.firstOrNull { it.name == name } ?: PLACED
    }
}

enum class PaymentMethod(val label: String, val detail: String) {
    CASH_ON_DELIVERY("Cash on delivery", "Pay when your order arrives"),
    UPI_ON_DELIVERY("UPI on delivery", "Pay with your UPI app at the door");

    companion object {
        fun fromName(name: String): PaymentMethod = entries.firstOrNull { it.name == name } ?: CASH_ON_DELIVERY
    }
}

/** Snapshot of a purchased product: later catalogue changes never alter it. */
data class OrderItem(
    val id: Long = 0,
    val productId: Int,
    val title: String,
    val unit: String,
    val unitPrice: Double,
    val quantity: Int,
    val thumbnail: String = "",
    val emoji: String = "🛍️",
    val category: String = "",
) {
    val lineTotal: Double get() = unitPrice * quantity
}

data class Order(
    val id: Long,
    val createdAt: Long,
    val status: OrderStatus,
    val statusUpdatedAt: Long,
    val subtotal: Double,
    val deliveryFee: Double,
    val discount: Double,
    val total: Double,
    val couponCode: String?,
    val itemCount: Int,
    val paymentMethod: PaymentMethod,
    val recipientName: String,
    val phone: String,
    val addressLabel: String,
    val addressText: String,
    val slotLabel: String,
    val slotStart: Long,
    val items: List<OrderItem> = emptyList(),
)
