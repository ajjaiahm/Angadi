package com.angadi.app.presentation.screens.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angadi.app.domain.model.SortOption
import com.angadi.app.presentation.components.EmptyState
import com.angadi.app.presentation.components.ProductActions
import com.angadi.app.presentation.components.ProductGridRow
import com.angadi.app.presentation.components.SampleDataFootnote
import com.angadi.app.presentation.components.ScreenHeader
import com.angadi.app.presentation.components.SelectableChip
import com.angadi.app.presentation.viewmodel.ExploreUiState
import com.angadi.app.presentation.viewmodel.ExploreViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    state: ExploreUiState,
    viewModel: ExploreViewModel,
    cartCount: Int,
    actions: ProductActions,
    onAssistant: () -> Unit,
    onBasket: () -> Unit,
) {
    val query = state.query
    val focus = LocalFocusManager.current
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var searchFocused by remember { mutableStateOf(false) }
    val showSuggestions = searchFocused && (state.suggestions.isNotEmpty())

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("explore_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader("Explore", "Find something lovely for your table", cartCount, onAssistant, onBasket) }
        item {
            OutlinedTextField(
                value = query.text,
                onValueChange = viewModel::setText,
                modifier = Modifier.fillMaxWidth().testTag("search_field").onFocusChanged { searchFocused = it.isFocused },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                label = { Text("Search products") },
                leadingIcon = { Text("⌕", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = if (query.text.isNotEmpty()) ({ TextButton(onClick = { viewModel.setText("") }) { Text("Clear") } }) else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.submit(); focus.clearFocus() }),
            )
        }
        if (showSuggestions) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("suggestions")) {
                        state.suggestions.forEach { suggestion ->
                            TextButton(onClick = { viewModel.submit(suggestion); focus.clearFocus() }, modifier = Modifier.fillMaxWidth()) {
                                Text(suggestion, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
        if (query.text.isBlank() && state.recentSearches.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent searches", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = viewModel::clearSearchHistory) { Text("Clear history", fontSize = 12.sp) }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.testTag("recent_searches")) {
                        state.recentSearches.forEach { SelectableChip(it, selected = false, onClick = { viewModel.submit(it) }) }
                    }
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.categories, key = { it }) { category ->
                    SelectableChip(category, selected = query.category == category, onClick = { viewModel.setCategory(category) })
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${state.results.size} good finds", Modifier.weight(1f).testTag("result_count"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showFilters = !showFilters }, modifier = Modifier.testTag("toggle_filters")) {
                    val count = query.activeFilterCount.let { if (query.category != "All") it - 1 else it }
                    Text(if (count > 0) "Filters ($count) ${if (showFilters) "▲" else "▼"}" else "Filters ${if (showFilters) "▲" else "▼"}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
            }
        }
        if (showFilters) item { FilterPanel(state, viewModel) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SortOption.entries, key = { it.name }) { option ->
                    SelectableChip(option.label, selected = query.sort == option, onClick = { viewModel.setSort(option) }, modifier = Modifier.testTag("sort_${option.name}"))
                }
            }
        }
        if (state.results.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EmptyState("No matches just yet", "We couldn't find anything for your search and filters. Try different words or clear the filters.", "⌕", Modifier.testTag("no_results"))
                    OutlinedButton(onClick = viewModel::clearAll, modifier = Modifier.fillMaxWidth()) { Text("Clear search and filters") }
                }
            }
        } else {
            items(state.results.chunked(2), key = { it.first().id }) { row -> ProductGridRow(row, actions) }
        }
        item { SampleDataFootnote() }
    }
}

@Composable
private fun FilterPanel(state: ExploreUiState, viewModel: ExploreViewModel) {
    val q = state.query
    var minText by rememberSaveable(q.minPrice) { mutableStateOf(q.minPrice?.toInt()?.toString().orEmpty()) }
    var maxText by rememberSaveable(q.maxPrice) { mutableStateOf(q.maxPrice?.toInt()?.toString().orEmpty()) }
    val min = minText.toDoubleOrNull()
    val max = maxText.toDoubleOrNull()
    val rangeInvalid = min != null && max != null && min > max
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.testTag("filter_panel")) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Price (₹)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    minText, { minText = it.filter(Char::isDigit).take(5) }, Modifier.weight(1f).testTag("min_price"), singleLine = true, label = { Text("Min") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = rangeInvalid,
                )
                OutlinedTextField(
                    maxText, { maxText = it.filter(Char::isDigit).take(5) }, Modifier.weight(1f).testTag("max_price"), singleLine = true, label = { Text("Max") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = rangeInvalid,
                    supportingText = if (state.maxCatalogPrice > 0) ({ Text("Up to ₹${state.maxCatalogPrice.toInt()}") }) else null,
                )
            }
            if (rangeInvalid) Text("Minimum price can't be higher than maximum.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
            Button(onClick = { viewModel.setPriceRange(min, max) }, enabled = !rangeInvalid, modifier = Modifier.testTag("apply_price")) { Text("Apply price range") }

            Text("Minimum rating", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(0.0, 3.0, 4.0, 4.5), key = { it }) { rating ->
                    SelectableChip(if (rating == 0.0) "Any" else "$rating★ & up", q.minRating == rating, { viewModel.setMinRating(rating) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("In stock only", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Switch(checked = q.inStockOnly, onCheckedChange = viewModel::setInStockOnly)
            }
            TextButton(onClick = { minText = ""; maxText = ""; viewModel.clearFilters() }) { Text("Reset filters") }
        }
    }
}
