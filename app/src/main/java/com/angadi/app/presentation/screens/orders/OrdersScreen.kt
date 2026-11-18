package com.angadi.app.presentation.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angadi.app.domain.logic.ReorderResult
import com.angadi.app.domain.model.Order
import com.angadi.app.domain.model.OrderStatus
import com.angadi.app.presentation.components.BackHeader
import com.angadi.app.presentation.components.EmptyState
import com.angadi.app.presentation.components.formatPrice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDateTime(millis: Long): String = SimpleDateFormat("dd MMM yyyy · h:mm a", Locale.getDefault()).format(Date(millis))

@Composable
fun OrdersScreen(orders: List<Order>?, onBack: () -> Unit, onOpen: (Long) -> Unit, onReorder: (Long) -> Unit, onShop: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("orders_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { BackHeader("Your orders", "Saved on this device", onBack) }
        when {
            orders == null -> item { CircularProgressIndicator(Modifier.padding(24.dp)) }
            orders.isEmpty() -> {
                item { EmptyState("No orders yet", "When you place an order it will show up here, with its items and progress.", "▤", Modifier.testTag("empty_orders")) }
                item { Button(onClick = onShop, modifier = Modifier.heightIn(min = 48.dp)) { Text("Start shopping") } }
            }
            else -> items(orders, key = { it.id }) { order -> OrderCard(order, onOpen, onReorder) }
        }
    }
}

@Composable
private fun OrderCard(order: Order, onOpen: (Long) -> Unit, onReorder: (Long) -> Unit) {
    val date = remember(order.createdAt) { formatDateTime(order.createdAt) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface)
            .clickable { onOpen(order.id) }.padding(16.dp).testTag("order_card_${order.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Order #${order.id}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusChip(order.status)
        }
        if (order.items.isNotEmpty()) {
            Text(
                order.items.joinToString { "${it.quantity} × ${it.title}" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${order.itemCount} item(s)", Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatPrice(order.total), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onOpen(order.id) }, modifier = Modifier.heightIn(min = 44.dp)) { Text("View details", fontSize = 12.sp) }
            if (order.items.isNotEmpty()) OutlinedButton(onClick = { onReorder(order.id) }, modifier = Modifier.heightIn(min = 44.dp).testTag("reorder_${order.id}")) { Text("Reorder", fontSize = 12.sp) }
        }
    }
}

@Composable
fun StatusChip(status: OrderStatus) {
    val (bg, fg) = when (status) {
        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Text(status.label, Modifier.clip(RoundedCornerShape(99.dp)).background(bg).padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
}

@Composable
fun ReorderSummaryDialog(result: ReorderResult, onDismiss: () -> Unit, onViewBasket: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("reorder_dialog"),
        title = { Text(if (result.hasAnything) "Added to your basket" else "Nothing could be added") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Items are added at today's prices, not the prices you paid before.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                result.added.forEach {
                    Text("✓ ${it.added} × ${it.title} – ${formatPrice(it.currentPrice)} each" + (it.reason?.let { r -> " ($r; you ordered ${it.requested})" } ?: ""), fontSize = 13.sp)
                }
                result.unavailable.forEach { Text("✕ ${it.title} – ${it.reason}", fontSize = 13.sp, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { if (result.hasAnything) TextButton(onClick = onViewBasket) { Text("View basket") } else TextButton(onClick = onDismiss) { Text("OK") } },
        dismissButton = { if (result.hasAnything) TextButton(onClick = onDismiss) { Text("Keep browsing") } },
    )
}
