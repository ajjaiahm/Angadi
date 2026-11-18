package com.angadi.app.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angadi.app.domain.logic.PricingEngine
import com.angadi.app.domain.model.CouponCatalog
import com.angadi.app.presentation.components.ProductActions
import com.angadi.app.presentation.components.ProductGrid
import com.angadi.app.presentation.components.ProductShelf
import com.angadi.app.presentation.components.RoundAction
import com.angadi.app.presentation.components.SampleDataFootnote
import com.angadi.app.presentation.components.SectionHeading
import com.angadi.app.presentation.components.categoryEmoji
import com.angadi.app.presentation.components.formatPrice
import com.angadi.app.presentation.viewmodel.ShopUiState

class HomeCallbacks(
    val onSearch: () -> Unit,
    val onCategory: (String) -> Unit,
    val onExplore: () -> Unit,
    val onAssistant: () -> Unit,
    val onSaved: () -> Unit,
    val onBasket: () -> Unit,
    val onAddresses: () -> Unit,
    val onClearRecent: () -> Unit,
    val onCoupons: () -> Unit,
)

@Composable
fun HomeScreen(state: ShopUiState, categories: List<String>, defaultAddressLine: String?, actions: ProductActions, callbacks: HomeCallbacks) {
    // "Popular" uses the catalogue's own rating because the sample API has no sales data.
    val topRated = state.products.sortedByDescending { it.rating }.take(10)
    val deals = state.products.filter { it.discountPercent >= 5.0 }.sortedByDescending { it.discountPercent }.take(10)
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("home_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { HomeHeader(state, callbacks) }
        item { DeliveryAddressPill(defaultAddressLine, callbacks.onAddresses) }
        item { SearchEntry(callbacks.onSearch) }
        item { HeroBanner(onShop = { callbacks.onCategory("Fresh produce") }) }
        item { OffersStrip(callbacks.onCoupons) }
        item { SectionHeading("Shop by category") }
        item { CategoryShelf(categories.filter { it != "All" }, callbacks.onCategory) }
        if (state.recentProducts.isNotEmpty()) {
            item { SectionHeading("Recently viewed", "Clear", callbacks.onClearRecent) }
            item { Box(Modifier.testTag("recently_viewed")) { ProductShelf(state.recentProducts, actions) } }
        }
        if (deals.isNotEmpty()) {
            item { SectionHeading("Deals for you", "View all", callbacks.onExplore) }
            item { ProductShelf(deals, actions) }
        }
        item { SectionHeading("Top rated", "View all", callbacks.onExplore) }
        item { ProductShelf(topRated, actions) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Fresh picks, just for you", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (state.isRefreshing) CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (state.catalogError != null) item { Text(state.catalogError, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { ProductGrid(state.products.take(8), actions) }
        item { SampleDataFootnote() }
    }
}

@Composable
private fun HomeHeader(state: ShopUiState, callbacks: HomeCallbacks) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("angadi", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, letterSpacing = (-1.4).sp)
                Spacer(Modifier.width(5.dp))
                Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
            }
            Text(state.profile.greeting, Modifier.testTag("home_greeting"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RoundAction("✦", "Open Angadi AI", onClick = callbacks.onAssistant)
        Spacer(Modifier.width(8.dp))
        RoundAction("♡", "Saved items", badge = state.favorites.size, onClick = callbacks.onSaved)
        Spacer(Modifier.width(8.dp))
        RoundAction("▤", "Basket", badge = state.cartCount, onClick = callbacks.onBasket)
    }
}

@Composable
private fun DeliveryAddressPill(addressLine: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick).heightIn(min = 48.dp).padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⌖", fontSize = 19.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text("DELIVERING TO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            Text(addressLine ?: "Add a delivery address", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SearchEntry(onSearch: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)).clickable { onSearch() }
            .padding(horizontal = 15.dp).testTag("home_search"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⌕", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(9.dp))
        Text("Search fruit, pantry, and more", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HeroBanner(onShop: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 180.dp).clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { onShop() }.padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("THE EVERYDAY GOOD", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("A little fresh,\na lot closer.", fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(7.dp))
            Text("Good things for your table.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(13.dp))
            Button(
                onClick = onShop,
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("Shop fresh picks", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        }
        Box(Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Text("🥬", fontSize = 59.sp)
        }
    }
}

@Composable
private fun OffersStrip(onCoupons: () -> Unit) {
    val offers = CouponCatalog.all.filter { it.expiresAt == null || it.expiresAt > System.currentTimeMillis() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeading("Offers", "At checkout", onCoupons)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(offers, key = { it.code }) { coupon ->
                Column(
                    Modifier.width(210.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable(onClick = onCoupons).padding(14.dp),
                ) {
                    Text(coupon.code, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSecondaryContainer, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(coupon.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text("Min. basket ${formatPrice(coupon.minOrderValue)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                }
            }
            item {
                Column(
                    Modifier.width(190.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp),
                ) {
                    Text("Free delivery", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text("on baskets of ${formatPrice(PricingEngine.FREE_DELIVERY_THRESHOLD)}+", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
                }
            }
        }
    }
}

@Composable
private fun CategoryShelf(categories: List<String>, onCategory: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 10.dp)) {
        items(categories, key = { it }) { category ->
            Column(
                Modifier.width(84.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onCategory(category) }.padding(vertical = 13.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(categoryEmoji(category), fontSize = 27.sp)
                Spacer(Modifier.height(7.dp))
                Text(category, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 13.sp)
            }
        }
    }
}
