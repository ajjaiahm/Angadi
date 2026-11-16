package com.angadi.app.presentation

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.angadi.app.data.local.OrderSummary
import com.angadi.app.data.repository.AngadiRepository
import com.angadi.app.domain.model.Product
import com.angadi.app.domain.model.SeedCatalog
import com.angadi.app.ui.theme.AppThemeMode
import java.util.concurrent.Executors

enum class StoreTab(val label: String, val symbol: String) {
    HOME("Home", "⌂"), EXPLORE("Explore", "⌕"), SAVED("Saved", "♡"), CART("Basket", "▤"), PROFILE("You", "○")
}

data class AssistantMessage(val fromAssistant: Boolean, val text: String)

class AngadiStore(context: Context) {
    private val repository = AngadiRepository(context.applicationContext)
    private val io = Executors.newFixedThreadPool(3)
    private val main = Handler(Looper.getMainLooper())

    var activeTab by mutableStateOf(StoreTab.HOME)
        private set
    var products by mutableStateOf(SeedCatalog.products)
        private set
    var isRefreshing by mutableStateOf(true)
        private set
    var themeMode by mutableStateOf(runCatching { AppThemeMode.valueOf(repository.getThemeMode()) }.getOrDefault(AppThemeMode.SYSTEM))
        private set
    var query by mutableStateOf("")
    var selectedCategory by mutableStateOf("All")
    var sortByPrice by mutableStateOf(false)
    var selectedProduct by mutableStateOf<Product?>(null)
        private set
    var showAssistant by mutableStateOf(false)
    var showCheckout by mutableStateOf(false)
    var cart by mutableStateOf<Map<Int, Int>>(emptyMap())
        private set
    var favorites by mutableStateOf<Set<Int>>(emptySet())
        private set
    var orders by mutableStateOf<List<OrderSummary>>(emptyList())
        private set
    var assistantServer by mutableStateOf(repository.getAssistantServer())
    var assistantServerDraft by mutableStateOf(repository.getAssistantServer())
    var serverStatus by mutableStateOf("Assistant server not connected")
        private set
    var isCheckingServer by mutableStateOf(false)
        private set
    var assistantInput by mutableStateOf("")
    var isAssistantThinking by mutableStateOf(false)
        private set
    var toastMessage by mutableStateOf<String?>(null)
        private set
    var checkoutAddress by mutableStateOf("")
    var isPlacingOrder by mutableStateOf(false)
        private set
    val assistantMessages = mutableStateListOf(
        AssistantMessage(true, "Hi Ajjaiah! I’m Angadi AI. I can guide you around the app: browse in Explore, tap a heart to save an item, add products to your Basket, and open You to change the theme or connect Gemini. Once Gemini is connected, you can also ask me general questions.")
    )

    val cartCount: Int get() = cart.values.sum()
    val categoryNames: List<String>
        get() = listOf("All", "Fresh produce", "Dairy & eggs", "Bakery", "Pantry", "Groceries")
    val cartItems: List<Pair<Product, Int>>
        get() = products.mapNotNull { product -> cart[product.id]?.let { product to it } }
    val favoriteProducts: List<Product>
        get() = products.filter { it.id in favorites }
    val cartSubtotal: Double
        get() = cartItems.sumOf { (product, count) -> product.price * count }

    init {
        io.execute {
            val savedCart = runCatching { repository.loadCart() }.getOrDefault(emptyMap())
            val savedFavorites = runCatching { repository.loadFavorites() }.getOrDefault(emptySet())
            val savedOrders = runCatching { repository.loadOrders() }.getOrDefault(emptyList())
            main.post {
                cart = savedCart
                favorites = savedFavorites
                orders = savedOrders
            }
        }
        refreshCatalog()
    }

    fun refreshCatalog() {
        isRefreshing = true
        io.execute {
            val remote = runCatching { repository.loadCatalog() }.getOrDefault(emptyList())
            val combined = (remote + SeedCatalog.products).distinctBy { it.id }
            main.post {
                products = combined
                isRefreshing = false
            }
        }
    }

    fun navigate(tab: StoreTab) {
        activeTab = tab
        showAssistant = false
        showCheckout = false
    }

    fun applyThemeMode(mode: AppThemeMode) {
        themeMode = mode
        repository.saveThemeMode(mode.name)
    }

    fun setCategory(category: String) {
        selectedCategory = category
        activeTab = StoreTab.EXPLORE
    }

