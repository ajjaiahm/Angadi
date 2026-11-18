package com.angadi.app.domain.logic

import com.angadi.app.domain.model.Address
import com.angadi.app.domain.model.Coupon
import com.angadi.app.domain.model.CouponCatalog
import com.angadi.app.domain.model.OrderItem
import com.angadi.app.domain.model.PaymentMethod
import java.time.LocalDate
import java.time.ZonedDateTime

data class OrderDraft(
    val items: List<OrderItem>,
    val breakdown: PriceBreakdown,
    val address: Address,
    val paymentMethod: PaymentMethod,
    val slotLabel: String,
    val slotStart: Long,
)

sealed interface PlanResult {
    data class Ready(val draft: OrderDraft) : PlanResult
    data class Rejected(val reason: String) : PlanResult
}

/**
 * Turns persisted basket rows plus the user's choices into an order. Prices, stock, coupon and slot
 * rules are all re-checked here; nothing computed by the UI is accepted.
 */
object OrderPlanner {
    fun plan(
        lines: List<CartLine>,
        address: Address?,
        slotDate: LocalDate?,
        slotStartHour: Int?,
        couponCode: String?,
        paymentMethod: PaymentMethod,
        usedCouponCodes: Set<String>,
        now: ZonedDateTime = ZonedDateTime.now(),
    ): PlanResult {
        if (lines.isEmpty()) return PlanResult.Rejected("Your basket is empty.")
        lines.firstOrNull { it.quantity > CartRules.maxQuantity(it.product) }?.let {
            return PlanResult.Rejected("Only ${CartRules.maxQuantity(it.product)} of ${it.product.title} can be ordered right now.")
        }
        if (address == null) return PlanResult.Rejected("Choose a delivery address.")
        if (AddressValidator.validate(address).isNotEmpty()) return PlanResult.Rejected("The delivery address is incomplete. Please edit it.")
        if (slotDate == null || slotStartHour == null) return PlanResult.Rejected("Choose a delivery slot.")
        val slot = DeliverySlotPlanner.find(slotDate, slotStartHour, now)
        if (slot == null || !slot.available) return PlanResult.Rejected("That delivery slot is no longer available. Pick another.")
        val coupon: Coupon? = couponCode?.takeIf { it.isNotBlank() }?.let {
            CouponCatalog.find(it) ?: return PlanResult.Rejected("Coupon $it doesn't exist.")
        }
        val breakdown = PricingEngine.calculate(lines, coupon, now.toInstant().toEpochMilli(), usedCouponCodes)
        breakdown.couponError?.let { return PlanResult.Rejected(it) }
        val items = lines.map {
            OrderItem(
                productId = it.product.id, title = it.product.title, unit = it.product.unit, unitPrice = it.product.price,
                quantity = it.quantity, thumbnail = it.product.thumbnail, emoji = it.product.emoji, category = it.product.category,
            )
        }
        return PlanResult.Ready(OrderDraft(items, breakdown, address, paymentMethod, "${slot.date} · ${slot.timeLabel}", slot.startMillis))
    }
}
