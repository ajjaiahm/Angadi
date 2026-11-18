package com.angadi.app.domain.logic

import com.angadi.app.domain.model.OrderItem
import com.angadi.app.domain.model.Product

data class ReorderLine(val title: String, val requested: Int, val added: Int, val currentPrice: Double, val reason: String? = null)

data class ReorderResult(
    /** The complete basket (productId -> quantity) after merging. */
    val newCart: Map<Int, Int>,
    val added: List<ReorderLine>,
    val unavailable: List<ReorderLine>,
) {
    val addedUnits: Int get() = added.sumOf { it.added }
    val hasAnything: Boolean get() = added.isNotEmpty()
}

object ReorderPlanner {
    /**
     * Re-adds a past order's items at *current* catalogue prices. Items missing from the catalogue,
     * out of stock, or already at the per-item limit are reported instead of silently dropped.
     */
    fun plan(items: List<OrderItem>, catalog: List<Product>, currentCart: Map<Int, Int>): ReorderResult {
        val byId = catalog.associateBy { it.id }
        val cart = currentCart.toMutableMap()
        val added = mutableListOf<ReorderLine>()
        val unavailable = mutableListOf<ReorderLine>()
        for (item in items) {
            val product = byId[item.productId]
            if (product == null) {
                unavailable += ReorderLine(item.title, item.quantity, 0, 0.0, "No longer in the catalogue")
                continue
            }
            val limit = CartRules.maxQuantity(product)
            val room = limit - (cart[product.id] ?: 0)
            if (limit <= 0) {
                unavailable += ReorderLine(item.title, item.quantity, 0, product.price, "Out of stock")
                continue
            }
            if (room <= 0) {
                unavailable += ReorderLine(item.title, item.quantity, 0, product.price, "Already at the maximum of $limit in your basket")
                continue
            }
            val qty = minOf(item.quantity, room)
            cart[product.id] = (cart[product.id] ?: 0) + qty
            added += ReorderLine(
                item.title, item.quantity, qty, product.price,
                if (qty < item.quantity) "Only $qty available" else null,
            )
        }
        return ReorderResult(cart, added, unavailable)
    }
}
