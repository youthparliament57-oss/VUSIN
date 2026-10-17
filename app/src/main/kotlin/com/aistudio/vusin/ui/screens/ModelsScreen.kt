package com.aistudio.vusin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.vusin.data.ModelUiModel
import com.aistudio.vusin.ui.MainViewModel

@Composable
fun ModelsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Gemini Models",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Models discovered via SDK ListModels API",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { viewModel.loadModels() },
                modifier = Modifier.testTag("refresh_models_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Models")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoadingModels) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.modelsList, key = { it.id }) { model ->
                    ModelCard(model = model)
                }
            }
        }
    }
}

@Composable
fun ModelCard(model: ModelUiModel) {
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
                    text = model.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Badge(
                    containerColor = when {
                        model.badge.contains("FLAGSHIP") -> MaterialTheme.colorScheme.primary
                        model.badge.contains("PRO") -> MaterialTheme.colorScheme.tertiary
                        model.badge.contains("NEXT-GEN") -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primaryContainer
                    },
                    contentColor = when {
                        model.badge.contains("FLAGSHIP") -> MaterialTheme.colorScheme.onPrimary
                        model.badge.contains("PRO") -> MaterialTheme.colorScheme.onTertiary
                        model.badge.contains("NEXT-GEN") -> MaterialTheme.colorScheme.onSecondary
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                ) {
                    Text(
                        text = model.badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "• ${model.category}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = model.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                model.inputTokenLimit?.let {
                    val limitStr = if (it >= 1000000) "${it / 1000000}M" else "${it / 1000}k"
                    Text(
                        text = "Context: $limitStr tokens",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                model.outputTokenLimit?.let {
                    val outStr = if (it >= 1000) "${it / 1000}k" else "$it"
                    Text(
                        text = "Output: $outStr tokens",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
