package com.angadi.app.presentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.angadi.app.data.local.OrderSummary
import com.angadi.app.domain.model.Product
import com.angadi.app.ui.theme.AppThemeMode

private enum class MainRoute { HOME, EXPLORE, SAVED, CART, PROFILE, ASSISTANT, CHECKOUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AngadiApp(store: AngadiStore) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(store.toastMessage) {
        store.toastMessage?.let {
            snackbar.showSnackbar(it)
            store.clearToast()
        }
    }
    val route = when {
        store.showAssistant -> MainRoute.ASSISTANT
        store.showCheckout -> MainRoute.CHECKOUT
        else -> when (store.activeTab) {
            StoreTab.HOME -> MainRoute.HOME
            StoreTab.EXPLORE -> MainRoute.EXPLORE
            StoreTab.SAVED -> MainRoute.SAVED
            StoreTab.CART -> MainRoute.CART
            StoreTab.PROFILE -> MainRoute.PROFILE
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!store.showAssistant && !store.showCheckout) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, windowInsets = WindowInsets.navigationBars) {
                    StoreTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = store.activeTab == tab,
                            onClick = { store.navigate(tab) },
                            icon = {
                                Box {
                                    Text(tab.symbol, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                                    if (tab == StoreTab.CART && store.cartCount > 0) {
                                        Box(
                                            Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-5).dp)
                                                .size(16.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary),
                                            contentAlignment = Alignment.Center,
                                        ) { Text(store.cartCount.coerceAtMost(99).toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
                                    }
                                }
                            },
                            label = { Text(tab.label, fontSize = 10.sp) },
                            alwaysShowLabel = true,
                        )
                    }
                }
            }
        },
    ) { insets ->
        AnimatedContent(
            targetState = route,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(130)) },
            label = "Angadi screen transition",
            modifier = Modifier.padding(insets),
        ) { screen ->
            when (screen) {
                MainRoute.HOME -> HomeScreen(store)
                MainRoute.EXPLORE -> ExploreScreen(store)
                MainRoute.SAVED -> SavedScreen(store)
                MainRoute.CART -> CartScreen(store)
                MainRoute.PROFILE -> ProfileScreen(store)
                MainRoute.ASSISTANT -> AssistantScreen(store)
                MainRoute.CHECKOUT -> CheckoutScreen(store)
            }
        }
    }
    store.selectedProduct?.let { product ->
        ProductDetailSheet(product, store)
    }
}

@Composable
private fun HomeScreen(store: AngadiStore) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { HomeHeader(store) }
        item { DeliveryAddressPill() }
        item { SearchBar(value = store.query, onValueChange = { store.query = it }, onSearch = { store.navigate(StoreTab.EXPLORE) }) }
        item { HeroBanner(onShop = { store.setCategory("Fresh produce") }, onAssistant = { store.showAssistant = true }) }
        item { SectionHeading("Shop by category", "See all", { store.navigate(StoreTab.EXPLORE) }) }
        item { CategoryShelf(store) }
        item { SectionHeading("Popular right now", "View all", { store.navigate(StoreTab.EXPLORE) }) }
        item { ProductShelf(store, store.products.take(10)) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Fresh picks, just for you", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (store.isRefreshing) CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
            }
        }
        item { ProductGrid(store, store.products.take(8)) }
        item { SampleDataFootnote() }
    }
}

@Composable
private fun HomeHeader(store: AngadiStore) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("angadi", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, letterSpacing = (-1.4).sp)
                Spacer(Modifier.width(5.dp))
                Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
            }
            Text("Good food, good mood.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RoundAction("✦", "Open Angadi AI") { store.showAssistant = true }
        Spacer(Modifier.width(9.dp))
        RoundAction("♡", "Saved items", badge = store.favorites.size) { store.navigate(StoreTab.SAVED) }
        Spacer(Modifier.width(9.dp))
        RoundAction("▤", "Basket", badge = store.cartCount) { store.navigate(StoreTab.CART) }
    }
}

