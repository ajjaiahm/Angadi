package com.angadi.app

import com.angadi.app.domain.model.Product
import java.time.ZoneId
import java.time.ZonedDateTime

val TEST_ZONE: ZoneId = ZoneId.of("Asia/Kolkata")
val TEST_NOW: ZonedDateTime = ZonedDateTime.of(2026, 11, 18, 9, 0, 0, 0, TEST_ZONE)
val TEST_NOW_MILLIS: Long = TEST_NOW.toInstant().toEpochMilli()

fun product(
    id: Int, title: String = "Product $id", price: Double = 100.0, category: String = "Pantry", rating: Double = 4.5,
    stock: Int = 10, discount: Double = 0.0, description: String = "",
) = Product(id = id, title = title, description = description, category = category, unitPrice = price, discountPercent = discount, rating = rating, stock = stock)
