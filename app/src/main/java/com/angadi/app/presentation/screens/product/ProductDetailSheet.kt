package com.angadi.app.presentation.screens.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angadi.app.data.repository.ReviewRepository
import com.angadi.app.domain.logic.CartRules
import com.angadi.app.domain.model.Product
import com.angadi.app.presentation.components.DiscountTag
import com.angadi.app.presentation.components.ProductArtwork
import com.angadi.app.presentation.components.QuantityControl
import com.angadi.app.presentation.components.RoundAction
import com.angadi.app.presentation.components.StarRow
import com.angadi.app.presentation.components.formatPrice
import com.angadi.app.presentation.components.formatRating
import com.angadi.app.presentation.screens.orders.formatDateTime
import com.angadi.app.presentation.viewmodel.ReviewsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailSheet(
    product: Product,
    isFavorite: Boolean,
    reviews: ReviewsUiState,
    onClose: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAdd: (Int) -> Unit,
    onSubmitReview: (Int, String, () -> Unit) -> Unit,
    onDeleteReview: (Long) -> Unit,
) {
    var quantity by remember(product.id) { mutableIntStateOf(1) }
    val maxQuantity = CartRules.maxQuantity(product)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        modifier = Modifier.testTag("product_sheet"),
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 26.dp)) {
            Box {
                ProductArtwork(product.thumbnail, product.emoji, product.category, product.title, Modifier.fillMaxWidth().height(260.dp), emojiSize = 90)
                RoundAction("×", "Close product", modifier = Modifier.align(Alignment.TopEnd).padding(14.dp).heightIn(min = 48.dp), onClick = onClose)
                if (product.discountPercent > 0) DiscountTag(product.discountPercent, Modifier.align(Alignment.TopStart).padding(14.dp))
            }
            Column(Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
                Text(product.category.uppercase(), fontSize = 10.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(5.dp))
                Text(product.title, Modifier.testTag("sheet_title"), fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("★ ${formatRating(product.rating)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFAB7818))
                    Text("  ·  ${product.unit}  ·  " + if (product.stock > 0) "${product.stock} in stock" else "Out of stock", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(13.dp))
                Text(product.description, fontSize = 14.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(formatPrice(product.price), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        if (product.discountPercent > 0) Text(formatPrice(product.unitPrice), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textDecoration = TextDecoration.LineThrough)
                    }
                    OutlinedButton(onClick = onToggleFavorite, shape = RoundedCornerShape(13.dp), modifier = Modifier.heightIn(min = 48.dp).testTag("sheet_favorite")) {
                        Text(if (isFavorite) "♥ Saved" else "♡ Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(14.dp))
                if (maxQuantity > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Quantity", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        QuantityControl(quantity, onMinus = { quantity = (quantity - 1).coerceAtLeast(1) }, onPlus = { quantity = (quantity + 1).coerceAtMost(maxQuantity) }, productName = product.title)
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Button(
                    onClick = { onAdd(quantity); onClose() }, enabled = maxQuantity > 0,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("sheet_add"), shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) { Text(if (maxQuantity > 0) "Add to basket  ·  ${formatPrice(product.price * quantity)}" else "Out of stock", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(18.dp))
                ReviewsSection(product, reviews, onSubmitReview, onDeleteReview)
            }
        }
    }
}

@Composable
private fun ReviewsSection(product: Product, state: ReviewsUiState, onSubmit: (Int, String, () -> Unit) -> Unit, onDelete: (Long) -> Unit) {
    val mine = state.mine
    var editing by remember(product.id) { mutableStateOf(false) }
    var rating by remember(product.id, mine?.id, editing) { mutableIntStateOf(mine?.rating ?: 0) }
    var body by remember(product.id, mine?.id, editing) { mutableStateOf(mine?.body.orEmpty()) }
    val summary = state.summary

    Column(Modifier.testTag("reviews_section"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Reviews", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            "Reviews are written on this device only. They aren't tied to an account or a verified purchase.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (summary.count == 0) {
            Text("No reviews yet. Be the first to share your thoughts.", Modifier.testTag("no_reviews"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatRating(summary.average), Modifier.testTag("review_average"), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                    StarRow(summary.average)
                    Text("${summary.count} review(s)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    (5 downTo 1).forEach { star ->
                        val count = summary.distribution[star] ?: 0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$star★", fontSize = 11.sp, modifier = Modifier.width(26.dp))
                            LinearProgressIndicator(progress = { if (summary.count == 0) 0f else count.toFloat() / summary.count }, modifier = Modifier.weight(1f).height(6.dp))
                            Text("$count", fontSize = 11.sp, modifier = Modifier.width(24.dp).padding(start = 6.dp))
                        }
                    }
                }
            }
            state.reviews.forEach { review ->
                Column(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background, RoundedCornerShape(14.dp)).padding(12.dp).testTag("review_${review.id}"),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StarRow(review.rating.toDouble())
                        Text("  ${review.authorName} · ${formatDateTime(review.updatedAt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(review.body, fontSize = 13.sp)
                    Row {
                        TextButton(onClick = { editing = true }, modifier = Modifier.heightIn(min = 48.dp).testTag("edit_review")) { Text("Edit") }
                        TextButton(onClick = { onDelete(review.id) }, modifier = Modifier.heightIn(min = 48.dp).testTag("delete_review")) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        if (mine == null || editing) {
            Text(if (mine == null) "Write a review" else "Edit your review", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                (1..5).forEach { star ->
                    TextButton(
                        onClick = { rating = star },
                        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Rate $star star${if (star > 1) "s" else ""}" }.testTag("star_$star"),
                    ) { Text(if (star <= rating) "★" else "☆", fontSize = 26.sp, color = Color(0xFFAB7818)) }
                }
            }
            OutlinedTextField(
                value = body, onValueChange = { body = it.take(ReviewRepository.MAX_BODY) }, modifier = Modifier.fillMaxWidth().testTag("review_body"),
                label = { Text("Your review") }, minLines = 2, maxLines = 5, shape = RoundedCornerShape(14.dp),
                supportingText = { Text("${body.length}/${ReviewRepository.MAX_BODY}") },
            )
            state.error?.let { Text(it, Modifier.testTag("review_error"), color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            Button(
                onClick = { onSubmit(rating, body) { editing = false; body = ""; rating = 0 } },
                modifier = Modifier.heightIn(min = 48.dp).testTag("submit_review"),
            ) { Text(if (mine == null) "Submit review" else "Save changes") }
        }
        LaunchedEffect(mine?.id) { if (mine != null) editing = false }
    }
}
