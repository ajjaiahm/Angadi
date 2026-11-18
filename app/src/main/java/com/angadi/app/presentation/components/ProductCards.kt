package com.angadi.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angadi.app.domain.model.Product

/** What a product card can do. Keeps cards free of any ViewModel dependency. */
class ProductActions(
    val favorites: Set<Int>,
    val ratingOverrides: Map<Int, Pair<Double, Int>> = emptyMap(),
    val onOpen: (Product) -> Unit,
    val onAdd: (Product) -> Unit,
    val onToggleFavorite: (Product) -> Unit,
)

@Composable
fun ProductShelf(products: List<Product>, actions: ProductActions) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 12.dp)) {
        items(products, key = { it.id }) { product -> ProductCard(product, actions, Modifier.width(176.dp)) }
    }
}

@Composable
fun ProductGrid(products: List<Product>, actions: ProductActions) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        products.chunked(2).forEach { row -> ProductGridRow(row, actions) }
    }
}

@Composable
fun ProductGridRow(products: List<Product>, actions: ProductActions) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        products.forEach { product -> ProductCard(product, actions, Modifier.weight(1f)) }
        if (products.size == 1) Spacer(Modifier.weight(1f))
    }
}

@Composable
fun ProductCard(product: Product, actions: ProductActions, modifier: Modifier = Modifier) {
    val isFavorite = product.id in actions.favorites
    val outOfStock = product.stock <= 0
    Card(
        modifier = modifier.testTag("product_card_${product.id}").clickable { actions.onOpen(product) },
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
    ) {
        Column(Modifier.padding(9.dp)) {
            Box(Modifier.fillMaxWidth()) {
                ProductArtwork(product.thumbnail, product.emoji, product.category, product.title, Modifier.fillMaxWidth().height(137.dp).clip(RoundedCornerShape(14.dp)), emojiSize = 54)
                if (product.discountPercent > 0) DiscountTag(product.discountPercent, Modifier.align(Alignment.TopStart).padding(7.dp))
                RoundAction(
                    if (isFavorite) "♥" else "♡",
                    if (isFavorite) "Remove ${product.title} from saved" else "Save ${product.title}",
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(44.dp),
                    isActive = isFavorite,
                ) { actions.onToggleFavorite(product) }
            }
            Spacer(Modifier.height(9.dp))
            Text(product.category, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(product.title, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatPrice(product.price), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                val review = actions.ratingOverrides[product.id]
                val rating = review?.first ?: product.rating
                Text("★ ${formatRating(rating)}", fontSize = 10.sp, color = Color(0xFF9B711C))
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { actions.onAdd(product) },
                enabled = !outOfStock,
                modifier = Modifier.fillMaxWidth().height(44.dp).semantics { contentDescription = "Add ${product.title} to basket" },
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) { Text(if (outOfStock) "Out of stock" else "+  Add", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun QuantityControl(count: Int, onMinus: () -> Unit, onPlus: () -> Unit, productName: String = "item") {
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallStepButton("−", "Remove one $productName", onMinus)
        Text(count.toString(), Modifier.width(27.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        SmallStepButton("+", "Add one $productName", onPlus)
    }
}

@Composable
private fun SmallStepButton(symbol: String, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(9.dp), color = Color.Transparent, modifier = Modifier.size(44.dp).semantics { contentDescription = label }) {
        Box(contentAlignment = Alignment.Center) { Text(symbol, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
    }
}
