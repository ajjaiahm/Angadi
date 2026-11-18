package com.angadi.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angadi.app.data.repository.ShopRepository
import com.angadi.app.domain.logic.ProductSearch
import com.angadi.app.domain.model.Product
import com.angadi.app.domain.model.ProductQuery
import com.angadi.app.domain.model.SortOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExploreUiState(
    val query: ProductQuery = ProductQuery(),
    val results: List<Product> = emptyList(),
    val categories: List<String> = listOf("All"),
    val suggestions: List<String> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val maxCatalogPrice: Double = 0.0,
)

class ExploreViewModel(private val shop: ShopRepository) : ViewModel() {
    private val query = MutableStateFlow(ProductQuery())

    val state: StateFlow<ExploreUiState> = combine(shop.catalog, query, shop.searchHistory()) { products, q, history ->
        ExploreUiState(
            query = q,
            results = ProductSearch.apply(products, q),
            categories = listOf("All") + products.map { it.category }.distinct(),
            suggestions = ProductSearch.suggestions(products, history, q.text),
            recentSearches = history,
            maxCatalogPrice = products.maxOfOrNull { it.price } ?: 0.0,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreUiState())

    fun setText(text: String) = query.update { it.copy(text = text) }

    /** Commits the current text to search history (called on keyboard search or suggestion tap). */
    fun submit(text: String = query.value.text) {
        query.update { it.copy(text = text) }
        viewModelScope.launch { shop.recordSearch(text) }
    }

    fun setCategory(category: String) = query.update { it.copy(category = category) }
    fun setPriceRange(min: Double?, max: Double?) = query.update { it.copy(minPrice = min, maxPrice = max) }
    fun setMinRating(rating: Double) = query.update { it.copy(minRating = rating) }
    fun setInStockOnly(enabled: Boolean) = query.update { it.copy(inStockOnly = enabled) }
    fun setSort(sort: SortOption) = query.update { it.copy(sort = sort) }
    fun clearFilters() = query.update { ProductQuery(text = it.text, sort = it.sort) }
    fun clearAll() = query.update { ProductQuery() }
    fun clearSearchHistory() {
        viewModelScope.launch { shop.clearSearchHistory() }
    }
}
