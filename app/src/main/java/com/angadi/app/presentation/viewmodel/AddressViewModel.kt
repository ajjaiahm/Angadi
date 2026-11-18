package com.angadi.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angadi.app.data.repository.AddressRepository
import com.angadi.app.data.repository.SaveAddressResult
import com.angadi.app.domain.logic.AddressField
import com.angadi.app.domain.model.Address
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddressEditorState(
    val draft: Address = Address(),
    val errors: Map<AddressField, String> = emptyMap(),
    val isNew: Boolean = true,
    val saved: Boolean = false,
)

class AddressViewModel(private val repository: AddressRepository) : ViewModel() {
    val addresses: StateFlow<List<Address>?> = repository.addresses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _editor = MutableStateFlow(AddressEditorState())
    val editor: StateFlow<AddressEditorState> = _editor.asStateFlow()

    fun startEditing(id: Long?, defaultName: String = "", defaultPhone: String = "") {
        viewModelScope.launch {
            val existing = id?.let { repository.get(it) }
            _editor.value = if (existing != null) AddressEditorState(existing, isNew = false)
            else AddressEditorState(Address(recipientName = defaultName, phone = defaultPhone))
        }
    }

    fun update(transform: (Address) -> Address) = _editor.update { it.copy(draft = transform(it.draft), errors = it.errors - it.errors.keys) }

    fun save() {
        val draft = _editor.value.draft
        viewModelScope.launch {
            when (val result = repository.save(draft)) {
                is SaveAddressResult.Invalid -> _editor.update { it.copy(errors = result.errors) }
                is SaveAddressResult.Saved -> _editor.update { it.copy(saved = true, errors = emptyMap()) }
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun setDefault(id: Long) {
        viewModelScope.launch { repository.setDefault(id) }
    }
}
