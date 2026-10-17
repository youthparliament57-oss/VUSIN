package com.aistudio.vusin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.vusin.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var keyInput by remember(uiState.apiKey) { mutableStateOf(uiState.apiKey) }
    var hideKey by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings & Credentials",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // API Key Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.hasApiKey) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (uiState.hasApiKey) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (uiState.hasApiKey) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (uiState.hasApiKey) "API Key Active" else "API Key Required for Live Calls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.hasApiKey) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = if (uiState.hasApiKey) {
                            "Client connects directly to Google Gemini API."
                        } else {
                            "Set your GEMINI_API_KEY below or in AI Studio Secrets panel."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.hasApiKey) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Key Input Box
        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("Gemini API Key") },
            placeholder = { Text("AIzaSy...") },
            visualTransformation = if (hideKey) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = {
                TextButton(onClick = { hideKey = !hideKey }) {
                    Text(if (hideKey) "Show" else "Hide", fontSize = 12.sp)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("api_key_input_field"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Button(
            onClick = {
                viewModel.updateApiKey(keyInput.trim())
                Toast.makeText(context, "API Key saved successfully!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_api_key_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Key, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save API Key")
        }

        HorizontalDivider()

        // SDK Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "About Google Gen AI SDK (VUSIN)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "• Package: com.google.genai.kotlin\n" +
                            "• Target: Android & Kotlin Multiplatform\n" +
                            "• Version: 1.7.0-SNAPSHOT\n" +
                            "• Ktor HTTP Engine: OkHttp\n" +
                            "• Models Supported: Gemini 2.5 Flash, 2.5 Pro, 2.0 Flash, 1.5 Flash\n" +
                            "• Capabilities: Streaming Content, Multi-turn Chat, Multimodal Vision, Token Counting",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
