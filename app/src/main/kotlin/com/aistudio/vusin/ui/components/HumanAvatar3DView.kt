package com.aistudio.vusin.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

enum class AvatarState {
    IDLE, THINKING, SPEAKING, HAPPY, VISION
}

class AvatarAndroidBridge(private val onAvatarClickedCallback: () -> Unit) {
    @JavascriptInterface
    fun onAvatarClicked() {
        onAvatarClickedCallback()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HumanAvatar3DView(
    avatarState: AvatarState,
    modifier: Modifier = Modifier,
    height: Int = 220,
    showControls: Boolean = true,
    onManualStateChange: ((AvatarState) -> Unit)? = null
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(avatarState, webViewRef) {
        val stateName = avatarState.name.lowercase()
        webViewRef?.evaluateJavascript("window.setAvatarState && window.setAvatarState('$stateName');", null)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isExpanded) 380.dp else height.dp)
            .testTag("human_avatar_3d_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(Color.TRANSPARENT)
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.allowFileAccessFromFileURLs = true
                        settings.allowUniversalAccessFromFileURLs = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                view?.evaluateJavascript(
                                    "window.setAvatarState && window.setAvatarState('${avatarState.name.lowercase()}');",
                                    null
                                )
                            }

                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: android.webkit.RenderProcessGoneDetail?
                            ): Boolean {
                                view?.destroy()
                                return true
                            }
                        }
                        addJavascriptInterface(
                            AvatarAndroidBridge {
                                onManualStateChange?.invoke(AvatarState.HAPPY)
                            },
                            "AndroidBridge"
                        )
                        loadUrl("file:///android_asset/avatar.html")
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top-right Expand / Collapse button
            IconButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .background(
                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Expand 3D avatar",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Bottom reaction control dock (interactive manual triggers)
            if (showControls) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = androidx.compose.ui.graphics.Color(0xFF1E293B).copy(alpha = 0.85f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActionChip(
                            icon = Icons.Default.PanTool,
                            label = "Salute",
                            isActive = avatarState == AvatarState.HAPPY,
                            onClick = {
                                onManualStateChange?.invoke(AvatarState.HAPPY)
                                webViewRef?.evaluateJavascript("window.setAvatarState('happy');", null)
                            }
                        )
                        ActionChip(
                            icon = Icons.Default.Psychology,
                            label = "Reason",
                            isActive = avatarState == AvatarState.THINKING,
                            onClick = {
                                onManualStateChange?.invoke(AvatarState.THINKING)
                                webViewRef?.evaluateJavascript("window.setAvatarState('thinking');", null)
                            }
                        )
                        ActionChip(
                            icon = Icons.Default.RecordVoiceOver,
                            label = "Synthesize",
                            isActive = avatarState == AvatarState.SPEAKING,
                            onClick = {
                                onManualStateChange?.invoke(AvatarState.SPEAKING)
                                webViewRef?.evaluateJavascript("window.setAvatarState('speaking');", null)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) MaterialTheme.colorScheme.onPrimary else androidx.compose.ui.graphics.Color.LightGray,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isActive) MaterialTheme.colorScheme.onPrimary else androidx.compose.ui.graphics.Color.LightGray
            )
        }
    }
}