@Composable
private fun DeliveryAddressPill() {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⌖", fontSize = 19.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text("DELIVERING TO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            Text("Choose your delivery address", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
        Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SearchBar(value: String, onValueChange: (String) -> Unit, onSearch: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)).clickable { onSearch() }
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⌕", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(9.dp))
        Text("Search fruit, pantry, and more", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text("↵", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun HeroBanner(onShop: () -> Unit, onAssistant: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 180.dp).clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
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
            ) { Text("Shop fresh picks", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        }
        Box(Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Text("🥬", fontSize = 59.sp)
        }
    }
}

@Composable
private fun SectionHeading(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text(action, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CategoryShelf(store: AngadiStore) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 10.dp)) {
        items(store.categoryNames.drop(1), key = { it }) { category ->
            val visual = categoryVisual(category)
            Column(
                Modifier.width(84.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { store.setCategory(category) }.padding(vertical = 13.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(visual.first, fontSize = 27.sp)
                Spacer(Modifier.height(7.dp))
                Text(category, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 13.sp)
            }
        }
    }
}

@Composable
private fun ProductShelf(store: AngadiStore, products: List<Product>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 12.dp)) {
        items(products, key = { it.id }) { product ->
            ProductCard(product, store, Modifier.width(176.dp))
        }
    }
}

