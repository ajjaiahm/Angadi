package com.angadi.app.presentation.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angadi.app.presentation.components.BackHeader
import com.angadi.app.presentation.viewmodel.ProfileEditorState
import com.angadi.app.presentation.viewmodel.ProfileViewModel

@Composable
fun ProfileEditScreen(editor: ProfileEditorState, viewModel: ProfileViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(editor.saved) { if (editor.saved) onSaved() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) viewModel.importImage(context, uri) }
    val d = editor.draft
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("profile_edit"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BackHeader("Edit profile", "Stored only on this device", onBack) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.size(72.dp)) { ProfileAvatar(d.imagePath, Modifier.fillMaxSize()) }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Choose photo", fontSize = 12.sp) }
                    if (d.imagePath.isNotBlank()) OutlinedButton(onClick = viewModel::removeImage, modifier = Modifier.heightIn(min = 48.dp)) { Text("Remove photo", fontSize = 12.sp) }
                }
            }
        }
        item {
            OutlinedTextField(d.name, { v -> viewModel.update { it.copy(name = v.take(60)) } }, Modifier.fillMaxWidth().testTag("profile_name_field"), label = { Text("Name") }, singleLine = true,
                shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
        }
        item {
            OutlinedTextField(d.email, { v -> viewModel.update { it.copy(email = v.take(80)) } }, Modifier.fillMaxWidth().testTag("profile_email_field"), label = { Text("Email (optional)") }, singleLine = true,
                shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        }
        item {
            OutlinedTextField(d.phone, { v -> viewModel.update { it.copy(phone = v.take(14)) } }, Modifier.fillMaxWidth().testTag("profile_phone_field"), label = { Text("Phone (optional)") }, singleLine = true,
                shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        }
        editor.error?.let { error -> item { Text(error, Modifier.testTag("profile_error"), color = MaterialTheme.colorScheme.error, fontSize = 12.sp) } }
        item { Text("There is no sign-in: this profile lives only on this device and isn't a verified account.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("save_profile"), shape = RoundedCornerShape(14.dp)) { Text("Save profile") } }
    }
}
