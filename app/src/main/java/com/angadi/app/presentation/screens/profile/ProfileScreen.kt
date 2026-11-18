package com.angadi.app.presentation.screens.profile

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angadi.app.domain.model.UserProfile
import com.angadi.app.presentation.components.ScreenHeader
import com.angadi.app.presentation.components.SectionCard
import com.angadi.app.presentation.viewmodel.AssistantUiState
import com.angadi.app.presentation.viewmodel.ShopUiState
import com.angadi.app.ui.theme.AppThemeMode

class ProfileCallbacks(
    val onAssistant: () -> Unit,
    val onBasket: () -> Unit,
    val onEditProfile: () -> Unit,
    val onOrders: () -> Unit,
    val onAddresses: () -> Unit,
    val onTheme: (AppThemeMode) -> Unit,
    val onRecentLimit: (Int) -> Unit,
    val onClearRecent: () -> Unit,
    val onServerDraft: (String) -> Unit,
    val onSaveServer: () -> Unit,
    val onTestServer: () -> Unit,
)

@Composable
fun ProfileScreen(state: ShopUiState, orderCount: Int, assistant: AssistantUiState, callbacks: ProfileCallbacks) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("profile_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader("Your Angadi", "A little home for your shopping", state.cartCount, callbacks.onAssistant, callbacks.onBasket) }
        item { ProfileCard(state.profile, callbacks.onEditProfile) }
        item {
            SectionCard("Shortcuts", "Everything saved on this device") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShortcutRow("▤", "Your orders", if (orderCount == 0) "No orders yet" else "$orderCount order(s)", "shortcut_orders", callbacks.onOrders)
                    ShortcutRow("⌖", "Saved addresses", "Manage delivery addresses", "shortcut_addresses", callbacks.onAddresses)
                }
            }
        }
        item {
            SectionCard("Appearance", "Choose how Angadi looks on this device") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeMode.entries.forEach { mode ->
                        val selected = state.themeMode == mode
                        Surface(
                            onClick = { callbacks.onTheme(mode) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("theme_${mode.name}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    when (mode) { AppThemeMode.SYSTEM -> "System"; AppThemeMode.LIGHT -> "Light"; AppThemeMode.DARK -> "Dark" },
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionCard("Recently viewed", "Products you opened lately, kept on this device") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Keep last", fontSize = 13.sp)
                    listOf(6, 12, 24).forEach { n ->
                        val selected = state.recentLimit == n
                        Surface(
                            onClick = { callbacks.onRecentLimit(n) }, modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(99.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                                Text("$n", color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                OutlinedButton(onClick = callbacks.onClearRecent, enabled = state.recentProducts.isNotEmpty(), modifier = Modifier.padding(top = 8.dp).heightIn(min = 48.dp).testTag("clear_recent")) { Text("Clear viewing history") }
            }
        }
        item {
            SectionCard("API & AI", "Connect your private Gemini proxy") {
                Text("The grocery catalogue loads from a public sample API. Add your own backend URL below to enable live Gemini replies.", fontSize = 11.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(7.dp))
                OutlinedTextField(
                    value = assistant.serverDraft, onValueChange = callbacks.onServerDraft, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Your API server URL") }, placeholder = { Text("https://your-server.example.com") },
                    supportingText = { Text("Local Android emulator: http://10.0.2.2:3000") }, singleLine = true, shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, capitalization = KeyboardCapitalization.None),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = callbacks.onSaveServer, shape = RoundedCornerShape(12.dp), modifier = Modifier.heightIn(min = 48.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Save server", fontSize = 12.sp) }
                    OutlinedButton(onClick = callbacks.onTestServer, enabled = !assistant.isCheckingServer, shape = RoundedCornerShape(12.dp), modifier = Modifier.heightIn(min = 48.dp)) {
                        if (assistant.isCheckingServer) CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp) else Text("Test connection", fontSize = 12.sp)
                    }
                }
                Text(assistant.serverStatus, Modifier.padding(top = 5.dp), fontSize = 11.sp, color = if (assistant.serverStatus.contains("connected", true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionCard("A note about this build", "Ready to grow with your business") {
                Text("Angadi keeps your basket, saved items, addresses, reviews and orders on this device only. There are no accounts, so your profile is a local convenience, not a sign-in. Checkout doesn't process payments or contact a delivery service. Connect your own commerce backend before launch.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = callbacks.onAssistant, shape = RoundedCornerShape(12.dp), modifier = Modifier.heightIn(min = 48.dp)) { Text("Chat with Angadi AI  ✦", color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
private fun ProfileCard(profile: UserProfile, onEdit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.primary).clickable(onClick = onEdit).padding(18.dp).testTag("profile_card"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(profile.imagePath, Modifier.size(56.dp))
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(if (profile.name.isBlank()) "Add your name" else profile.name, Modifier.testTag("profile_name"), color = MaterialTheme.colorScheme.onPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(
                listOf(profile.email, profile.phone).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Local profile · tap to edit" },
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), fontSize = 11.sp,
            )
        }
        Text("✎", color = MaterialTheme.colorScheme.secondary, fontSize = 22.sp)
    }
}

@Composable
fun ProfileAvatar(imagePath: String, modifier: Modifier = Modifier) {
    val bitmap = remember(imagePath) { if (imagePath.isBlank()) null else runCatching { BitmapFactory.decodeFile(imagePath, BitmapFactory.Options().apply { inSampleSize = 4 }) }.getOrNull() }
    Box(modifier.clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap.asImageBitmap(), "Profile photo", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text("☺", color = MaterialTheme.colorScheme.onPrimary, fontSize = 26.sp)
    }
}

@Composable
private fun ShortcutRow(symbol: String, title: String, detail: String, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.background).clickable(onClick = onClick)
            .heightIn(min = 56.dp).padding(12.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(symbol, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
    }
}
