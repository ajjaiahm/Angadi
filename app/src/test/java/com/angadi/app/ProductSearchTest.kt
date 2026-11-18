package com.angadi.app

import com.angadi.app.domain.logic.ProductSearch
import com.angadi.app.domain.model.ProductQuery
import com.angadi.app.domain.model.SortOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductSearchTest {
    private val catalog = listOf(
        product(1, "Red apples", 190.0, "Fresh produce", 4.9, 40, description = "Sweet and juicy"),
        product(2, "Green apple juice", 120.0, "Pantry", 4.2, 5),
        product(3, "Whole milk", 68.0, "Dairy & eggs", 4.8, 0),
        product(4, "Sourdough loaf", 155.0, "Bakery", 4.6, 8, description = "Apple butter on top"),
        product(5, "Almonds", 249.0, "Pantry", 4.0, 22),
    )

    @Test fun textSearchMatchesTitleCategoryAndDescriptionCaseInsensitively() {
        val ids = ProductSearch.apply(catalog, ProductQuery(text = "APPLE")).map { it.id }.toSet()
        assertEquals(setOf(1, 2, 4), ids)
        assertEquals(listOf(3), ProductSearch.apply(catalog, ProductQuery(text = "dairy")).map { it.id })
    }

    @Test fun relevanceRanksTitlePrefixAboveDescriptionMatch() {
        val ids = ProductSearch.apply(catalog, ProductQuery(text = "apple")).map { it.id }
        assertEquals(1, ids.first())
        assertEquals(4, ids.last())
    }

    @Test fun noMatchesReturnsEmptyList() {
        assertTrue(ProductSearch.apply(catalog, ProductQuery(text = "zzz")).isEmpty())
    }

    @Test fun categoryAndPriceRangeFiltersCombine() {
        val q = ProductQuery(category = "Pantry", minPrice = 100.0, maxPrice = 200.0)
        assertEquals(listOf(2), ProductSearch.apply(catalog, q).map { it.id })
    }

    @Test fun ratingAndStockFilters() {
        assertEquals(setOf(1, 3), ProductSearch.apply(catalog, ProductQuery(minRating = 4.7)).map { it.id }.toSet())
        assertEquals(setOf(1, 2, 4, 5), ProductSearch.apply(catalog, ProductQuery(inStockOnly = true)).map { it.id }.toSet())
    }

    @Test fun filtersAndSortingWorkTogether() {
        val q = ProductQuery(text = "apple", minPrice = 100.0, sort = SortOption.PRICE_HIGH_LOW)
        assertEquals(listOf(1, 4, 2), ProductSearch.apply(catalog, q).map { it.id })
        val cheapOnly = q.copy(maxPrice = 150.0)
        assertEquals(listOf(2), ProductSearch.apply(catalog, cheapOnly).map { it.id })
    }

    @Test fun sortOptionsOrderCorrectly() {
        assertEquals(listOf(3, 2, 4, 1, 5), ProductSearch.apply(catalog, ProductQuery(sort = SortOption.PRICE_LOW_HIGH)).map { it.id })
        assertEquals(listOf(5, 1, 4, 2, 3), ProductSearch.apply(catalog, ProductQuery(sort = SortOption.PRICE_HIGH_LOW)).map { it.id })
        assertEquals(listOf(1, 3, 4, 2, 5), ProductSearch.apply(catalog, ProductQuery(sort = SortOption.RATING)).map { it.id })
    }

    @Test fun suggestionsPreferRecentSearchesThenCatalogue() {
        val s = ProductSearch.suggestions(catalog, listOf("apple pie", "milk"), "app")
        assertEquals("apple pie", s.first())
        assertTrue(s.contains("Red apples"))
        assertEquals(listOf("apple pie", "milk"), ProductSearch.suggestions(catalog, listOf("apple pie", "milk"), ""))
    }

    @Test fun activeFilterCountIgnoresTextAndSort() {
        assertEquals(0, ProductQuery(text = "x", sort = SortOption.RATING).activeFilterCount)
        assertEquals(3, ProductQuery(category = "Bakery", minPrice = 1.0, inStockOnly = true).activeFilterCount)
    }
}
