package com.angadi.app.data.repository

import android.content.Context
import com.angadi.app.data.remote.GeminiAssistantService
import com.angadi.app.ui.theme.AppThemeMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** SharedPreferences-backed settings (theme, assistant server, history limit). Preference keys are unchanged from v1. */
class SettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("angadi_preferences", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(runCatching { AppThemeMode.valueOf(prefs.getString("theme_mode", "SYSTEM").orEmpty()) }.getOrDefault(AppThemeMode.SYSTEM))
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _assistantServer = MutableStateFlow(prefs.getString("assistant_server", "").orEmpty())
    val assistantServer: StateFlow<String> = _assistantServer.asStateFlow()

    private val _recentLimit = MutableStateFlow(prefs.getInt("recent_limit", DEFAULT_RECENT_LIMIT).coerceIn(1, MAX_RECENT_LIMIT))
    val recentLimit: StateFlow<Int> = _recentLimit.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    fun setAssistantServer(url: String) {
        val value = url.trim().trimEnd('/')
        prefs.edit().putString("assistant_server", value).apply()
        _assistantServer.value = value
    }

    fun setRecentLimit(limit: Int) {
        val value = limit.coerceIn(1, MAX_RECENT_LIMIT)
        prefs.edit().putInt("recent_limit", value).apply()
        _recentLimit.value = value
    }

    companion object {
        const val DEFAULT_RECENT_LIMIT = 12
        const val MAX_RECENT_LIMIT = 30
    }
}

class AssistantRepository(
    private val service: GeminiAssistantService = GeminiAssistantService(),
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun ask(url: String, messages: List<Pair<Boolean, String>>, context: String): String = withContext(io) { service.ask(url, messages, context) }
    suspend fun check(url: String): String = withContext(io) { service.checkServer(url) }
}
