package com.angadi.app.presentation.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angadi.app.domain.model.Order
import com.angadi.app.domain.model.OrderStatus
import com.angadi.app.presentation.components.BackHeader
import com.angadi.app.presentation.components.ProductArtwork
import com.angadi.app.presentation.components.SectionCard
import com.angadi.app.presentation.components.SummaryLine
import com.angadi.app.presentation.components.formatPrice

@Composable
fun OrderDetailScreen(
    order: Order?,
    loading: Boolean,
    onBack: () -> Unit,
    onCancel: (Long) -> Unit,
    onSimulateNext: (Long) -> Unit,
    onReorder: (Long) -> Unit,
) {
    var confirmCancel by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("order_detail_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { BackHeader(if (order != null) "Order #${order.id}" else "Order", order?.let { formatDateTime(it.createdAt) }, onBack) }
        if (order == null) {
            item { if (loading) CircularProgressIndicator(Modifier.padding(24.dp)) else Text("We couldn't find this order.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            return@LazyColumn
        }
        item { SectionCard("Status", "Simulated progress – there's no live delivery tracking in this build") { StatusTimeline(order) } }
        item {
            SectionCard("Items", "${order.itemCount} item(s) · prices as purchased") {
                if (order.items.isEmpty()) {
                    Text("Item details weren't recorded for this order (it was placed before order details were added).", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    order.items.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("order_item_${item.productId}")) {
                            ProductArtwork(item.thumbnail, item.emoji, item.category, item.title, Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)), emojiSize = 26)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${item.quantity} × ${formatPrice(item.unitPrice)} · ${item.unit}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(formatPrice(item.lineTotal), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            SectionCard("Payment summary", order.paymentMethod.label) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryLine("Subtotal", formatPrice(order.subtotal))
                    SummaryLine("Delivery", if (order.deliveryFee == 0.0) "Free" else formatPrice(order.deliveryFee))
                    if (order.discount > 0) SummaryLine("Discount${order.couponCode?.let { " ($it)" }.orEmpty()}", "− ${formatPrice(order.discount)}", valueColor = MaterialTheme.colorScheme.primary)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SummaryLine("Total", formatPrice(order.total), strong = true)
                }
                Text("Payment is collected on delivery; none was taken in this demo.", Modifier.padding(top = 8.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionCard("Delivery", "Demo slot") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(order.slotLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (order.recipientName.isNotBlank()) Text("${order.addressLabel} · ${order.recipientName}${if (order.phone.isNotBlank()) " · ${order.phone}" else ""}", fontSize = 12.sp)
                    Text(order.addressText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (order.items.isNotEmpty()) Button(onClick = { onReorder(order.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("detail_reorder")) { Text("Reorder these items") }
                if (!order.status.isFinished) OutlinedButton(onClick = { onSimulateNext(order.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("simulate_next")) {
                    Text("Demo: move to “${order.status.next()?.label}”")
                }
                if (order.status.canCancel) OutlinedButton(onClick = { confirmCancel = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("cancel_order")) {
                    Text("Cancel order", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (confirmCancel && order != null) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Cancel this order?") },
            text = { Text("Order #${order.id} will be marked as cancelled.") },
            confirmButton = { TextButton(onClick = { confirmCancel = false; onCancel(order.id) }, modifier = Modifier.testTag("confirm_cancel")) { Text("Cancel order") } },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep order") } },
        )
    }
}

@Composable
private fun StatusTimeline(order: Order) {
    if (order.status == OrderStatus.CANCELLED) {
        Text("Cancelled on ${formatDateTime(order.statusUpdatedAt)}", Modifier.testTag("status_cancelled"), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        return
    }
    val currentIndex = OrderStatus.timeline.indexOf(order.status)
    Column(Modifier.testTag("status_timeline")) {
        OrderStatus.timeline.forEachIndexed { index, step ->
            val reached = index <= currentIndex
            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                    Box(
                        Modifier.size(18.dp).clip(CircleShape).background(if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        contentAlignment = Alignment.Center,
                    ) { if (reached) Text("✓", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary) }
                    if (index < OrderStatus.timeline.lastIndex) Box(Modifier.width(2.dp).heightIn(min = 22.dp).background(if (index < currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    step.label + if (index == currentIndex) "  ·  current" else "",
                    fontSize = 13.sp, fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
