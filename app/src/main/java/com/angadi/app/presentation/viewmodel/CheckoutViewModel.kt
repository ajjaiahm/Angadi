package com.angadi.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angadi.app.data.repository.AddressRepository
import com.angadi.app.data.repository.OrderRepository
import com.angadi.app.data.repository.PlaceOrderResult
import com.angadi.app.domain.logic.CartLine
import com.angadi.app.domain.logic.CouponCheck
import com.angadi.app.domain.logic.DeliverySlotPlanner
import com.angadi.app.domain.logic.PriceBreakdown
import com.angadi.app.domain.logic.PricingEngine
import com.angadi.app.domain.model.Address
import com.angadi.app.domain.model.Coupon
import com.angadi.app.domain.model.CouponCatalog
import com.angadi.app.domain.model.DeliveryDay
import com.angadi.app.domain.model.PaymentMethod
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CouponOption(val coupon: Coupon, val eligible: Boolean, val note: String)

data class CheckoutUiState(
    val addresses: List<Address> = emptyList(),
    val selectedAddress: Address? = null,
    val days: List<DeliveryDay> = emptyList(),
    val slotDate: LocalDate? = null,
    val slotHour: Int? = null,
    val couponInput: String = "",
    val appliedCoupon: Coupon? = null,
    val couponMessage: String? = null,
    val couponOptions: List<CouponOption> = emptyList(),
    val payment: PaymentMethod = PaymentMethod.CASH_ON_DELIVERY,
    val lines: List<CartLine> = emptyList(),
    val breakdown: PriceBreakdown = PriceBreakdown(0, 0.0, 0.0, 0.0, 0.0),
    val isPlacing: Boolean = false,
    val error: String? = null,
) {
    val canPlace: Boolean get() = lines.isNotEmpty() && selectedAddress != null && slotDate != null && slotHour != null && !isPlacing
}

/**
 * Holds only the user's *choices*. The totals shown here are a preview computed by [PricingEngine];
 * [OrderRepository.placeOrder] recalculates everything from persisted data before saving.
 */
class CheckoutViewModel(
    private val shop: com.angadi.app.data.repository.ShopRepository,
    private val orders: OrderRepository,
    addresses: AddressRepository,
) : ViewModel() {

    private data class Choices(
        val addressId: Long? = null, val slotDate: LocalDate? = null, val slotHour: Int? = null,
        val couponInput: String = "", val applied: Coupon? = null, val couponMessage: String? = null,
        val payment: PaymentMethod = PaymentMethod.CASH_ON_DELIVERY, val isPlacing: Boolean = false,
        val error: String? = null, val used: Set<String> = emptySet(), val days: List<DeliveryDay> = emptyList(),
    )

    private val choices = MutableStateFlow(Choices())
    private val placed = Channel<Long>(Channel.BUFFERED)
    /** Emits the new order id once an order has been saved. */
    val orderPlaced = placed.receiveAsFlow()

    val state: StateFlow<CheckoutUiState> = combine(choices, addresses.addresses, shop.catalog, shop.cart) { c, addressList, catalog, cart ->
        val byId = catalog.associateBy { it.id }
        val lines = cart.mapNotNull { (id, qty) -> byId[id]?.let { CartLine(it, qty) } }
        val now = System.currentTimeMillis()
        val subtotal = PricingEngine.subtotal(lines)
        val breakdown = PricingEngine.calculate(lines, c.applied, now, c.used)
        val selected = addressList.firstOrNull { it.id == c.addressId } ?: addressList.firstOrNull { it.isDefault } ?: addressList.firstOrNull()
        CheckoutUiState(
            addresses = addressList, selectedAddress = selected, days = c.days, slotDate = c.slotDate, slotHour = c.slotHour,
            couponInput = c.couponInput, appliedCoupon = c.applied.takeIf { breakdown.couponError == null },
            couponMessage = breakdown.couponError ?: c.couponMessage,
            couponOptions = CouponCatalog.all.map { coupon ->
                when (val check = PricingEngine.validateCoupon(coupon, subtotal, now, c.used)) {
                    is CouponCheck.Valid -> CouponOption(coupon, true, "Saves ₹${check.discount.toInt()} on this basket")
                    is CouponCheck.Invalid -> CouponOption(coupon, false, check.reason)
                }
            },
            payment = c.payment, lines = lines, breakdown = breakdown, isPlacing = c.isPlacing, error = c.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CheckoutUiState())

    init {
        refreshSlots()
        viewModelScope.launch { choices.update { it.copy(used = orders.usedCoupons()) } }
    }

    /** Re-plans (simulated) slots; keeps the selection only if it is still bookable. */
    fun refreshSlots() {
        val days = DeliverySlotPlanner.plan()
        choices.update { c ->
            val stillValid = c.slotDate != null && c.slotHour != null &&
                days.firstOrNull { it.date == c.slotDate }?.slots?.firstOrNull { it.startHour == c.slotHour }?.available == true
            c.copy(days = days, slotDate = c.slotDate.takeIf { stillValid }, slotHour = c.slotHour.takeIf { stillValid })
        }
    }

    fun selectAddress(id: Long) = choices.update { it.copy(addressId = id, error = null) }
    fun selectSlot(date: LocalDate, hour: Int) = choices.update { it.copy(slotDate = date, slotHour = hour, error = null) }
    fun selectPayment(method: PaymentMethod) = choices.update { it.copy(payment = method) }
    fun setCouponInput(text: String) = choices.update { it.copy(couponInput = text.uppercase().take(20), couponMessage = null) }

    fun applyCoupon(code: String = choices.value.couponInput) {
        val coupon = CouponCatalog.find(code)
        if (coupon == null) {
            choices.update { it.copy(applied = null, couponMessage = "We couldn't find that coupon code.") }
            return
        }
        val subtotal = PricingEngine.subtotal(state.value.lines)
        when (val check = PricingEngine.validateCoupon(coupon, subtotal, System.currentTimeMillis(), choices.value.used)) {
            is CouponCheck.Valid -> choices.update { it.copy(applied = coupon, couponInput = coupon.code, couponMessage = "${coupon.code} applied – you save ₹${check.discount.toInt()}") }
            is CouponCheck.Invalid -> choices.update { it.copy(applied = null, couponMessage = check.reason) }
        }
    }

    fun removeCoupon() = choices.update { it.copy(applied = null, couponInput = "", couponMessage = null) }

    fun placeOrder() {
        val current = state.value
        if (choices.value.isPlacing) return
        choices.update { it.copy(isPlacing = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                orders.placeOrder(current.selectedAddress, current.slotDate, current.slotHour, current.appliedCoupon?.code, current.payment)
            }.getOrElse { PlaceOrderResult.Rejected("We couldn't save your order. Please try again.") }
            when (result) {
                is PlaceOrderResult.Placed -> {
                    choices.update { Choices(used = runCatching { orders.usedCoupons() }.getOrDefault(it.used), days = DeliverySlotPlanner.plan()) }
                    placed.trySend(result.orderId)
                }
                is PlaceOrderResult.Rejected -> {
                    choices.update { it.copy(isPlacing = false, error = result.reason) }
                    refreshSlots()
                }
            }
        }
    }
}
