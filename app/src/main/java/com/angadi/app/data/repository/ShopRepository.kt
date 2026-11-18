package com.angadi.app.data.repository

import com.angadi.app.data.local.AngadiDatabase
import com.angadi.app.data.local.CachedProductEntity
import com.angadi.app.data.local.CartItemEntity
import com.angadi.app.data.local.FavoriteEntity
import com.angadi.app.data.remote.DummyJsonCatalogService
import com.angadi.app.domain.model.Product
import com.angadi.app.domain.model.SeedCatalog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Catalogue, basket, saved items, recently viewed products and search history. */
class ShopRepository(
    private val db: AngadiDatabase,
    private val catalogService: DummyJsonCatalogService = DummyJsonCatalogService(),
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _catalog = MutableStateFlow(SeedCatalog.products)
    val catalog: StateFlow<List<Product>> = _catalog.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _catalogError = MutableStateFlow<String?>(null)
    val catalogError: StateFlow<String?> = _catalogError.asStateFlow()

    val cart: Flow<Map<Int, Int>> = db.cartDao().observe().map { rows -> rows.filter { it.quantity > 0 }.associate { it.productId to it.quantity } }
    val favorites: Flow<Set<Int>> = db.favoriteDao().observe().map { it.toSet() }
    val recentViewIds: Flow<List<Int>> = db.recentViewDao().observeIds()

    /** Loads the last-known catalogue so baskets keep working offline. */
    suspend fun loadCachedCatalog() {
        val cached = db.catalogDao().getAll().map { it.toProduct() }
        if (cached.isNotEmpty()) _catalog.value = (cached + SeedCatalog.products).distinctBy { it.id }
    }

    suspend fun refreshCatalog() {
        _isRefreshing.value = true
        try {
            val remote = withContext(io) { catalogService.fetchGroceries() }
            if (remote.isNotEmpty()) db.catalogDao().upsertAll(remote.map { it.toEntity() })
            _catalog.value = (remote + SeedCatalog.products).distinctBy { it.id }
            _catalogError.value = null
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (error: Exception) {
            _catalogError.value = "Showing saved products – couldn't refresh the catalogue."
        } finally {
            _isRefreshing.value = false
        }
    }

    suspend fun setQuantity(productId: Int, quantity: Int) {
        if (quantity <= 0) db.cartDao().remove(productId) else db.cartDao().upsert(CartItemEntity(productId, quantity))
    }

    suspend fun replaceCart(items: Map<Int, Int>) = db.cartDao().replaceAll(items.filterValues { it > 0 }.map { CartItemEntity(it.key, it.value) })

    suspend fun cartSnapshot(): Map<Int, Int> = db.cartDao().getAll().associate { it.productId to it.quantity }

    suspend fun setFavorite(productId: Int, favorite: Boolean) {
        if (favorite) db.favoriteDao().add(FavoriteEntity(productId)) else db.favoriteDao().remove(productId)
    }

    suspend fun recordView(productId: Int, limit: Int) = db.recentViewDao().record(productId, clock(), limit.coerceAtLeast(1))
    suspend fun clearRecentViews() = db.recentViewDao().clear()

    fun searchHistory(limit: Int = SEARCH_HISTORY_LIMIT): Flow<List<String>> = db.searchHistoryDao().observe(limit)
    suspend fun recordSearch(query: String) {
        val normalized = query.trim().lowercase()
        if (normalized.length >= 2) db.searchHistoryDao().record(normalized, clock(), SEARCH_HISTORY_LIMIT)
    }
    suspend fun clearSearchHistory() = db.searchHistoryDao().clear()

    companion object {
        const val SEARCH_HISTORY_LIMIT = 10
    }
}

private fun Product.toEntity() = CachedProductEntity(id, title, description, category, unitPrice, discountPercent, rating, stock, thumbnail, unit, emoji, isSampleApiPrice)
private fun CachedProductEntity.toProduct() = Product(id, title, description, category, unitPrice, discountPercent, rating, stock, thumbnail, unit, emoji, isSampleApiPrice)
