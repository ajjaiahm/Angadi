package com.angadi.app.domain.model

import kotlin.math.roundToInt

data class Product(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val unitPrice: Double,
    val discountPercent: Double = 0.0,
    val rating: Double = 4.7,
    val stock: Int = 20,
    val thumbnail: String = "",
    val unit: String = "each",
    val emoji: String = "🛍️",
    val isSampleApiPrice: Boolean = false,
) {
    val price: Double
        get() = (unitPrice * (100.0 - discountPercent.coerceIn(0.0, 90.0)) / 100.0).roundToInt().toDouble()
}

object SeedCatalog {
    val products = listOf(
        Product(9001, "Crisp red apples", "Sweet, juicy apples picked at their best. A lovely snack or lunchbox favourite.", "Fresh produce", 189.0, 10.0, 4.9, 42, unit = "1 kg", emoji = "🍎"),
        Product(9002, "Creamy avocados", "Ripe and ready for toast, salads, or a quick guacamole.", "Fresh produce", 129.0, 0.0, 4.8, 18, unit = "2 pcs", emoji = "🥑"),
        Product(9003, "Golden bananas", "Naturally sweet bananas, perfect for breakfast and smoothies.", "Fresh produce", 59.0, 0.0, 4.7, 36, unit = "6 pcs", emoji = "🍌"),
        Product(9004, "Farm fresh milk", "Smooth, rich milk from local dairy farms.", "Dairy & eggs", 68.0, 0.0, 4.8, 20, unit = "1 litre", emoji = "🥛"),
        Product(9005, "Free-range eggs", "Farm fresh eggs with golden yolks. Packed with care.", "Dairy & eggs", 112.0, 5.0, 4.9, 14, unit = "6 pcs", emoji = "🥚"),
        Product(9006, "Country sourdough", "Slow-fermented loaf with a golden crust and soft centre.", "Bakery", 155.0, 0.0, 4.8, 8, unit = "1 loaf", emoji = "🍞"),
        Product(9007, "Almonds, roasted", "Crunchy lightly roasted almonds for a wholesome everyday bite.", "Pantry", 249.0, 8.0, 4.6, 22, unit = "250 g", emoji = "🥜"),
        Product(9008, "Cherry tomatoes", "Bright, sweet tomatoes that bring colour to any meal.", "Fresh produce", 95.0, 0.0, 4.7, 16, unit = "250 g", emoji = "🍅"),
        Product(9009, "Extra virgin olive oil", "A fruity, versatile finishing oil for everyday cooking.", "Pantry", 549.0, 12.0, 4.8, 11, unit = "500 ml", emoji = "🫒"),
        Product(9010, "Tulsi green tea", "A gentle, aromatic tea for a slower afternoon.", "Pantry", 179.0, 0.0, 4.6, 27, unit = "20 bags", emoji = "🍵"),
        Product(9011, "Sweet strawberries", "Juicy seasonal berries for cereal, dessert, or straight from the punnet.", "Fresh produce", 149.0, 0.0, 4.8, 9, unit = "250 g", emoji = "🍓"),
        Product(9012, "Creamy peanut butter", "A smooth, satisfying spread made for toast and snacks.", "Pantry", 199.0, 0.0, 4.7, 15, unit = "340 g", emoji = "🥜"),
    )
}

fun productEmoji(title: String): String {
    val text = title.lowercase()
    return when {
        "apple" in text -> "🍎"
        "avocado" in text -> "🥑"
        "banana" in text -> "🍌"
        "milk" in text -> "🥛"
        "egg" in text -> "🥚"
        "bread" in text || "loaf" in text -> "🍞"
        "tomato" in text -> "🍅"
        "berry" in text || "strawber" in text -> "🍓"
        "tea" in text -> "🍵"
        "oil" in text -> "🫒"
        "almond" in text || "nut" in text -> "🥜"
        "fish" in text -> "🐟"
        "meat" in text || "chicken" in text -> "🍗"
        "orange" in text -> "🍊"
        "potato" in text -> "🥔"
        else -> "🛍️"
    }
}
