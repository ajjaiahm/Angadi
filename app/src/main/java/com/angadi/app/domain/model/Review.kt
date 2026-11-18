package com.angadi.app.domain.model

/** A review written on this device. Reviews are not tied to an account or to verified purchases. */
data class Review(
    val id: Long,
    val productId: Int,
    val authorName: String,
    val rating: Int,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class RatingSummary(val average: Double, val count: Int, val distribution: Map<Int, Int>) {
    companion object {
        fun of(reviews: List<Review>): RatingSummary {
            val dist = (5 downTo 1).associateWith { star -> reviews.count { it.rating == star } }
            val avg = if (reviews.isEmpty()) 0.0 else reviews.sumOf { it.rating }.toDouble() / reviews.size
            return RatingSummary(avg, reviews.size, dist)
        }
    }
}
