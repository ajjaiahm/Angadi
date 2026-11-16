package com.angadi.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.angadi.app.data.local.AngadiDatabase
import com.angadi.app.data.local.OrderSummary
import com.angadi.app.data.remote.DummyJsonCatalogService
import com.angadi.app.data.remote.GeminiAssistantService
import com.angadi.app.domain.model.Product

class AngadiRepository(context: Context) {
    private val database = AngadiDatabase(context)
    private val preferences: SharedPreferences = context.getSharedPreferences("angadi_preferences", Context.MODE_PRIVATE)
    private val catalogService = DummyJsonCatalogService()
    private val assistantService = GeminiAssistantService()

    fun loadCatalog(): List<Product> = catalogService.fetchGroceries()
    fun loadCart(): Map<Int, Int> = database.loadCart()
    fun saveCart(items: Map<Int, Int>) = database.saveCart(items)
    fun loadFavorites(): Set<Int> = database.loadFavorites()
    fun saveFavorites(ids: Set<Int>) = database.saveFavorites(ids)
    fun createOrder(total: Double, itemCount: Int) = database.createOrder(total, itemCount)
    fun loadOrders(): List<OrderSummary> = database.loadOrders()
    fun getAssistantServer(): String = preferences.getString("assistant_server", "").orEmpty()
    fun saveAssistantServer(url: String) { preferences.edit().putString("assistant_server", url.trim().trimEnd('/')).apply() }
    fun getThemeMode(): String = preferences.getString("theme_mode", "SYSTEM").orEmpty()
    fun saveThemeMode(mode: String) { preferences.edit().putString("theme_mode", mode).apply() }
    fun askAssistant(url: String, messages: List<Pair<Boolean, String>>, context: String): String = assistantService.ask(url, messages, context)
    fun checkAssistantServer(url: String): String = assistantService.checkServer(url)
}
