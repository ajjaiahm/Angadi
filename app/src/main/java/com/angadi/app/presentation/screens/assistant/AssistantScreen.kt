package com.angadi.app.presentation.screens.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angadi.app.presentation.components.RoundAction
import com.angadi.app.presentation.viewmodel.AssistantMessage
import com.angadi.app.presentation.viewmodel.AssistantUiState

@Composable
fun AssistantScreen(state: AssistantUiState, onBack: () -> Unit, onInput: (String) -> Unit, onSend: (String?) -> Unit) {
    Column(Modifier.fillMaxSize().imePadding().testTag("assistant_screen")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundAction("‹", "Back to shop", onClick = onBack)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Angadi AI", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Text(if (state.server.isBlank()) "App guide · Gemini setup needed" else "Shopping sidekick · Gemini", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("✦", fontSize = 22.sp, color = MaterialTheme.colorScheme.secondary)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (state.messages.size <= 1) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text("Ask me how to shop, save items, manage your basket, or change the theme.", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp)
                Spacer(Modifier.height(11.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("How do I use this app?", "Easy breakfast ideas", "Healthy snack picks", "What’s in my basket?")) { suggestion ->
                        AssistChip(onClick = { onSend(suggestion) }, label = { Text(suggestion, fontSize = 11.sp) })
                    }
                }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.messages) { message -> ChatBubble(message) }
            if (state.isThinking) item { ChatBubble(AssistantMessage(true, "Thinking of something good…")) }
        }
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = state.input, onValueChange = onInput, modifier = Modifier.weight(1f).testTag("assistant_input"),
                placeholder = { Text("Ask about the app, shopping, or food…", fontSize = 13.sp) },
                shape = RoundedCornerShape(20.dp), maxLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onSend(null) }, enabled = state.input.isNotBlank() && !state.isThinking, modifier = Modifier.size(52.dp), contentPadding = PaddingValues(0.dp), shape = CircleShape) {
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
            Box(Modifier.padding(top = 3.dp).size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Text("✦", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            }
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
