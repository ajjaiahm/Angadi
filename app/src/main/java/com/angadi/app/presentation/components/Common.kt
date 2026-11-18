package com.angadi.app.presentation.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

fun formatRating(rating: Double): String = String.format(Locale.getDefault(), "%.1f", rating)

fun formatPrice(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).apply { maximumFractionDigits = 0 }.format(amount)

@Composable
fun RoundAction(
    symbol: String,
    label: String,
    modifier: Modifier = Modifier.size(48.dp),
    badge: Int = 0,
    isActive: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = label },
        shape = CircleShape,
        color = if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol, fontSize = 19.sp, color = if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            if (badge > 0) Box(
                Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp).size(15.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge.coerceAtMost(9).toString(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF17251F))
            }
        }
    }
}

@Composable
fun BackHeader(title: String, subtitle: String? = null, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RoundAction("‹", "Back", onClick = onBack)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String, cartCount: Int, onAssistant: () -> Unit, onBasket: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        RoundAction("✦", "Open Angadi AI", onClick = onAssistant)
        Spacer(Modifier.width(8.dp))
        RoundAction("▤", "Basket", badge = cartCount, onClick = onBasket)
    }
}

@Composable
fun SectionHeading(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        if (action != null) TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(action, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, symbol: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 24.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(65.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Text(symbol, color = MaterialTheme.colorScheme.primary, fontSize = 31.sp)
        }
        Spacer(Modifier.height(13.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(5.dp))
        Text(body, fontSize = 12.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun SectionCard(title: String, subtitle: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
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
fun SummaryLine(label: String, value: String, strong: Boolean = false, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = if (strong) 14.sp else 13.sp, color = if (strong) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontSize = if (strong) 15.sp else 13.sp, color = valueColor, fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
fun SampleDataFootnote() {
    Text("Sample catalogue · prices and stock are illustrative demo data.", Modifier.fillMaxWidth().padding(vertical = 4.dp), textAlign = TextAlign.Center, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun DiscountTag(percent: Double, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondary).padding(horizontal = 7.dp, vertical = 4.dp)) {
        Text("${percent.toInt()}% OFF", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF17251F))
    }
}

@Composable
fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Surface(
        onClick = onClick, enabled = enabled, modifier = modifier,
        shape = RoundedCornerShape(99.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            label, Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                selected -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun StarRow(rating: Double, modifier: Modifier = Modifier, size: Int = 14) {
    val full = rating.toInt().coerceIn(0, 5)
    Text("★".repeat(full) + "☆".repeat(5 - full), modifier, fontSize = size.sp, color = Color(0xFFAB7818))
}

@Composable
fun ProductArtwork(thumbnail: String, emoji: String, category: String, title: String, modifier: Modifier = Modifier, emojiSize: Int = 46) {
    if (thumbnail.isBlank()) {
        Box(modifier.background(artworkColor(category)), contentAlignment = Alignment.Center) { Text(emoji, fontSize = emojiSize.sp) }
    } else {
        val bitmap by produceState<Bitmap?>(initialValue = null, key1 = thumbnail) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val connection = URL(thumbnail).openConnection() as HttpURLConnection
                    connection.connectTimeout = 5_000
                    connection.readTimeout = 7_000
                    try { connection.inputStream.use { BitmapFactory.decodeStream(it) } } finally { connection.disconnect() }
                }.getOrNull()
            }
        }
        val loaded = bitmap
        if (loaded == null) {
            Box(modifier.background(artworkColor(category)), contentAlignment = Alignment.Center) { Text(emoji, fontSize = emojiSize.sp) }
        } else {
            Image(loaded.asImageBitmap(), contentDescription = title, modifier = modifier.background(MaterialTheme.colorScheme.background), contentScale = ContentScale.Crop)
        }
    }
}

fun categoryEmoji(category: String): String = when (category) {
    "Fresh produce" -> "🥬"
    "Dairy & eggs" -> "🥛"
    "Bakery" -> "🥖"
    "Pantry" -> "🫙"
    else -> "🛍️"
}

private fun artworkColor(category: String): Color = when {
    category.contains("produce", true) || category.contains("grocer", true) -> Color(0xFFEAF2E2)
    category.contains("dairy", true) -> Color(0xFFE8F0F3)
    category.contains("bakery", true) -> Color(0xFFFFF0DE)
    else -> Color(0xFFF0ECE2)
}

@Composable
fun SpacedColumn(modifier: Modifier = Modifier, spacing: Int = 12, content: @Composable () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.dp)) { content() }
}
