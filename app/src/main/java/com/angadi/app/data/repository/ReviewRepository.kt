package com.angadi.app.data.repository

import com.angadi.app.data.local.AngadiDatabase
import com.angadi.app.data.local.ReviewEntity
import com.angadi.app.domain.model.Review
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReviewRepository(private val db: AngadiDatabase, private val clock: () -> Long = System::currentTimeMillis) {
    private val dao get() = db.reviewDao()

    fun reviews(productId: Int): Flow<List<Review>> = dao.observe(productId).map { rows -> rows.map { it.toDomain() } }

    /** productId -> (average, count) over reviews written on this device. */
    val summaries: Flow<Map<Int, Pair<Double, Int>>> = dao.observeSummaries().map { rows -> rows.associate { it.productId to (it.average to it.total) } }

    /** One review per product on this device. Submitting again edits the existing one. Returns an error message or null. */
    suspend fun submit(productId: Int, authorName: String, rating: Int, body: String): String? {
        val text = body.trim()
        if (rating !in 1..5) return "Choose a star rating from 1 to 5."
        if (text.length < MIN_BODY) return "Write at least $MIN_BODY characters."
        if (text.length > MAX_BODY) return "Reviews can be up to $MAX_BODY characters."
        val now = clock()
        val existing = dao.forProduct(productId)
        dao.upsert(
            ReviewEntity(
                id = existing?.id ?: 0, productId = productId, authorName = authorName.trim().ifBlank { "You" },
                rating = rating, body = text, createdAt = existing?.createdAt ?: now, updatedAt = now,
            ),
        )
        return null
    }

    suspend fun delete(reviewId: Long) = dao.delete(reviewId)

    companion object {
        const val MIN_BODY = 3
        const val MAX_BODY = 500
    }
}

private fun ReviewEntity.toDomain() = Review(id, productId, authorName, rating, body, createdAt, updatedAt)
