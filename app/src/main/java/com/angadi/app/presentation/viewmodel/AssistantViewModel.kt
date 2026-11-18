package com.angadi.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angadi.app.data.repository.AssistantRepository
import com.angadi.app.data.repository.SettingsRepository
import com.angadi.app.data.repository.ShopRepository
import com.angadi.app.domain.logic.CartLine
import com.angadi.app.domain.logic.PricingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssistantMessage(val fromAssistant: Boolean, val text: String)

data class AssistantUiState(
    val messages: List<AssistantMessage> = listOf(AssistantMessage(true, WELCOME)),
    val input: String = "",
    val isThinking: Boolean = false,
    val server: String = "",
    val serverDraft: String = "",
    val serverStatus: String = "Assistant server not connected",
    val isCheckingServer: Boolean = false,
) {
    companion object {
        const val WELCOME = "Hi! I’m Angadi AI. I can guide you around the app: browse in Explore, tap a heart to save an item, add products to your Basket, and open You to change the theme, manage addresses or connect Gemini. Once Gemini is connected, you can also ask me general questions."
    }
}

class AssistantViewModel(
    private val assistant: AssistantRepository,
    private val settings: SettingsRepository,
    private val shop: ShopRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AssistantUiState(server = settings.assistantServer.value, serverDraft = settings.assistantServer.value))
    val state: StateFlow<AssistantUiState> = _state.asStateFlow()

    fun setInput(text: String) = _state.update { it.copy(input = text) }
    fun setServerDraft(text: String) = _state.update { it.copy(serverDraft = text) }

    fun saveServer() {
        val value = _state.value.serverDraft.trim().trimEnd('/')
        settings.setAssistantServer(value)
        _state.update {
            it.copy(
                server = value, serverDraft = value,
                serverStatus = if (value.isBlank()) "Assistant server not connected" else "Server address saved. Tap Test connection.",
            )
        }
    }

    fun testServer() {
        val url = _state.value.serverDraft.trim().trimEnd('/')
        if (url.isBlank()) {
            _state.update { it.copy(serverStatus = "Enter your API server address first") }
            return
        }
        _state.update { it.copy(isCheckingServer = true, serverStatus = "Checking connection…") }
        viewModelScope.launch {
            val result = runCatching { assistant.check(url) }.getOrElse { "Connection failed: ${it.message ?: "check the server address"}" }
            _state.update { it.copy(isCheckingServer = false, serverStatus = result) }
        }
    }

    fun send(textOverride: String? = null) {
        val current = _state.value
        val text = (textOverride ?: current.input).trim()
        if (text.isBlank() || current.isThinking) return
        val withUser = current.messages + AssistantMessage(false, text)
        if (current.server.isBlank()) {
            viewModelScope.launch {
                val reply = localReply(text)
                _state.update { it.copy(input = "", messages = withUser + AssistantMessage(true, reply)) }
            }
            return
        }
        _state.update { it.copy(input = "", messages = withUser, isThinking = true) }
        viewModelScope.launch {
            val reply = runCatching {
                assistant.ask(current.server, withUser.map { it.fromAssistant to it.text }, buildContext())
            }.getOrElse { "I couldn’t reach the AI assistant right now. Check the API server connection in your profile and try again. (${it.message ?: "network error"})" }
            _state.update { it.copy(messages = it.messages + AssistantMessage(true, reply), isThinking = false) }
        }
    }

    private suspend fun buildContext(): String {
        val products = shop.catalog.value
        val cart = shop.cart.first()
        val lines = cart.mapNotNull { (id, qty) -> products.firstOrNull { it.id == id }?.let { CartLine(it, qty) } }
        return buildString {
            appendLine("Current catalogue (sample data): ${products.take(24).joinToString { "${it.title} at ₹${it.price.toInt()}" }}")
            appendLine("Basket: ${lines.joinToString { "${it.quantity} × ${it.product.title}" }.ifBlank { "empty" }}")
            append("Prices, inventory, and order records in this app are demo data.")
        }
    }

    private suspend fun localReply(question: String): String {
        val prompt = question.lowercase()
        return when {
            "theme" in prompt || "dark mode" in prompt || "light mode" in prompt || "appearance" in prompt -> "Open You → Appearance and choose System, Light, or Dark. Your choice is saved on this device."
            "gemini" in prompt || "connect" in prompt || "ai setup" in prompt -> "To enable live Gemini answers, start the proxy from the backend folder, then open You → API & AI and save its server address. Keep the API key in backend/.env, never in the app. On an Android emulator using the same computer, use http://10.0.2.2:3000; on a phone, use a reachable server address or hosted HTTPS URL."
            "address" in prompt -> "Open You → Saved addresses to add, edit, delete or choose a default address. Checkout uses your default address unless you pick another."
            "coupon" in prompt || "discount" in prompt || "offer" in prompt -> "At checkout, open Coupons, pick one of the offers or type a code. Offers have minimum basket values and expiry dates, which checkout explains if a code can't be used."
            "track" in prompt || "reorder" in prompt || "order" in prompt && "history" in prompt -> "Open You → Your orders to see order history and progress. Open an order to see items and its status timeline, or tap Reorder to add its items to your basket at today's prices."
            "save" in prompt || "favorite" in prompt || "favourite" in prompt -> "Browse in Explore and tap the heart on a product to save it. Open Saved to see your list later."
            "checkout" in prompt || "place order" in prompt || "order" in prompt -> "Add products to your Basket, open Basket, and choose Checkout to pick an address, delivery slot and coupon. This project records a demo order only; it doesn’t take payment or arrange delivery."
            "search" in prompt || "find product" in prompt || "browse" in prompt || "explore" in prompt -> "Open Explore to browse products. Use the search field, filters and sort to narrow the list, then tap a product for details."
            "help" in prompt || "guide" in prompt || "use this app" in prompt || "use the app" in prompt -> "Here’s a quick guide: Explore to search products, tap a heart to save one, use Basket to adjust quantities, and open You for orders, addresses, appearance and AI settings."
            "breakfast" in prompt -> "For an easy breakfast, try farm fresh milk, golden bananas, and country sourdough."
            "snack" in prompt || "healthy" in prompt -> "A simple snack pairing: crisp apples with roasted almonds, or creamy avocado on sourdough."
            "basket" in prompt || "cart" in prompt -> {
                val cart = shop.cart.first()
                val lines = cart.mapNotNull { (id, qty) -> shop.catalog.value.firstOrNull { it.id == id }?.let { CartLine(it, qty) } }
                if (lines.isEmpty()) "Your basket is empty right now. Browse fresh picks and tap Add to start one."
                else "You have ${lines.sumOf { it.quantity }} item(s) in your basket, around ₹${PricingEngine.subtotal(lines).toInt()} before delivery."
            }
            else -> "I can guide you around Angadi while offline: browse and search in Explore, save items with the heart button, manage Basket, or open You for orders, addresses and settings. For answers to general questions, connect Gemini in You → API & AI."
        }
    }
}
