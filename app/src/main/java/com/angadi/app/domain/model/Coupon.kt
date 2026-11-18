package com.angadi.app.domain.model

enum class DiscountType { PERCENT, FIXED }

data class Coupon(
    val code: String,
    val title: String,
    val description: String,
    val type: DiscountType,
    /** Percent (0-100) for [DiscountType.PERCENT] or rupees for [DiscountType.FIXED]. */
    val value: Double,
    val minOrderValue: Double = 0.0,
    val maxDiscount: Double? = null,
    /** Epoch millis after which the coupon can no longer be applied; null = never expires. */
    val expiresAt: Long? = null,
    val oneTimeUse: Boolean = false,
)

/** Coupons bundled with the app. A merchant backend would normally serve these. */
object CouponCatalog {
    const val END_OF_2027 = 1_830_297_599_000L   // 2027-12-31T23:59:59Z
    const val OCT_31_2026 = 1_793_491_199_000L   // 2026-10-31T23:59:59Z (already expired; exercises validation)

    val all = listOf(
        Coupon("WELCOME50", "₹50 off your first basket", "Flat ₹50 off on orders of ₹299 or more. One use per device.", DiscountType.FIXED, 50.0, minOrderValue = 299.0, expiresAt = END_OF_2027, oneTimeUse = true),
        Coupon("FRESH10", "10% off", "10% off up to ₹100 on orders of ₹499 or more.", DiscountType.PERCENT, 10.0, minOrderValue = 499.0, maxDiscount = 100.0, expiresAt = END_OF_2027),
        Coupon("BIG150", "₹150 off big baskets", "Flat ₹150 off on orders of ₹1,499 or more.", DiscountType.FIXED, 150.0, minOrderValue = 1499.0, expiresAt = END_OF_2027),
        Coupon("FESTIVE20", "Festive 20% off", "20% off up to ₹200 on orders of ₹999 or more.", DiscountType.PERCENT, 20.0, minOrderValue = 999.0, maxDiscount = 200.0, expiresAt = OCT_31_2026),
    )

    fun find(code: String): Coupon? = all.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
}
