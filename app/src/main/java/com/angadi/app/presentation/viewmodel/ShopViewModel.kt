package com.angadi.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angadi.app.data.repository.ProfileRepository
import com.angadi.app.data.repository.ReviewRepository
import com.angadi.app.data.repository.SettingsRepository
import com.angadi.app.data.repository.ShopRepository
import com.angadi.app.domain.logic.CartLine
import com.angadi.app.domain.logic.CartRules
import com.angadi.app.domain.logic.PriceBreakdown
import com.angadi.app.domain.logic.PricingEngine
import com.angadi.app.domain.model.Product
import com.angadi.app.domain.model.UserProfile
import com.angadi.app.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class ShopUiState(
    val products: List<Product> = emptyList(),
    val isRefreshing: Boolean = false,
    val catalogError: String? = null,
    val cart: Map<Int, Int> = emptyMap(),
    val cartLines: List<CartLine> = emptyList(),
    val breakdown: PriceBreakdown = PriceBreakdown(0, 0.0, 0.0, 0.0, 0.0),
    val favorites: Set<Int> = emptySet(),
    val favoriteProducts: List<Product> = emptyList(),
    val recentProducts: List<Product> = emptyList(),
    val ratingOverrides: Map<Int, Pair<Double, Int>> = emptyMap(),
    val profile: UserProfile = UserProfile(),
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val recentLimit: Int = SettingsRepository.DEFAULT_RECENT_LIMIT,
) {
    val cartCount: Int get() = cart.values.sum()
}

class ShopViewModel(
    private val shop: ShopRepository,
    private val settings: SettingsRepository,
    profileRepository: ProfileRepository,
    reviewRepository: ReviewRepository,
) : ViewModel() {

    val state: StateFlow<ShopUiState> = combine(
        shop.catalog, shop.isRefreshing, shop.catalogError, shop.cart, shop.favorites,
    ) { products, refreshing, error, cart, favorites ->
        val byId = products.associateBy { it.id }
        val lines = cart.mapNotNull { (id, qty) -> byId[id]?.let { CartLine(it, qty) } }
        ShopUiState(
            products = products, isRefreshing = refreshing, catalogError = error, cart = cart, cartLines = lines,
            breakdown = PricingEngine.calculate(lines), favorites = favorites,
            favoriteProducts = products.filter { it.id in favorites },
        )
    }.combine(shop.recentViewIds) { base, recentIds ->
        val byId = base.products.associateBy { it.id }
        base.copy(recentProducts = recentIds.mapNotNull { byId[it] })
    }.combine(profileRepository.profile) { base, profile -> base.copy(profile = profile) }
        .combine(settings.themeMode) { base, theme -> base.copy(themeMode = theme) }
        .combine(settings.recentLimit) { base, limit -> base.copy(recentLimit = limit) }
        .combine(reviewRepository.summaries) { base, summaries -> base.copy(ratingOverrides = summaries) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ShopUiState(products = shop.catalog.value))

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val messageChannel = Channel<String>(Channel.BUFFERED)
    /** One-shot snackbar messages. */
    val messages = messageChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            shop.loadCachedCatalog()
            shop.refreshCatalog()
        }
    }

    fun refreshCatalog() {
        viewModelScope.launch { shop.refreshCatalog() }
    }

    fun addToCart(product: Product, quantity: Int = 1) {
        viewModelScope.launch {
            val current = shop.cartSnapshot()[product.id] ?: 0
            val limit = CartRules.maxQuantity(product)
            when {
                limit <= 0 -> messageChannel.trySend("${product.title} is out of stock")
                current >= limit -> messageChannel.trySend("You already have the maximum of $limit in your basket")
                else -> {
                    val next = (current + quantity).coerceAtMost(limit)
                    shop.setQuantity(product.id, next)
                    messageChannel.trySend(if (current + quantity > limit) "Added up to the limit of $limit" else "${product.title} added to your basket")
                }
            }
        }
    }

    fun changeQuantity(product: Product, delta: Int) {
        viewModelScope.launch {
            val current = shop.cartSnapshot()[product.id] ?: 0
            val next = (current + delta).coerceAtMost(CartRules.maxQuantity(product))
            if (delta > 0 && next == current) messageChannel.trySend("Only ${CartRules.maxQuantity(product)} available per order")
            shop.setQuantity(product.id, next)
        }
    }

    fun removeFromCart(productId: Int) {
        viewModelScope.launch { shop.setQuantity(productId, 0) }
    }

    fun toggleFavorite(product: Product) {
        viewModelScope.launch {
            val nowFavorite = product.id !in state.value.favorites
            shop.setFavorite(product.id, nowFavorite)
            messageChannel.trySend(if (nowFavorite) "Saved for later" else "Removed from saved items")
        }
    }

    fun openProduct(product: Product) {
        _selectedProduct.value = product
        viewModelScope.launch { shop.recordView(product.id, settings.recentLimit.value) }
    }

    fun closeProduct() {
        _selectedProduct.value = null
    }

    fun clearRecentlyViewed() {
        viewModelScope.launch {
            shop.clearRecentViews()
            messageChannel.trySend("Viewing history cleared")
        }
    }

    fun setRecentLimit(limit: Int) = settings.setRecentLimit(limit)
    fun setThemeMode(mode: AppThemeMode) = settings.setThemeMode(mode)
    fun showMessage(text: String) {
        messageChannel.trySend(text)
    }
}
