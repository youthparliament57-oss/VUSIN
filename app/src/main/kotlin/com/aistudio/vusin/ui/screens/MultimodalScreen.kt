package com.aistudio.vusin.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.vusin.R
import com.aistudio.vusin.ui.MainViewModel
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultimodalScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val sampleImages = listOf(
        Pair(R.drawable.sample_google, "Google Logo (PNG)"),
        Pair(R.drawable.app_launcher_icon_1792182038225, "AI Emblem (Artwork)")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "Multimodal Vision",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Analyze images alongside text prompts using Gemini's native multimodal capabilities.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Select Image
        Text(
            text = "Choose Sample Image",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sampleImages.forEachIndexed { index, (drawableRes, label) ->
                val isSelected = uiState.selectedSampleImageIndex == index
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp)
                        .clickable { viewModel.selectSampleImage(index) },
                    border = if (isSelected) CardDefaults.outlinedCardBorder().copy(width = 2.dp) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = drawableRes),
                            contentDescription = label,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Prompt Input
        OutlinedTextField(
            value = uiState.multimodalPrompt,
            onValueChange = { viewModel.updateMultimodalPrompt(it) },
            label = { Text("Vision Prompt") },
            placeholder = { Text("Ask anything about the chosen image...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("multimodal_prompt_input"),
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(12.dp)
        )

        // Run Analysis Button
        Button(
            onClick = {
                val selectedRes = sampleImages[uiState.selectedSampleImageIndex.coerceIn(0, sampleImages.size - 1)].first
                val bitmap = BitmapFactory.decodeResource(context.resources, selectedRes)
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val byteArray = stream.toByteArray()
                viewModel.analyzeImageWithPrompt(byteArray)
            },
            enabled = uiState.multimodalPrompt.isNotBlank() && !uiState.isMultimodalLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("analyze_image_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (uiState.isMultimodalLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing Image...")
            } else {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyze Image")
            }
        }

        // Result Card
        if (uiState.multimodalResult.isNotBlank() || uiState.isMultimodalLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Vision Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.multimodalResult.ifEmpty { "Reasoning across vision tokens..." },
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
