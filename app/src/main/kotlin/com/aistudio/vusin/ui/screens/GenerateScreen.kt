package com.aistudio.vusin.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.vusin.data.GenAiRepository
import com.aistudio.vusin.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showAdvancedSettings by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Generate Content",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Configure parameters and stream responses directly from Gemini models.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Model Selection Dropdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Model (2026 Lineup)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = uiState.selectedGenerateModel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(GenAiRepository.POPULAR_MODELS) { modelName ->
                        FilterChip(
                            selected = uiState.selectedGenerateModel == modelName,
                            onClick = { viewModel.selectGenerateModel(modelName) },
                            label = {
                                Text(
                                    text = modelName.removePrefix("gemini-"),
                                    fontSize = 12.sp,
                                    fontWeight = if (uiState.selectedGenerateModel == modelName) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        }

        // Prompt Input
        OutlinedTextField(
            value = uiState.generatePrompt,
            onValueChange = { viewModel.updateGeneratePrompt(it) },
            label = { Text("Prompt") },
            placeholder = { Text("Enter your prompt for Gemini...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("generate_prompt_input"),
            minLines = 3,
            maxLines = 6,
            shape = RoundedCornerShape(12.dp)
        )

        // Advanced Settings Expander
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showAdvancedSettings = !showAdvancedSettings }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generation Config & System Instruction", fontWeight = FontWeight.Medium)
                }
                Text(
                    text = if (showAdvancedSettings) "▲" else "▼",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showAdvancedSettings) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Temperature: ${String.format("%.1f", uiState.temperature)}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = uiState.temperature,
                        onValueChange = { viewModel.updateTemperature(it) },
                        valueRange = 0.0f..2.0f,
                        steps = 19
                    )

                    OutlinedTextField(
                        value = uiState.systemInstruction,
                        onValueChange = { viewModel.updateSystemInstruction(it) },
                        label = { Text("System Instruction") },
                        placeholder = { Text("e.g. You are a concise code assistant.") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        }

        // Action Button
        Button(
            onClick = { viewModel.startGenerateStream() },
            enabled = uiState.generatePrompt.isNotBlank() && !uiState.isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("start_generate_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (uiState.isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generating...")
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Content")
            }
        }

        // Output Result Section
        if (uiState.generateResult.isNotBlank() || uiState.isGenerating) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Model Output",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Gemini Output", uiState.generateResult)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = uiState.generateResult.ifEmpty { "Waiting for first token..." },
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
