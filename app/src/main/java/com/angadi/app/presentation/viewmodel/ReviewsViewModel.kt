package com.angadi.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angadi.app.data.repository.ProfileRepository
import com.angadi.app.data.repository.ReviewRepository
import com.angadi.app.domain.model.RatingSummary
import com.angadi.app.domain.model.Review
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewsUiState(
    val reviews: List<Review> = emptyList(),
    val summary: RatingSummary = RatingSummary.of(emptyList()),
    val mine: Review? = null,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewsViewModel(private val repository: ReviewRepository, private val profile: ProfileRepository) : ViewModel() {
    private val productId = MutableStateFlow<Int?>(null)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<ReviewsUiState> = combine(
        productId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.reviews(id) }, error,
    ) { reviews, err ->
        // One review per product per device, so the only entry is by definition this device's.
        ReviewsUiState(reviews, RatingSummary.of(reviews), reviews.firstOrNull(), err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewsUiState())

    fun show(id: Int) {
        productId.value = id
        error.value = null
    }

    fun submit(rating: Int, body: String, onSuccess: () -> Unit = {}) {
        val id = productId.value ?: return
        viewModelScope.launch {
            val name = runCatching { profile.profile.first().name }.getOrDefault("")
            val problem = repository.submit(id, name, rating, body)
            error.update { problem }
            if (problem == null) onSuccess()
        }
    }

    fun delete(reviewId: Long) {
        viewModelScope.launch { repository.delete(reviewId) }
    }
}
