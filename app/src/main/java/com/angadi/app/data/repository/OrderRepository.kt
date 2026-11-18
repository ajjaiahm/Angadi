package com.angadi.app.data.repository

import androidx.room.withTransaction
import com.angadi.app.data.local.AngadiDatabase
import com.angadi.app.data.local.CouponUsageEntity
import com.angadi.app.data.local.OrderEntity
import com.angadi.app.data.local.OrderItemEntity
import com.angadi.app.domain.logic.CartLine
import com.angadi.app.domain.logic.OrderPlanner
import com.angadi.app.domain.logic.PlanResult
import com.angadi.app.domain.logic.ReorderPlanner
import com.angadi.app.domain.logic.ReorderResult
import com.angadi.app.domain.model.Address
import com.angadi.app.domain.model.Order
import com.angadi.app.domain.model.OrderItem
import com.angadi.app.domain.model.OrderStatus
import com.angadi.app.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZonedDateTime

sealed interface PlaceOrderResult {
    data class Placed(val orderId: Long) : PlaceOrderResult
    data class Rejected(val reason: String) : PlaceOrderResult
}

class OrderRepository(
    private val db: AngadiDatabase,
    private val shop: ShopRepository,
    private val clock: () -> ZonedDateTime = { ZonedDateTime.now() },
) {
    private val dao get() = db.orderDao()

    val orders: Flow<List<Order>> = combine(dao.observeOrders(), dao.observeAllItems()) { orders, items ->
        val byOrder = items.groupBy { it.orderId }
        orders.map { it.toDomain(byOrder[it.id].orEmpty().map(OrderItemEntity::toDomain)) }
    }

    fun observeOrder(id: Long): Flow<Order?> = combine(dao.observeOrder(id), dao.observeItems(id)) { order, items ->
        order?.toDomain(items.map(OrderItemEntity::toDomain))
    }

    suspend fun usedCoupons(): Set<String> = dao.usedCoupons().toSet()

    /**
     * Places an order from the *persisted* basket. Prices, stock, coupon, delivery fee, and the delivery slot are all
     * recalculated here by [OrderPlanner]; the caller only supplies choices (address, slot, coupon code, payment).
     */
    suspend fun placeOrder(
        address: Address?,
        slotDate: LocalDate?,
        slotStartHour: Int?,
        couponCode: String?,
        payment: PaymentMethod,
    ): PlaceOrderResult = db.withTransaction {
        val now = clock()
        val catalog = shop.catalog.value.associateBy { it.id }
        val lines = db.cartDao().getAll().mapNotNull { row -> catalog[row.productId]?.let { CartLine(it, row.quantity) } }
        when (val plan = OrderPlanner.plan(lines, address, slotDate, slotStartHour, couponCode, payment, usedCoupons(), now)) {
            is PlanResult.Rejected -> PlaceOrderResult.Rejected(plan.reason)
            is PlanResult.Ready -> {
                val draft = plan.draft
                val b = draft.breakdown
                val millis = now.toInstant().toEpochMilli()
                val orderId = dao.insertOrder(
                    OrderEntity(
                        createdAt = millis, status = OrderStatus.PLACED.name, statusUpdatedAt = millis,
                        subtotal = b.subtotal, deliveryFee = b.deliveryFee, discount = b.discount, total = b.total,
                        couponCode = b.couponCode, itemCount = b.itemCount, paymentMethod = draft.paymentMethod.name,
                        recipientName = draft.address.recipientName, phone = draft.address.phone,
                        addressLabel = draft.address.label.display, addressText = draft.address.singleLine,
                        slotLabel = draft.slotLabel, slotStart = draft.slotStart,
                    ),
                )
                dao.insertItems(draft.items.map { it.toEntity(orderId) })
                b.couponCode?.let { dao.recordCouponUse(CouponUsageEntity(it, millis, orderId)) }
                db.cartDao().clear()
                PlaceOrderResult.Placed(orderId)
            }
        }
    }

    /** Demo-only progression: there is no logistics integration, so the user advances status manually. */
    suspend fun simulateNextStatus(orderId: Long) {
        val order = dao.getOrder(orderId) ?: return
        val next = OrderStatus.fromName(order.status).next() ?: return
        dao.updateStatus(orderId, next.name, clock().toInstant().toEpochMilli())
    }

    /** Returns false if the order can no longer be cancelled. Cancelling releases a one-time coupon. */
    suspend fun cancel(orderId: Long): Boolean = db.withTransaction {
        val order = dao.getOrder(orderId) ?: return@withTransaction false
        if (!OrderStatus.fromName(order.status).canCancel) return@withTransaction false
        dao.updateStatus(orderId, OrderStatus.CANCELLED.name, clock().toInstant().toEpochMilli())
        dao.releaseCoupon(orderId)
        true
    }

    /** Adds the order's items to the basket at today's prices and returns a summary of what could not be added. */
    suspend fun reorder(orderId: Long): ReorderResult? = db.withTransaction {
        val items = dao.getItems(orderId).map(OrderItemEntity::toDomain)
        if (items.isEmpty()) return@withTransaction null
        val result = ReorderPlanner.plan(items, shop.catalog.value, shop.cartSnapshot())
        if (result.hasAnything) shop.replaceCart(result.newCart)
        result
    }
}

private fun OrderEntity.toDomain(items: List<OrderItem>) = Order(
    id = id, createdAt = createdAt, status = OrderStatus.fromName(status), statusUpdatedAt = statusUpdatedAt,
    subtotal = subtotal, deliveryFee = deliveryFee, discount = discount, total = total, couponCode = couponCode,
    itemCount = itemCount, paymentMethod = PaymentMethod.fromName(paymentMethod), recipientName = recipientName, phone = phone,
    addressLabel = addressLabel, addressText = addressText, slotLabel = slotLabel, slotStart = slotStart, items = items,
)

private fun OrderItemEntity.toDomain() = OrderItem(id, productId, title, unit, unitPrice, quantity, thumbnail, emoji, category)
private fun OrderItem.toEntity(orderId: Long) = OrderItemEntity(0, orderId, productId, title, unit, unitPrice, quantity, thumbnail, emoji, category)
