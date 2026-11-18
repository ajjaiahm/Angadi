package com.angadi.app.domain.model

enum class SortOption(val label: String) {
    RELEVANCE("Relevance"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    RATING("Rating"),
}

data class ProductQuery(
    val text: String = "",
    val category: String = "All",
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val minRating: Double = 0.0,
    val inStockOnly: Boolean = false,
    val sort: SortOption = SortOption.RELEVANCE,
) {
    val activeFilterCount: Int
        get() = listOf(category != "All", minPrice != null, maxPrice != null, minRating > 0.0, inStockOnly).count { it }
}
