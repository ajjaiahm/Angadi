package com.angadi.app.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angadi.app.data.repository.ProfileRepository
import com.angadi.app.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ProfileEditorState(val draft: UserProfile = UserProfile(), val error: String? = null, val saved: Boolean = false)

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {
    val profile: StateFlow<UserProfile> = repository.profile.stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile())

    private val _editor = MutableStateFlow(ProfileEditorState())
    val editor: StateFlow<ProfileEditorState> = _editor.asStateFlow()

    fun startEditing() {
        _editor.value = ProfileEditorState(draft = profile.value)
    }

    fun update(transform: (UserProfile) -> UserProfile) = _editor.update { it.copy(draft = transform(it.draft), error = null, saved = false) }

    fun save() {
        viewModelScope.launch {
            val error = repository.save(_editor.value.draft)
            _editor.update { it.copy(error = error, saved = error == null) }
        }
    }

    /** Copies the picked image into app-private storage so it survives the source being removed. */
    fun importImage(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                runCatching {
                    val target = File(appContext.filesDir, "profile_${System.currentTimeMillis()}.jpg")
                    appContext.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
                    target.takeIf { it.length() in 1..MAX_IMAGE_BYTES }?.absolutePath.also { if (it == null) target.delete() }
                }.getOrNull()
            }
            if (path == null) _editor.update { it.copy(error = "Couldn't use that image. Pick a photo under 8 MB.") }
            else {
                val old = _editor.value.draft.imagePath
                _editor.update { it.copy(draft = it.draft.copy(imagePath = path), error = null) }
                if (old.isNotBlank() && old != profile.value.imagePath) File(old).delete()
            }
        }
    }

    fun removeImage() = update { it.copy(imagePath = "") }

    private companion object {
        const val MAX_IMAGE_BYTES = 8L * 1024 * 1024
    }
}