    fun visibleProducts(): List<Product> {
        val matching = products.filter { product ->
            val matchesQuery = query.isBlank() || product.title.contains(query.trim(), ignoreCase = true) || product.description.contains(query.trim(), ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || product.category.equals(selectedCategory, ignoreCase = true)
            matchesQuery && matchesCategory
        }
        return if (sortByPrice) matching.sortedBy { it.price } else matching
    }

    fun addToCart(product: Product, quantity: Int = 1) {
        val updated = cart.toMutableMap()
        updated[product.id] = (updated[product.id] ?: 0) + quantity
        cart = updated
        persistCart(updated)
        toastMessage = "${product.title} added to your basket"
    }

    fun changeQuantity(productId: Int, delta: Int) {
        val updated = cart.toMutableMap()
        val quantity = (updated[productId] ?: 0) + delta
        if (quantity <= 0) updated.remove(productId) else updated[productId] = quantity
        cart = updated
        persistCart(updated)
    }

    fun toggleFavorite(product: Product) {
        val updated = favorites.toMutableSet()
        if (!updated.add(product.id)) updated.remove(product.id)
        favorites = updated
        io.execute { runCatching { repository.saveFavorites(updated) } }
        toastMessage = if (product.id in updated) "Saved for later" else "Removed from saved items"
    }

    fun openProduct(product: Product) {
        selectedProduct = product
    }

    fun closeProduct() {
        selectedProduct = null
    }

    fun saveAssistantServer() {
        val value = assistantServerDraft.trim().trimEnd('/')
        repository.saveAssistantServer(value)
        assistantServer = value
        serverStatus = if (value.isBlank()) "Assistant server not connected" else "Server address saved. Tap Test connection."
        toastMessage = if (value.isBlank()) "Assistant preview mode is on" else "Assistant server address saved"
    }

    fun testAssistantServer() {
        if (assistantServerDraft.isBlank()) {
            serverStatus = "Enter your API server address first"
            return
        }
        isCheckingServer = true
        serverStatus = "Checking connection…"
        io.execute {
            val result = runCatching { repository.checkAssistantServer(assistantServerDraft.trim().trimEnd('/')) }
                .getOrElse { "Connection failed: ${it.message ?: "check the server address"}" }
            main.post {
                isCheckingServer = false
                serverStatus = result
            }
        }
    }

    fun sendAssistant() {
        val text = assistantInput.trim()
        if (text.isBlank() || isAssistantThinking) return
        assistantInput = ""
        assistantMessages.add(AssistantMessage(false, text))
        if (assistantServer.isBlank()) {
            assistantMessages.add(AssistantMessage(true, localAssistantReply(text)))
            return
        }
        isAssistantThinking = true
        val history = assistantMessages.map { it.fromAssistant to it.text }
        val storeContext = buildString {
            appendLine("Current catalogue (sample data): ${products.take(24).joinToString { "${it.title} at ₹${it.price.toInt()}" }}")
            appendLine("Basket: ${cartItems.joinToString { "${it.second} × ${it.first.title}" }.ifBlank { "empty" }}")
            appendLine("Saved item count: ${favorites.size}. Past order count: ${orders.size}.")
            append("Prices, inventory, and order records in this app are demo data.")
        }
        io.execute {
            val reply = runCatching { repository.askAssistant(assistantServer, history, storeContext) }
                .getOrElse { "I couldn’t reach the AI assistant right now. Check the API server connection in your profile and try again. (${it.message ?: "network error"})" }
            main.post {
                assistantMessages.add(AssistantMessage(true, reply))
                isAssistantThinking = false
            }
        }
    }

    fun placeOrder() {
        if (cart.isEmpty() || isPlacingOrder) return
        if (checkoutAddress.trim().length < 8) {
            toastMessage = "Add a delivery address to continue"
            return
        }
        val total = cartSubtotal
        val itemCount = cartCount
        isPlacingOrder = true
        io.execute {
            val saved = runCatching { repository.createOrder(total, itemCount) }.isSuccess
            val nextOrders = runCatching { repository.loadOrders() }.getOrDefault(orders)
            main.post {
                isPlacingOrder = false
                if (saved) {
                    cart = emptyMap()
                    orders = nextOrders
                    showCheckout = false
                    activeTab = StoreTab.PROFILE
                    toastMessage = "Order placed! Payment is demo-only in this build."
                } else {
                    toastMessage = "Couldn’t save your order. Please try again."
                }
            }
        }
    }

    fun clearToast() {
        toastMessage = null
    }

    private fun persistCart(updated: Map<Int, Int>) {
        io.execute { runCatching { repository.saveCart(updated) } }
    }

    private fun localAssistantReply(question: String): String {
        val prompt = question.lowercase()
        val suggestions = when {
            "theme" in prompt || "dark mode" in prompt || "light mode" in prompt || "appearance" in prompt -> "Open You → Appearance and choose System, Light, or Dark. Your choice is saved on this device."
            "gemini" in prompt || "connect" in prompt || "ai setup" in prompt -> "To enable live Gemini answers, start the proxy from the backend folder, then open You → API & AI and save its server address. Keep the API key in backend/.env, never in the app. On an Android emulator using the same computer, use http://10.0.2.2:3000; on a phone, use a reachable server address or hosted HTTPS URL."
            "save" in prompt || "favorite" in prompt || "favourite" in prompt -> "Browse in Explore and tap the heart on a product to save it. Open Saved to see your list later."
            "checkout" in prompt || "place order" in prompt || "order" in prompt -> "Add products to your Basket, open Basket, and choose Checkout to enter an address. This project records a demo order only; it doesn’t take payment or arrange delivery."
            "search" in prompt || "find product" in prompt || "browse" in prompt || "explore" in prompt -> "Open Explore to browse products. Use the search field and category chips to narrow the list, then tap a product for details."
            "help" in prompt || "guide" in prompt || "use this app" in prompt || "use the app" in prompt -> "Here’s a quick guide: Explore to search products, tap a heart to save one, use Basket to adjust quantities, and open You → Appearance to change the theme. Open You → API & AI to configure live Gemini answers."
            "breakfast" in prompt -> "For an easy breakfast, try farm fresh milk, golden bananas, and country sourdough."
            "snack" in prompt || "healthy" in prompt -> "A simple snack pairing: crisp apples with roasted almonds, or creamy avocado on sourdough."
            "basket" in prompt || "cart" in prompt -> if (cartItems.isEmpty()) "Your basket is empty right now. Browse fresh picks and tap Add to start one." else "You have $cartCount item(s) in your basket, around ₹${cartSubtotal.toInt()} before delivery."
            else -> "I can guide you around Angadi while offline: browse and search in Explore, save items with the heart button, manage Basket, or open You for theme and AI settings. For answers to general questions, connect Gemini in You → API & AI."
        }
        return suggestions
    }
}
