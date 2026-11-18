package com.angadi.app.presentation.screens.addresses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angadi.app.domain.logic.AddressField
import com.angadi.app.domain.model.Address
import com.angadi.app.domain.model.AddressLabel
import com.angadi.app.presentation.components.BackHeader
import com.angadi.app.presentation.components.EmptyState
import com.angadi.app.presentation.components.SelectableChip
import com.angadi.app.presentation.viewmodel.AddressEditorState

@Composable
fun AddressesScreen(addresses: List<Address>?, onBack: () -> Unit, onAdd: () -> Unit, onEdit: (Long) -> Unit, onDelete: (Long) -> Unit, onSetDefault: (Long) -> Unit) {
    var pendingDelete by remember { mutableStateOf<Address?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("addresses_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { BackHeader("Saved addresses", "Used at checkout", onBack) }
        when {
            addresses == null -> item { CircularProgressIndicator(Modifier.padding(24.dp)) }
            addresses.isEmpty() -> item { EmptyState("No addresses yet", "Add a home or work address to check out faster.", "⌖", Modifier.testTag("empty_addresses")) }
            else -> items(addresses, key = { it.id }) { address ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp).testTag("address_card_${address.id}"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(address.label.display, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        if (address.isDefault) Text("Default", Modifier.clip(RoundedCornerShape(99.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Text("${address.recipientName} · ${address.phone}", fontSize = 12.sp)
                    Text(address.singleLine, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { onEdit(address.id) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Edit") }
                        if (!address.isDefault) TextButton(onClick = { onSetDefault(address.id) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Make default") }
                        TextButton(onClick = { pendingDelete = address }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        item { Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("add_address"), shape = RoundedCornerShape(14.dp)) { Text("+ Add new address") } }
    }
    pendingDelete?.let { address ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this address?") },
            text = { Text("${address.label.display} – ${address.singleLine}") },
            confirmButton = { TextButton(onClick = { onDelete(address.id); pendingDelete = null }, modifier = Modifier.testTag("confirm_delete_address")) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Keep") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddressEditorScreen(editor: AddressEditorState, onUpdate: ((Address) -> Address) -> Unit, onSave: () -> Unit, onSaved: () -> Unit, onBack: () -> Unit) {
    LaunchedEffect(editor.saved) { if (editor.saved) onSaved() }
    val d = editor.draft
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("address_editor"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { BackHeader(if (editor.isNew) "Add address" else "Edit address", null, onBack) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AddressLabel.entries.forEach { label -> SelectableChip(label.display, d.label == label, { onUpdate { it.copy(label = label) } }, Modifier.testTag("label_${label.name}")) }
            }
        }
        item { Field("Recipient name", d.recipientName, editor.errors[AddressField.RECIPIENT], "field_recipient", KeyboardCapitalization.Words) { v -> onUpdate { it.copy(recipientName = v) } } }
        item { Field("Phone number", d.phone, editor.errors[AddressField.PHONE], "field_phone", keyboardType = KeyboardType.Phone) { v -> onUpdate { it.copy(phone = v.take(14)) } } }
        item { Field("House / building", d.house, editor.errors[AddressField.HOUSE], "field_house") { v -> onUpdate { it.copy(house = v) } } }
        item { Field("Street", d.street, editor.errors[AddressField.STREET], "field_street", KeyboardCapitalization.Words) { v -> onUpdate { it.copy(street = v) } } }
        item { Field("Area / locality", d.area, editor.errors[AddressField.AREA], "field_area", KeyboardCapitalization.Words) { v -> onUpdate { it.copy(area = v) } } }
        item { Field("City", d.city, editor.errors[AddressField.CITY], "field_city", KeyboardCapitalization.Words) { v -> onUpdate { it.copy(city = v) } } }
        item { Field("State", d.state, editor.errors[AddressField.STATE], "field_state", KeyboardCapitalization.Words) { v -> onUpdate { it.copy(state = v) } } }
        item { Field("PIN code", d.pinCode, editor.errors[AddressField.PIN_CODE], "field_pin", keyboardType = KeyboardType.Number) { v -> onUpdate { it.copy(pinCode = v.filter(Char::isDigit).take(6)) } } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Use as default address", Modifier.weight(1f), fontSize = 13.sp)
                androidx.compose.material3.Switch(checked = d.isDefault, onCheckedChange = { c -> onUpdate { it.copy(isDefault = c) } }, modifier = Modifier.testTag("field_default"))
            }
        }
        item { Button(onClick = onSave, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("save_address"), shape = RoundedCornerShape(14.dp)) { Text("Save address") } }
        item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cancel") } }
    }
}

@Composable
private fun Field(
    label: String, value: String, error: String?, tag: String,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth().testTag(tag), label = { Text(label) }, singleLine = true,
        isError = error != null, supportingText = error?.let { { Text(it, Modifier.testTag("${tag}_error")) } },
        keyboardOptions = KeyboardOptions(capitalization = capitalization, keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
    )
}