@Composable
private fun ExploreScreen(store: AngadiStore) {
    val shown = store.visibleProducts()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader("Explore", "Find something lovely for your table", store) }
        item {
            OutlinedTextField(
                value = store.query,
                onValueChange = { store.query = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                label = { Text("Search products") },
                leadingIcon = { Text("⌕", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            )
        }
        item { CategoryFilter(store) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${shown.size} good finds", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { store.sortByPrice = !store.sortByPrice }) {
                    Text(if (store.sortByPrice) "Price: low first ↑" else "Sort by price ⇅", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
            }
        }
        if (shown.isEmpty()) {
            item { EmptyState("No matches just yet", "Try a different search or category.", "⌕") }
        } else {
            items(shown.chunked(2), key = { it.first().id }) { row -> ProductGridRow(row, store) }
        }
        item { SampleDataFootnote() }
    }
}

@Composable
private fun CategoryFilter(store: AngadiStore) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(store.categoryNames, key = { it }) { category ->
            val selected = store.selectedCategory == category
            Surface(
                onClick = { store.selectedCategory = category },
                shape = RoundedCornerShape(99.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(category, Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SavedScreen(store: AngadiStore) {
    val favorites = store.favoriteProducts
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader("Saved for later", "Your little list of good things", store) }
        if (favorites.isEmpty()) {
            item { EmptyState("Nothing saved yet", "Tap the heart on anything you like and it’ll be here.", "♡") }
            item { Button(onClick = { store.navigate(StoreTab.EXPLORE) }, shape = RoundedCornerShape(14.dp)) { Text("Explore products") } }
        } else {
            items(favorites.chunked(2), key = { it.first().id }) { row -> ProductGridRow(row, store) }
        }
    }
}

@Composable
private fun CartScreen(store: AngadiStore) {
    val items = store.cartItems
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader("Your basket", "The good stuff you picked", store) }
        if (items.isEmpty()) {
            item { EmptyState("Your basket is taking a breather", "Browse the shelves and add a few favourites.", "▤") }
            item {
                Button(onClick = { store.navigate(StoreTab.EXPLORE) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Text("Browse the shop") }
            }
        } else {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("✦", color = MaterialTheme.colorScheme.primary, fontSize = 19.sp)
                        Spacer(Modifier.width(9.dp))
                        Text("Fresh finds, ready when you are.", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            items(items, key = { it.first.id }) { (product, count) -> CartProductRow(product, count, store) }
            item { PriceSummary(store.cartSubtotal, store.cartCount, onCheckout = { store.showCheckout = true }) }
            item { SampleDataFootnote() }
        }
    }
}

@Composable
private fun CartProductRow(product: Product, count: Int, store: AngadiStore) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(MaterialTheme.colorScheme.surface).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductArtwork(product, Modifier.size(88.dp).clip(RoundedCornerShape(15.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(product.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(product.unit, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatPrice(product.price), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                QuantityControl(count, onMinus = { store.changeQuantity(product.id, -1) }, onPlus = { store.changeQuantity(product.id, 1) })
            }
        }
    }
}

@Composable
private fun QuantityControl(count: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallStepButton("−", "Remove one", onMinus)
        Text(count.toString(), Modifier.width(27.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        SmallStepButton("+", "Add one", onPlus)
    }
}

@Composable
private fun SmallStepButton(symbol: String, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(9.dp), color = Color.Transparent, modifier = Modifier.size(32.dp).semantics { contentDescription = label }) {
        Box(contentAlignment = Alignment.Center) { Text(symbol, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
    }
}

@Composable
private fun PriceSummary(subtotal: Double, itemCount: Int, onCheckout: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("Order summary", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            SummaryLine("Items ($itemCount)", formatPrice(subtotal))
            SummaryLine("Delivery", if (subtotal >= 699) "Free" else formatPrice(40.0))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SummaryLine("Total", formatPrice(subtotal + if (subtotal >= 699) 0.0 else 40.0), strong = true)
            Button(onClick = onCheckout, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text("Continue to checkout  ·  ${formatPrice(subtotal + if (subtotal >= 699) 0.0 else 40.0)}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = if (strong) 14.sp else 13.sp, color = if (strong) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontSize = if (strong) 15.sp else 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun CheckoutScreen(store: AngadiStore) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundAction("‹", "Back to basket") { store.showCheckout = false }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Checkout", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("One last little step", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            SectionCard("Delivery address", "Where should we bring your order?") {
                OutlinedTextField(
                    value = store.checkoutAddress,
                    onValueChange = { store.checkoutAddress = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Full delivery address") },
                    placeholder = { Text("House, street, area, city") },
                    shape = RoundedCornerShape(14.dp),
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }
        }
        item {
            SectionCard("Payment", "Choose how you’d like to pay") {
                PaymentOption("Cash on delivery", "Pay when your order arrives", true)
                Spacer(Modifier.height(9.dp))
                PaymentOption("UPI on delivery", "Use your preferred UPI app", false)
                Spacer(Modifier.height(10.dp))
                Text("Demo checkout only. No payment is collected in this build.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionCard("Your order", "${store.cartCount} item(s)") {
                store.cartItems.forEach { (product, quantity) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("$quantity × ${product.title}", Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatPrice(product.price * quantity), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SummaryLine("Total", formatPrice(store.cartSubtotal + if (store.cartSubtotal >= 699) 0.0 else 40.0), strong = true)
            }
        }
        item {
            Button(
                onClick = { store.placeOrder() }, enabled = !store.isPlacingOrder,
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                if (store.isPlacingOrder) CircularProgressIndicator(Modifier.size(19.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Place demo order", fontWeight = FontWeight.Bold)
            }
        }
        item { Text("Sample catalogue and checkout are for demonstration. Connect your own merchant, inventory, payment, and delivery services before accepting real orders.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp) }
    }
}

@Composable
private fun PaymentOption(title: String, detail: String, selected: Boolean) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (selected) "●" else "○", color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(detail, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileScreen(store: AngadiStore) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader("Your Angadi", "A little home for your shopping", store) }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.primary).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(55.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) { Text("☺", color = MaterialTheme.colorScheme.onPrimary, fontSize = 26.sp) }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("Hello, Ajjaiah", color = MaterialTheme.colorScheme.onPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Guest shopper · local demo", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.77f), fontSize = 11.sp)
                }
                Text("✦", color = MaterialTheme.colorScheme.secondary, fontSize = 22.sp)
            }
        }
        item {
            SectionCard("Appearance", "Choose how Angadi looks on this device") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeMode.entries.forEach { mode ->
                        val selected = store.themeMode == mode
                        Surface(
                            onClick = { store.applyThemeMode(mode) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    when (mode) { AppThemeMode.SYSTEM -> "System"; AppThemeMode.LIGHT -> "Light"; AppThemeMode.DARK -> "Dark" },
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionCard("Your orders", "Saved on this device") {
                if (store.orders.isEmpty()) {
                    Text("Your first order will show up here.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                } else {
                    store.orders.forEachIndexed { index, order ->
                        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        OrderRow(order)
                    }
                }
            }
        }
        item {
            SectionCard("API & AI", "Connect your private Gemini proxy") {
                Text("The grocery catalogue loads from a public sample API. Add your own backend URL below to enable live Gemini replies.", fontSize = 11.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(7.dp))
                OutlinedTextField(
                    value = store.assistantServerDraft,
                    onValueChange = { store.assistantServerDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Your API server URL") },
                    placeholder = { Text("https://your-server.example.com") },
                    supportingText = { Text("Local Android emulator: http://10.0.2.2:3000") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, capitalization = KeyboardCapitalization.None),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { store.saveAssistantServer() }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Save server", fontSize = 12.sp) }
                    OutlinedButton(onClick = { store.testAssistantServer() }, enabled = !store.isCheckingServer, shape = RoundedCornerShape(12.dp)) {
                        if (store.isCheckingServer) CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
                        else Text("Test connection", fontSize = 12.sp)
                    }
                }
                Text(store.serverStatus, Modifier.padding(top = 5.dp), fontSize = 11.sp, color = if (store.serverStatus.contains("connected", true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionCard("A note about this build", "Ready to grow with your business") {
                Text("Angadi includes an offline-ready local basket, saved items, and order history. Sample checkout does not process payments or contact a delivery service. Connect your own commerce backend before launch.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { store.showAssistant = true }, shape = RoundedCornerShape(12.dp)) { Text("Chat with Angadi AI  ✦", color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
private fun OrderRow(order: OrderSummary) {
    val date = remember(order.createdAt) { SimpleDateFormat("dd MMM yyyy · h:mm a", Locale.getDefault()).format(Date(order.createdAt)) }
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Order #${order.id}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("${order.itemCount} item(s) · $date", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatPrice(order.total), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Demo order", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AssistantScreen(store: AngadiStore) {
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundAction("‹", "Back to shop") { store.showAssistant = false }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Angadi AI", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Text(if (store.assistantServer.isBlank()) "App guide · Gemini setup needed" else "Shopping sidekick · Gemini", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("✦", fontSize = 22.sp, color = MaterialTheme.colorScheme.secondary)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (store.assistantMessages.size <= 1) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text("Ask me how to shop, save items, manage your basket, or change the theme.", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp)
                Spacer(Modifier.height(11.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("How do I use this app?", "Easy breakfast ideas", "Healthy snack picks", "What’s in my basket?")) { suggestion ->
                        AssistChip(onClick = { store.assistantInput = suggestion; store.sendAssistant() }, label = { Text(suggestion, fontSize = 11.sp) })
                    }
                }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(store.assistantMessages) { message -> ChatBubble(message) }
            if (store.isAssistantThinking) item { ChatBubble(AssistantMessage(true, "Thinking of something good…")) }
        }
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = store.assistantInput,
                onValueChange = { store.assistantInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about the app, shopping, or food…", fontSize = 13.sp) },
                shape = RoundedCornerShape(20.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { store.sendAssistant() }, enabled = store.assistantInput.isNotBlank() && !store.isAssistantThinking, modifier = Modifier.size(52.dp), contentPadding = PaddingValues(0.dp), shape = CircleShape) {
                Text("↑", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(3.dp))
    }
}

@Composable
private fun ChatBubble(message: AssistantMessage) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromAssistant) Arrangement.Start else Arrangement.End) {
        if (message.fromAssistant) {
            Box(Modifier.padding(top = 3.dp).size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Text("✦", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp) }
            Spacer(Modifier.width(7.dp))
        }
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            color = if (message.fromAssistant) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = if (message.fromAssistant) 5.dp else 17.dp, bottomEnd = if (message.fromAssistant) 17.dp else 5.dp),
        ) {
            Text(message.text, Modifier.padding(horizontal = 14.dp, vertical = 11.dp), fontSize = 13.sp, color = if (message.fromAssistant) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary, lineHeight = 19.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDetailSheet(product: Product, store: AngadiStore) {
    ModalBottomSheet(
        onDismissRequest = { store.closeProduct() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 26.dp)) {
            Box {
                ProductArtwork(product, Modifier.fillMaxWidth().height(260.dp))
                RoundAction("×", "Close product", modifier = Modifier.align(Alignment.TopEnd).padding(14.dp)) { store.closeProduct() }
                if (product.discountPercent > 0) DiscountTag(Modifier.align(Alignment.TopStart).padding(14.dp), product.discountPercent)
            }
            Column(Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
                Text(product.category.uppercase(), fontSize = 10.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(5.dp))
                Text(product.title, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("★ ${String.format(Locale.getDefault(), "%.1f", product.rating)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFAB7818))
                    Text("  ·  ${product.unit}  ·  ${product.stock} in stock", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(13.dp))
                Text(product.description, fontSize = 14.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(formatPrice(product.price), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        if (product.discountPercent > 0) Text(formatPrice(product.unitPrice), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                    }
                    OutlinedButton(onClick = { store.toggleFavorite(product) }, shape = RoundedCornerShape(13.dp)) {
                        Text(if (product.id in store.favorites) "♥ Saved" else "♡ Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { store.addToCart(product); store.closeProduct() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Add to basket  ·  ${formatPrice(product.price)}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProductGrid(store: AngadiStore, products: List<Product>) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        products.chunked(2).forEach { row -> ProductGridRow(row, store) }
    }
}

@Composable
private fun ProductGridRow(products: List<Product>, store: AngadiStore) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        products.forEach { product -> ProductCard(product, store, Modifier.weight(1f)) }
        if (products.size == 1) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ProductCard(product: Product, store: AngadiStore, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable { store.openProduct(product) },
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
    ) {
        Column(Modifier.padding(9.dp)) {
            Box(Modifier.fillMaxWidth()) {
                ProductArtwork(product, Modifier.fillMaxWidth().height(137.dp).clip(RoundedCornerShape(14.dp)))
                if (product.discountPercent > 0) DiscountTag(Modifier.align(Alignment.TopStart).padding(7.dp), product.discountPercent)
                RoundAction(
                    if (product.id in store.favorites) "♥" else "♡",
                    if (product.id in store.favorites) "Remove from saved" else "Save product",
                    modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(32.dp),
                    isActive = product.id in store.favorites,
                ) { store.toggleFavorite(product) }
            }
            Spacer(Modifier.height(9.dp))
            Text(product.category, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(product.title, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatPrice(product.price), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                Text("★ ${String.format(Locale.getDefault(), "%.1f", product.rating)}", fontSize = 9.sp, color = Color(0xFF9B711C))
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { store.addToCart(product) },
                modifier = Modifier.fillMaxWidth().height(36.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) { Text("+  Add", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ProductArtwork(product: Product, modifier: Modifier = Modifier) {
    if (product.thumbnail.isBlank()) {
        Box(modifier.background(artworkColor(product.category)), contentAlignment = Alignment.Center) { Text(product.emoji, fontSize = 54.sp) }
    } else {
        val bitmap by produceState<Bitmap?>(initialValue = null, key1 = product.thumbnail) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val connection = URL(product.thumbnail).openConnection() as HttpURLConnection
                    connection.connectTimeout = 5_000
                    connection.readTimeout = 7_000
                    try { connection.inputStream.use { BitmapFactory.decodeStream(it) } } finally { connection.disconnect() }
                }.getOrNull()
            }
        }
        if (bitmap == null) {
            Box(modifier.background(artworkColor(product.category)), contentAlignment = Alignment.Center) { Text(product.emoji, fontSize = 46.sp) }
        } else {
            Image(bitmap!!.asImageBitmap(), contentDescription = product.title, modifier = modifier.background(MaterialTheme.colorScheme.background), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun DiscountTag(modifier: Modifier = Modifier, percent: Double) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondary).padding(horizontal = 7.dp, vertical = 4.dp)) {
        Text("${percent.toInt()}% OFF", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF17251F))
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String, store: AngadiStore) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        RoundAction("✦", "Open Angadi AI") { store.showAssistant = true }
        Spacer(Modifier.width(8.dp))
        RoundAction("▤", "Basket", badge = store.cartCount) { store.navigate(StoreTab.CART) }
    }
}

@Composable
private fun RoundAction(symbol: String, label: String, modifier: Modifier = Modifier.size(42.dp), badge: Int = 0, isActive: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = label },
        shape = CircleShape,
        color = if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol, fontSize = 19.sp, color = if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            if (badge > 0) Box(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp).size(14.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary), contentAlignment = Alignment.Center) {
                Text(badge.coerceAtMost(9).toString(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF17251F))
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String, symbol: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 24.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(65.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Text(symbol, color = MaterialTheme.colorScheme.primary, fontSize = 31.sp) }
        Spacer(Modifier.height(13.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(5.dp))
        Text(body, fontSize = 12.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SectionCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SampleDataFootnote() {
    Text("Sample catalogue · prices and stock are illustrative demo data.", Modifier.fillMaxWidth().padding(vertical = 4.dp), textAlign = TextAlign.Center, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun categoryVisual(category: String): Pair<String, Color> = when (category) {
    "Fresh produce" -> "🥬" to Color(0xFFE7F1DB)
    "Dairy & eggs" -> "🥛" to Color(0xFFEAF1F4)
    "Bakery" -> "🥖" to Color(0xFFFFEEDA)
    "Pantry" -> "🫙" to Color(0xFFF2E9DC)
    else -> "🛍️" to Color(0xFFE6F2E9)
}

private fun artworkColor(category: String): Color = when {
    category.contains("produce", true) || category.contains("grocer", true) -> Color(0xFFEAF2E2)
    category.contains("dairy", true) -> Color(0xFFE8F0F3)
    category.contains("bakery", true) -> Color(0xFFFFF0DE)
    else -> Color(0xFFF0ECE2)
}

private fun formatPrice(amount: Double): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).apply { maximumFractionDigits = 0 }.format(amount)
