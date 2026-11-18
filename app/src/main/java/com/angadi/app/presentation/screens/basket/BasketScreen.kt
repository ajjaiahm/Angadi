package com.angadi.app.presentation.screens.basket

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angadi.app.domain.logic.CartLine
import com.angadi.app.domain.logic.PriceBreakdown
import com.angadi.app.domain.logic.PricingEngine
import com.angadi.app.presentation.components.EmptyState
import com.angadi.app.presentation.components.ProductArtwork
import com.angadi.app.presentation.components.QuantityControl
import com.angadi.app.presentation.components.SampleDataFootnote
import com.angadi.app.presentation.components.ScreenHeader
import com.angadi.app.presentation.components.SummaryLine
import com.angadi.app.presentation.components.formatPrice

@Composable
fun BasketScreen(
    lines: List<CartLine>,
    breakdown: PriceBreakdown,
    cartCount: Int,
    onAssistant: () -> Unit,
    onBasket: () -> Unit,
    onBrowse: () -> Unit,
    onChangeQuantity: (CartLine, Int) -> Unit,
    onRemove: (CartLine) -> Unit,
    onCheckout: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("basket_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader("Your basket", "The good stuff you picked", cartCount, onAssistant, onBasket) }
        if (lines.isEmpty()) {
            item { EmptyState("Your basket is taking a breather", "Browse the shelves and add a few favourites.", "▤", Modifier.testTag("empty_basket")) }
            item { Button(onClick = onBrowse, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Browse the shop") } }
        } else {
            if (breakdown.deliveryFee > 0) item {
                val remaining = PricingEngine.FREE_DELIVERY_THRESHOLD - breakdown.subtotal
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(16.dp)) {
                    Text("Add ${formatPrice(remaining)} more for free delivery", Modifier.fillMaxWidth().padding(14.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
            items(lines, key = { it.product.id }) { line -> BasketRow(line, onChangeQuantity, onRemove) }
            item { PriceSummary(breakdown, onCheckout) }
            item { SampleDataFootnote() }
        }
    }
}

@Composable
private fun BasketRow(line: CartLine, onChange: (CartLine, Int) -> Unit, onRemove: (CartLine) -> Unit) {
    val product = line.product
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(MaterialTheme.colorScheme.surface).padding(12.dp).testTag("basket_row_${product.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductArtwork(product.thumbnail, product.emoji, product.category, product.title, Modifier.size(88.dp).clip(RoundedCornerShape(15.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(product.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(product.unit, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatPrice(line.lineTotal), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                QuantityControl(line.quantity, onMinus = { onChange(line, -1) }, onPlus = { onChange(line, 1) }, productName = product.title)
            }
            TextButton(onClick = { onRemove(line) }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Text("Remove", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun PriceSummary(breakdown: PriceBreakdown, onCheckout: (() -> Unit)? = null) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("Order summary", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            SummaryLine("Items (${breakdown.itemCount})", formatPrice(breakdown.subtotal))
            SummaryLine("Delivery", if (breakdown.deliveryFee == 0.0) "Free" else formatPrice(breakdown.deliveryFee))
            if (breakdown.discount > 0) SummaryLine("Discount${breakdown.couponCode?.let { " ($it)" }.orEmpty()}", "− ${formatPrice(breakdown.discount)}", valueColor = MaterialTheme.colorScheme.primary)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SummaryLine("Total", formatPrice(breakdown.total), strong = true)
            if (onCheckout != null) Button(
                onClick = onCheckout, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("checkout_button"), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) { Text("Continue to checkout  ·  ${formatPrice(breakdown.total)}", fontWeight = FontWeight.Bold) }
        }
    }
}
