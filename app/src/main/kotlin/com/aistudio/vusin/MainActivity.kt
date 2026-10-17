package com.aistudio.vusin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.aistudio.vusin.ui.MainViewModel
import com.aistudio.vusin.ui.Screen
import com.aistudio.vusin.ui.screens.*
import com.aistudio.vusin.ui.theme.GenAIShowcaseTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GenAIShowcaseTheme {
                AppRoot(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle back button to return to Chat if on other screens
    if (uiState.currentScreen != Screen.CHAT) {
        BackHandler {
            viewModel.selectScreen(Screen.CHAT)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                NavigationBarItem(
                    selected = uiState.currentScreen == Screen.CHAT,
                    onClick = { viewModel.selectScreen(Screen.CHAT) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat") },
                    label = { Text("Chat") },
                    modifier = Modifier.testTag("nav_tab_chat")
                )
                NavigationBarItem(
                    selected = uiState.currentScreen == Screen.GENERATE,
                    onClick = { viewModel.selectScreen(Screen.GENERATE) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Generate") },
                    label = { Text("Generate") },
                    modifier = Modifier.testTag("nav_tab_generate")
                )
                NavigationBarItem(
                    selected = uiState.currentScreen == Screen.MULTIMODAL,
                    onClick = { viewModel.selectScreen(Screen.MULTIMODAL) },
                    icon = { Icon(Icons.Default.Visibility, contentDescription = "Vision") },
                    label = { Text("Vision") },
                    modifier = Modifier.testTag("nav_tab_vision")
                )
                NavigationBarItem(
                    selected = uiState.currentScreen == Screen.TOKENS,
                    onClick = { viewModel.selectScreen(Screen.TOKENS) },
                    icon = { Icon(Icons.Default.Numbers, contentDescription = "Tokens") },
                    label = { Text("Tokens") },
                    modifier = Modifier.testTag("nav_tab_tokens")
                )
                NavigationBarItem(
                    selected = uiState.currentScreen == Screen.MODELS,
                    onClick = { viewModel.selectScreen(Screen.MODELS) },
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = "Models") },
                    label = { Text("Models") },
                    modifier = Modifier.testTag("nav_tab_models")
                )
                NavigationBarItem(
                    selected = uiState.currentScreen == Screen.SETTINGS,
                    onClick = { viewModel.selectScreen(Screen.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        val screenModifier = Modifier.padding(innerPadding)
        when (uiState.currentScreen) {
            Screen.CHAT -> ChatScreen(viewModel = viewModel, modifier = screenModifier)
            Screen.GENERATE -> GenerateScreen(viewModel = viewModel, modifier = screenModifier)
            Screen.MULTIMODAL -> MultimodalScreen(viewModel = viewModel, modifier = screenModifier)
            Screen.TOKENS -> TokensScreen(viewModel = viewModel, modifier = screenModifier)
            Screen.MODELS -> ModelsScreen(viewModel = viewModel, modifier = screenModifier)
            Screen.SETTINGS -> SettingsScreen(viewModel = viewModel, modifier = screenModifier)
        }
    }
}
