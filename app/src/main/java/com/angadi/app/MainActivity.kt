package com.angadi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.angadi.app.presentation.navigation.AngadiApp
import com.angadi.app.presentation.viewmodel.AppViewModelFactory
import com.angadi.app.presentation.viewmodel.ShopViewModel
import com.angadi.app.ui.theme.ANGADITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val shopViewModel: ShopViewModel = viewModel(factory = AppViewModelFactory)
            val state by shopViewModel.state.collectAsStateWithLifecycle()
            ANGADITheme(themeMode = state.themeMode) {
                AngadiApp(shopViewModel)
            }
        }
    }
}
