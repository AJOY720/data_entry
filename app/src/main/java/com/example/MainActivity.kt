package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DataEntryScreen
import com.example.ui.screens.WebViewScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DataEntryViewModel

enum class AppScreen {
    DATA_ENTRY,
    WEB_VIEW
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: DataEntryViewModel = viewModel()
                    var currentScreen by remember { mutableStateOf(AppScreen.DATA_ENTRY) }

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.DATA_ENTRY -> {
                                DataEntryScreen(
                                    viewModel = viewModel,
                                    onOpenWebView = { currentScreen = AppScreen.WEB_VIEW }
                                )
                            }
                            AppScreen.WEB_VIEW -> {
                                WebViewScreen(
                                    url = "https://ajoy720.github.io/data-entry--form/index.html",
                                    onBack = { currentScreen = AppScreen.DATA_ENTRY }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
