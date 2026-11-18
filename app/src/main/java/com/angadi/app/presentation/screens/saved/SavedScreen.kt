package com.angadi.app.presentation.screens.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.angadi.app.domain.model.Product
import com.angadi.app.presentation.components.EmptyState
import com.angadi.app.presentation.components.ProductActions
import com.angadi.app.presentation.components.ProductGridRow
import com.angadi.app.presentation.components.ScreenHeader

@Composable
fun SavedScreen(favorites: List<Product>, cartCount: Int, actions: ProductActions, onAssistant: () -> Unit, onBasket: () -> Unit, onExplore: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("saved_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader("Saved for later", "Your little list of good things", cartCount, onAssistant, onBasket) }
        if (favorites.isEmpty()) {
            item { EmptyState("Nothing saved yet", "Tap the heart on anything you like and it’ll be here.", "♡") }
            item { Button(onClick = onExplore, shape = RoundedCornerShape(14.dp)) { Text("Explore products") } }
        } else {
            items(favorites.chunked(2), key = { it.first().id }) { row -> ProductGridRow(row, actions) }
        }
    }
}
