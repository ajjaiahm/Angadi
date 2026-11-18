package com.angadi.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angadi.app.data.repository.OrderRepository
import com.angadi.app.domain.logic.ReorderResult
import com.angadi.app.domain.model.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OrdersViewModel(private val repository: OrderRepository) : ViewModel() {
    val orders: StateFlow<List<Order>?> = repository.orders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _reorderResult = MutableStateFlow<ReorderResult?>(null)
    val reorderResult: StateFlow<ReorderResult?> = _reorderResult.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    fun order(id: Long): Flow<Order?> = repository.observeOrder(id)

    fun cancel(id: Long) {
        viewModelScope.launch {
            _notice.value = if (repository.cancel(id)) "Order #$id cancelled" else "This order can no longer be cancelled"
        }
    }

    fun simulateNext(id: Long) {
        viewModelScope.launch { repository.simulateNextStatus(id) }
    }

    fun reorder(id: Long) {
        viewModelScope.launch {
            val result = repository.reorder(id)
            if (result == null) _notice.value = "This order has no item details to reorder (placed before order details were saved)."
            else _reorderResult.value = result
        }
    }

    fun dismissReorder() {
        _reorderResult.value = null
    }

    fun consumeNotice() {
        _notice.value = null
    }
}
