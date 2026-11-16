package com.angadi.app.data.remote

import com.angadi.app.domain.model.Product
import com.angadi.app.domain.model.productEmoji
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

class DummyJsonCatalogService {
    fun fetchGroceries(): List<Product> {
        val connection = URL("https://dummyjson.com/products/category/groceries?limit=30").openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.requestMethod = "GET"
        return try {
            if (connection.responseCode !in 200..299) error("Catalog service returned ${connection.responseCode}")
            val json = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            val products = json.optJSONArray("products") ?: return emptyList()
            buildList {
                for (index in 0 until products.length()) {
                    val item = products.optJSONObject(index) ?: continue
                    val title = item.optString("title", "Grocery item")
                    val sourcePrice = item.optDouble("price", 0.0)
                    // DummyJSON is placeholder data in USD. This rough INR conversion is for the demo feed only.
                    val demoPriceInr = (sourcePrice * DEMO_USD_TO_INR).roundToInt().coerceAtLeast(1).toDouble()
                    add(Product(
                        id = item.optInt("id"),
                        title = title,
                        description = item.optString("description", "A pantry pick for your everyday table."),
                        category = "Groceries",
                        unitPrice = demoPriceInr,
                        discountPercent = item.optDouble("discountPercentage", 0.0).coerceIn(0.0, 50.0),
                        rating = item.optDouble("rating", 4.6),
                        stock = item.optInt("stock", 10),
                        thumbnail = item.optString("thumbnail", ""),
                        unit = "each",
                        emoji = productEmoji(title),
                        isSampleApiPrice = true,
                    ))
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val DEMO_USD_TO_INR = 85.0
    }
}
