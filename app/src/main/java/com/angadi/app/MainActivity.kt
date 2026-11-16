package com.angadi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.angadi.app.presentation.AngadiApp
import com.angadi.app.presentation.AngadiStore
import com.angadi.app.ui.theme.ANGADITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val store = remember { AngadiStore(applicationContext) }
            ANGADITheme(themeMode = store.themeMode) {
                AngadiApp(store)
            }
        }
    }
}
