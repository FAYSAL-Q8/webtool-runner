/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.ui.screens.runner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.ToolWebChromeClient
import com.example.engine.ToolWebViewClient
import com.example.engine.WebEngineHelper
import com.example.ui.theme.CoralRose
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassDefaults
import com.example.ui.theme.MidnightIndigo
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.VibrantEmerald
import com.example.ui.theme.glassmorphism

@Composable
fun WebToolRunnerScreen(
    toolId: Long,
    viewModel: WebToolRunnerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // File Chooser state callback
    var fileUploadCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val clipData = result.data?.clipData
            val data = result.data?.data
            val uris = when {
                clipData != null -> (0 until clipData.itemCount).map { clipData.getItemAt(it).uri }.toTypedArray()
                data != null -> arrayOf(data)
                else -> null
            }
            fileUploadCallback?.onReceiveValue(uris)
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }

    // On-demand Web Permissions (Camera, Microphone) requested by specific tools
    var pendingWebPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    val webPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        pendingWebPermissionRequest?.let { req ->
            val grantedResources = mutableListOf<String>()
            for (res in req.resources) {
                when (res) {
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                        if (results[Manifest.permission.CAMERA] == true || ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            grantedResources.add(res)
                        }
                    }
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                        if (results[Manifest.permission.RECORD_AUDIO] == true || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            grantedResources.add(res)
                        }
                    }
                    else -> grantedResources.add(res)
                }
            }
            if (grantedResources.isNotEmpty()) {
                req.grant(grantedResources.toTypedArray())
            } else {
                req.deny()
            }
        }
        pendingWebPermissionRequest = null
    }

    // On-demand Geolocation permission requested by specific tools
    var pendingGeolocation by remember { mutableStateOf<Pair<String, GeolocationPermissions.Callback>?>(null) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val isGranted = results.values.any { it }
        pendingGeolocation?.let { (origin, callback) ->
            callback.invoke(origin, isGranted, false)
        }
        pendingGeolocation = null
    }

    LaunchedEffect(toolId) {
        viewModel.loadTool(toolId)
    }

    BackHandler {
        if (uiState.isConsoleOpen) {
            viewModel.toggleConsole()
        } else if (uiState.isFullscreen) {
            viewModel.toggleFullscreen()
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.statusToast) {
        uiState.statusToast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Header Bar (hidden in full screen)
            AnimatedVisibility(
                visible = !uiState.isFullscreen,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button & Title badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)), CircleShape)
                                .testTag("runner_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = uiState.tool?.name ?: "Loading...",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1
                            )
                            uiState.tool?.group?.let { grp ->
                                Text(
                                    text = grp.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        letterSpacing = 0.8.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Action buttons (Wipe Storage, Reload, Console, Fullscreen)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Wipe Tool Storage
                        IconButton(
                            onClick = {
                                webViewRef?.let { wv ->
                                    WebEngineHelper.wipeToolStorage(wv, toolId) {
                                        viewModel.showToast("Isolated storage wiped")
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(BorderStroke(1.5.dp, CoralRose.copy(alpha = 0.7f)), CircleShape)
                                .testTag("runner_wipe_storage_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CleaningServices,
                                contentDescription = "Wipe Storage",
                                tint = CoralRose,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Reload
                        IconButton(
                            onClick = {
                                webViewRef?.reload()
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)), CircleShape)
                                .testTag("runner_reload_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Reload",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Console inspector
                        IconButton(
                            onClick = { viewModel.toggleConsole() },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.isConsoleOpen) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    BorderStroke(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.primary
                                    ),
                                    CircleShape
                                )
                                .testTag("runner_console_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Terminal,
                                contentDescription = "Console",
                                tint = if (uiState.isConsoleOpen) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Desktop Mode Toggle
                        IconButton(
                            onClick = {
                                val nextDesktop = !uiState.isDesktopMode
                                viewModel.toggleDesktopMode()
                                webViewRef?.let { wv ->
                                    WebEngineHelper.applyDesktopMode(wv, nextDesktop)
                                }
                                viewModel.showToast(if (nextDesktop) "Desktop view enabled (1280px)" else "Mobile view enabled")
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.isDesktopMode) ElectricViolet
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    BorderStroke(
                                        1.5.dp,
                                        if (uiState.isDesktopMode) ElectricViolet
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                    ),
                                    CircleShape
                                )
                                .testTag("runner_desktop_mode_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isDesktopMode) Icons.Rounded.Smartphone else Icons.Rounded.DesktopWindows,
                                contentDescription = if (uiState.isDesktopMode) "Switch to Mobile" else "Switch to Desktop",
                                tint = if (uiState.isDesktopMode) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Fullscreen
                        IconButton(
                            onClick = { viewModel.toggleFullscreen() },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)), CircleShape)
                                .testTag("runner_fullscreen_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Sleek Progress Line
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    progress = { (uiState.progress.toFloat() / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = NeonCyan,
                    trackColor = Color.Transparent
                )
            }

            // Embedded Hardware-Accelerated WebView
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                uiState.tool?.let { currentTool ->
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                WebEngineHelper.configureWebView(this) {
                                    val nextDesktop = !uiState.isDesktopMode
                                    viewModel.toggleDesktopMode()
                                    WebEngineHelper.applyDesktopMode(this, nextDesktop)
                                    viewModel.showToast(if (nextDesktop) "Desktop view enabled (1280px)" else "Mobile view restored")
                                }

                                webChromeClient = ToolWebChromeClient(
                                    onProgress = { p -> viewModel.updateProgress(p) },
                                    onConsoleLog = { log -> viewModel.addConsoleLog(log) },
                                    onFileChooser = { callback, params ->
                                        fileUploadCallback = callback
                                        try {
                                            val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                                type = "*/*"
                                            }
                                            fileChooserLauncher.launch(intent)
                                            true
                                        } catch (e: Exception) {
                                            false
                                        }
                                    },
                                    onRequestWebPermission = { req ->
                                        val needed = mutableListOf<String>()
                                        for (res in req.resources) {
                                            if (res == PermissionRequest.RESOURCE_VIDEO_CAPTURE &&
                                                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                                needed.add(Manifest.permission.CAMERA)
                                            }
                                            if (res == PermissionRequest.RESOURCE_AUDIO_CAPTURE &&
                                                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                                needed.add(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                        if (needed.isEmpty()) {
                                            req.grant(req.resources)
                                        } else {
                                            pendingWebPermissionRequest = req
                                            webPermissionsLauncher.launch(needed.toTypedArray())
                                        }
                                    },
                                    onRequestGeolocation = { origin, callback ->
                                        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        if (hasFine || hasCoarse) {
                                            callback.invoke(origin, true, false)
                                        } else {
                                            pendingGeolocation = Pair(origin, callback)
                                            locationPermissionLauncher.launch(
                                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                            )
                                        }
                                    }
                                )

                                webViewClient = ToolWebViewClient(
                                    onPageFinishedAction = { wv ->
                                        // Capture screenshot of the viewport for dynamic thumbnail
                                        WebEngineHelper.captureThumbnail(wv) { bmp ->
                                            viewModel.saveCapturedThumbnail(bmp)
                                        }
                                    }
                                )

                                WebEngineHelper.loadTool(this, currentTool)
                                webViewRef = this
                            }
                        },
                        update = { wv ->
                            webViewRef = wv
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("embedded_webview")
                    )
                } ?: Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MidnightIndigo)
                }
            }
        }

        // Fullscreen Floating Controls
        if (uiState.isFullscreen) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Desktop Mode Floating Pill
                Surface(
                    shape = CircleShape,
                    color = if (uiState.isDesktopMode) ElectricViolet.copy(alpha = 0.9f) else Slate900.copy(alpha = 0.85f),
                    modifier = Modifier
                        .glassmorphism(shape = CircleShape)
                        .clickable {
                            val nextDesktop = !uiState.isDesktopMode
                            viewModel.toggleDesktopMode()
                            webViewRef?.let { wv ->
                                WebEngineHelper.applyDesktopMode(wv, nextDesktop)
                            }
                            viewModel.showToast(if (nextDesktop) "Desktop view enabled (1280px)" else "Mobile view restored")
                        }
                        .testTag("fullscreen_desktop_mode_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.isDesktopMode) Icons.Rounded.Smartphone else Icons.Rounded.DesktopWindows,
                            contentDescription = "Desktop Toggle",
                            tint = Slate200,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (uiState.isDesktopMode) "Mobile" else "Desktop",
                            color = Slate200,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Fullscreen Exit Floating Pill
                Surface(
                    shape = CircleShape,
                    color = Slate900.copy(alpha = 0.85f),
                    modifier = Modifier
                        .glassmorphism(shape = CircleShape)
                        .clickable { viewModel.toggleFullscreen() }
                        .testTag("exit_fullscreen_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FullscreenExit,
                            contentDescription = "Exit Fullscreen",
                            tint = Slate200,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exit", color = Slate200, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Console Inspector Bottom Drawer
        AnimatedVisibility(
            visible = uiState.isConsoleOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = Slate950,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .glassmorphism(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    // Drawer Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Terminal,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Web Console (${uiState.consoleLogs.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row {
                            IconButton(
                                onClick = { viewModel.clearConsoleLogs() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteSweep,
                                    contentDescription = "Clear Console",
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.toggleConsole() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Log items
                    if (uiState.consoleLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No console output logged.",
                                color = Slate400,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("console_logs_list")
                        ) {
                            items(uiState.consoleLogs) { log ->
                                val color = when (log.level) {
                                    "ERROR" -> CoralRose
                                    "WARN" -> ElectricViolet
                                    "INFO" -> NeonCyan
                                    else -> Slate200
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "[${log.level}]",
                                        color = color,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(55.dp)
                                    )
                                    Text(
                                        text = log.message + (if (log.lineNumber > 0) " (line ${log.lineNumber})" else ""),
                                        color = color.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }
}
