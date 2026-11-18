package com.angadi.app.domain.logic

import com.angadi.app.domain.model.Product
import com.angadi.app.domain.model.ProductQuery
import com.angadi.app.domain.model.SortOption

object ProductSearch {
    /** Applies text search, filters and sorting together. Pure and side-effect free. */
    fun apply(products: List<Product>, query: ProductQuery): List<Product> {
        val text = query.text.trim()
        val filtered = products.filter { p ->
            (text.isEmpty() || score(p, text) > 0) &&
                (query.category == "All" || p.category.equals(query.category, ignoreCase = true)) &&
                (query.minPrice == null || p.price >= query.minPrice) &&
                (query.maxPrice == null || p.price <= query.maxPrice) &&
                p.rating >= query.minRating &&
                (!query.inStockOnly || p.stock > 0)
        }
        return when (query.sort) {
            SortOption.RELEVANCE -> if (text.isEmpty()) filtered else filtered.sortedByDescending { score(it, text) }
            SortOption.PRICE_LOW_HIGH -> filtered.sortedBy { it.price }
            SortOption.PRICE_HIGH_LOW -> filtered.sortedByDescending { it.price }
            SortOption.RATING -> filtered.sortedByDescending { it.rating }
        }
    }

    private fun score(product: Product, text: String): Int {
        val title = product.title.lowercase()
        val needle = text.lowercase()
        var score = 0
        if (title.startsWith(needle)) score += 6
        if (title.split(' ').any { it.startsWith(needle) }) score += 4
        if (title.contains(needle)) score += 3
        if (product.category.contains(needle, ignoreCase = true)) score += 2
        if (product.description.contains(needle, ignoreCase = true)) score += 1
        return score
    }

    /** Suggestions: recent searches that match first, then product titles and categories. */
    fun suggestions(products: List<Product>, recent: List<String>, text: String, limit: Int = 6): List<String> {
        val needle = text.trim().lowercase()
        if (needle.isEmpty()) return recent.take(limit)
        val fromRecent = recent.filter { it.lowercase().contains(needle) && it.lowercase() != needle }
        val fromCatalog = (products.map { it.title } + products.map { it.category }.distinct())
            .filter { it.lowercase().contains(needle) && it.lowercase() != needle }
        return (fromRecent + fromCatalog).distinctBy { it.lowercase() }.take(limit)
    }
}
