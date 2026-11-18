package com.angadi.app.domain.logic

import com.angadi.app.domain.model.Coupon
import com.angadi.app.domain.model.DiscountType
import com.angadi.app.domain.model.Product
import kotlin.math.roundToInt

data class CartLine(val product: Product, val quantity: Int) {
    val lineTotal: Double get() = product.price * quantity
}

data class PriceBreakdown(
    val itemCount: Int,
    val subtotal: Double,
    val deliveryFee: Double,
    val discount: Double,
    val total: Double,
    val couponCode: String? = null,
    val couponError: String? = null,
) {
    val savingsText: String? get() = if (discount > 0) "You saved ₹${discount.toInt()}" else null
}

sealed interface CouponCheck {
    data class Valid(val coupon: Coupon, val discount: Double) : CouponCheck
    data class Invalid(val reason: String) : CouponCheck
}

/**
 * Single source of truth for basket maths. The order placement path recalculates everything
 * with this object from persisted cart rows, so UI-supplied totals are never trusted.
 */
object PricingEngine {
    const val FREE_DELIVERY_THRESHOLD = 699.0
    const val DELIVERY_FEE = 40.0

    /** Delivery is judged on the pre-discount subtotal so a coupon can't make a basket lose free delivery. */
    fun deliveryFee(subtotal: Double): Double = if (subtotal <= 0.0 || subtotal >= FREE_DELIVERY_THRESHOLD) 0.0 else DELIVERY_FEE

    fun subtotal(lines: List<CartLine>): Double = lines.sumOf { it.lineTotal }

    fun validateCoupon(coupon: Coupon, subtotal: Double, nowMillis: Long, usedCodes: Set<String>): CouponCheck {
        if (coupon.expiresAt != null && nowMillis > coupon.expiresAt) return CouponCheck.Invalid("${coupon.code} has expired.")
        if (coupon.oneTimeUse && usedCodes.any { it.equals(coupon.code, ignoreCase = true) }) {
            return CouponCheck.Invalid("${coupon.code} has already been used on this device.")
        }
        if (subtotal < coupon.minOrderValue) {
            val missing = (coupon.minOrderValue - subtotal).roundToInt()
            return CouponCheck.Invalid("Add ₹$missing more to use ${coupon.code} (minimum ₹${coupon.minOrderValue.toInt()}).")
        }
        val raw = when (coupon.type) {
            DiscountType.PERCENT -> subtotal * coupon.value / 100.0
            DiscountType.FIXED -> coupon.value
        }
        val capped = coupon.maxDiscount?.let { minOf(raw, it) } ?: raw
        val discount = capped.roundToInt().toDouble().coerceIn(0.0, subtotal)
        return CouponCheck.Valid(coupon, discount)
    }

    fun calculate(lines: List<CartLine>, coupon: Coupon? = null, nowMillis: Long = System.currentTimeMillis(), usedCodes: Set<String> = emptySet()): PriceBreakdown {
        val subtotal = subtotal(lines)
        val itemCount = lines.sumOf { it.quantity }
        val fee = deliveryFee(subtotal)
        if (coupon == null) return PriceBreakdown(itemCount, subtotal, fee, 0.0, subtotal + fee)
        return when (val check = validateCoupon(coupon, subtotal, nowMillis, usedCodes)) {
            is CouponCheck.Valid -> PriceBreakdown(itemCount, subtotal, fee, check.discount, subtotal - check.discount + fee, coupon.code)
            is CouponCheck.Invalid -> PriceBreakdown(itemCount, subtotal, fee, 0.0, subtotal + fee, couponError = check.reason)
        }
    }
}

/** Quantity rules shared by the basket and reorder. */
object CartRules {
    const val MAX_PER_ITEM = 20

    fun maxQuantity(product: Product): Int = minOf(product.stock, MAX_PER_ITEM).coerceAtLeast(0)
}
