package com.angadi.app.presentation.screens.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angadi.app.domain.model.Address
import com.angadi.app.domain.model.Coupon
import com.angadi.app.domain.model.PaymentMethod
import com.angadi.app.presentation.components.BackHeader
import com.angadi.app.presentation.components.SectionCard
import com.angadi.app.presentation.components.SelectableChip
import com.angadi.app.presentation.components.SummaryLine
import com.angadi.app.presentation.components.formatPrice
import com.angadi.app.presentation.viewmodel.CheckoutUiState
import com.angadi.app.presentation.viewmodel.CheckoutViewModel
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckoutScreen(state: CheckoutUiState, viewModel: CheckoutViewModel, onBack: () -> Unit, onManageAddresses: () -> Unit, onAddAddress: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("checkout_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { BackHeader("Checkout", "One last little step", onBack) }
        item {
            SectionCard("Delivery address", "Where should we bring your order?") {
                if (state.addresses.isEmpty()) {
                    Text("You haven't saved an address yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onAddAddress, modifier = Modifier.padding(top = 10.dp).heightIn(min = 48.dp).testTag("add_address_checkout")) { Text("Add delivery address") }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.addresses.forEach { address -> AddressChoice(address, address.id == state.selectedAddress?.id) { viewModel.selectAddress(address.id) } }
                    }
                    Row(Modifier.padding(top = 6.dp)) {
                        TextButton(onClick = onAddAddress) { Text("+ Add new") }
                        TextButton(onClick = onManageAddresses) { Text("Manage addresses") }
                    }
                }
            }
        }
        item {
            SectionCard("Delivery slot", "Demo availability – simulated, not a live delivery schedule") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag("slot_picker")) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.days, key = { it.date.toString() }) { day ->
                            val selected = state.slotDate == day.date
                            SelectableChip(day.label, selected = selected || (state.slotDate == null && day == state.days.firstOrNull { d -> d.slots.any { it.available } }), onClick = {
                                day.slots.firstOrNull { it.available }?.let { viewModel.selectSlot(day.date, it.startHour) }
                            }, enabled = day.slots.any { it.available })
                        }
                    }
                    val visibleDay = state.days.firstOrNull { it.date == state.slotDate } ?: state.days.firstOrNull { d -> d.slots.any { it.available } }
                    visibleDay?.let { day ->
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            day.slots.forEach { slot ->
                                val selected = state.slotDate == slot.date && state.slotHour == slot.startHour
                                SelectableChip(
                                    label = if (slot.available) "${slot.timeLabel} · ${slot.slotsLeft} left" else "${slot.timeLabel} · Full",
                                    selected = selected, enabled = slot.available,
                                    onClick = { viewModel.selectSlot(slot.date, slot.startHour) },
                                    modifier = Modifier.testTag("slot_${slot.key}"),
                                )
                            }
                        }
                    }
                    if (state.days.none { d -> d.slots.any { it.available } }) Text("No slots are available right now.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        item { CouponSection(state, viewModel) }
        item {
            SectionCard("Payment", "Choose how you’d like to pay") {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    PaymentMethod.entries.forEach { method -> PaymentOption(method, state.payment == method) { viewModel.selectPayment(method) } }
                }
                Text("Pay on delivery only. No payment gateway is connected, so nothing is charged in this build.", Modifier.padding(top = 10.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionCard("Your order", "${state.breakdown.itemCount} item(s)") {
                state.lines.forEach { line ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${line.quantity} × ${line.product.title}", Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatPrice(line.lineTotal), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryLine("Subtotal", formatPrice(state.breakdown.subtotal))
                    SummaryLine("Delivery", if (state.breakdown.deliveryFee == 0.0) "Free" else formatPrice(state.breakdown.deliveryFee))
                    if (state.breakdown.discount > 0) SummaryLine("Discount (${state.breakdown.couponCode})", "− ${formatPrice(state.breakdown.discount)}", valueColor = MaterialTheme.colorScheme.primary)
                    SummaryLine("Total", formatPrice(state.breakdown.total), strong = true)
                }
            }
        }
        state.error?.let { error -> item { Text(error, Modifier.testTag("checkout_error"), color = MaterialTheme.colorScheme.error, fontSize = 13.sp) } }
        item {
            Button(
                onClick = viewModel::placeOrder, enabled = state.canPlace,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("place_order"), shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                if (state.isPlacing) CircularProgressIndicator(Modifier.size(19.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Place demo order · ${formatPrice(state.breakdown.total)}", fontWeight = FontWeight.Bold)
            }
        }
        if (state.selectedAddress == null || state.slotDate == null) item {
            Text(
                listOfNotNull("a delivery address".takeIf { state.selectedAddress == null }, "a delivery slot".takeIf { state.slotDate == null }).joinToString(" and ", prefix = "Choose "),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { Text("Sample catalogue and checkout are for demonstration. Connect your own merchant, inventory, payment, and delivery services before accepting real orders.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp) }
    }
}

@Composable
private fun AddressChoice(address: Address, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect).heightIn(min = 56.dp).padding(12.dp).testTag("address_choice_${address.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (selected) "●" else "○", color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("${address.label.display} · ${address.recipientName}${if (address.isDefault) " · Default" else ""}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(address.singleLine, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CouponSection(state: CheckoutUiState, viewModel: CheckoutViewModel) {
    SectionCard("Coupons", "Offers are checked again when you place the order") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.couponInput, onValueChange = viewModel::setCouponInput, modifier = Modifier.weight(1f).testTag("coupon_input"),
                label = { Text("Coupon code") }, singleLine = true, shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            )
            if (state.appliedCoupon != null) OutlinedButton(onClick = viewModel::removeCoupon, modifier = Modifier.heightIn(min = 48.dp)) { Text("Remove") }
            else Button(onClick = { viewModel.applyCoupon() }, enabled = state.couponInput.isNotBlank(), modifier = Modifier.heightIn(min = 48.dp).testTag("apply_coupon")) { Text("Apply") }
        }
        state.couponMessage?.let {
            Text(it, Modifier.padding(top = 6.dp).testTag("coupon_message"), fontSize = 12.sp, color = if (state.appliedCoupon != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.size(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.couponOptions.forEach { option -> CouponRow(option.coupon, option.eligible, option.note, state.appliedCoupon?.code == option.coupon.code) { viewModel.applyCoupon(option.coupon.code) } }
        }
    }
}

@Composable
private fun CouponRow(coupon: Coupon, eligible: Boolean, note: String, applied: Boolean, onApply: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(14.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(coupon.code, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (eligible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(coupon.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(note, fontSize = 11.sp, color = if (eligible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        }
        TextButton(onClick = onApply, enabled = eligible && !applied) { Text(if (applied) "Applied" else "Use") }
    }
}

@Composable
private fun PaymentOption(method: PaymentMethod, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect).heightIn(min = 56.dp).padding(12.dp).testTag("payment_${method.name}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (selected) "●" else "○", color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(method.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(method.detail, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Suppress("unused")
private fun LocalDate.label(): String = toString()
