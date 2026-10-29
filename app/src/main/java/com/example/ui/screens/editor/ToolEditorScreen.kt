/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.ui.screens.editor

import com.example.ui.components.AppFooter

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Tool
import com.example.engine.ToolWebChromeClient
import com.example.engine.ToolWebViewClient
import com.example.engine.WebEngineHelper
import com.example.engine.ZipToolManager
import com.example.ui.screens.editor.components.FileExplorerBottomSheet
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassDefaults
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.VibrantEmerald
import java.io.File
import java.util.Locale

@Composable
fun ToolEditorScreen(
    toolId: Long?,
    viewModel: ToolEditorViewModel,
    onNavigateBack: () -> Unit,
    onSavedSuccessfully: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var previewWebViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(toolId) {
        viewModel.loadTool(toolId)
    }

    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    // SAF Document Picker: Import ZIP or HTML
    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val fileName = getFileNameFromUri(context, it) ?: "import"
                val isZip = fileName.endsWith(".zip", ignoreCase = true)
                context.contentResolver.openInputStream(it)?.use { stream ->
                    if (isZip) {
                        viewModel.importZipIntoProject(stream, fileName)
                    } else {
                        viewModel.importHtmlIntoProject(stream, fileName)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // SAF Document Creator: Export Project as ZIP
    val zipExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    val success = viewModel.exportProjectZip(stream)
                    if (success) {
                        Toast.makeText(context, "Exported project ZIP successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // File Explorer Bottom Sheet
    FileExplorerBottomSheet(
        isOpen = uiState.isFileExplorerOpen,
        onDismiss = { viewModel.setFileExplorerOpen(false) },
        files = uiState.projectFiles,
        selectedFilePath = uiState.selectedFileRelativePath,
        entryPoint = uiState.entryPoint,
        onSelectFile = { viewModel.selectFile(it) },
        onCreateFile = { viewModel.createNewFile(it) },
        onCreateFolder = { viewModel.createNewFolder(it) },
        onDeleteFile = { viewModel.deleteFile(it) },
        onRenameFile = { old, new -> viewModel.renameFile(old, new) },
        onSetEntryPoint = { viewModel.setAsEntryPoint(it) },
        onConvertOrImportZip = { viewModel.convertToMultiFileProject() }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)), CircleShape)
                        .testTag("editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (toolId == null || toolId <= 0) "New Web Tool" else "Edit Tool",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = if (uiState.isMultiFile) ElectricViolet.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (uiState.isMultiFile) "Multi-File Project" else "Single HTML",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isMultiFile) ElectricViolet else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Header Actions: Files Drawer, Import, Export, Save
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Files Button (opens file explorer)
                IconButton(
                    onClick = {
                        if (!uiState.isMultiFile) {
                            viewModel.convertToMultiFileProject()
                        }
                        viewModel.setFileExplorerOpen(true)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("editor_files_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = "Project Files",
                        tint = if (uiState.isMultiFile) NeonCyan else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Import ZIP/HTML Button
                IconButton(
                    onClick = {
                        fileImportLauncher.launch(arrayOf("*/*", "application/zip", "text/html", "text/plain"))
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("editor_import_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileUpload,
                        contentDescription = "Import Archive or Code",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Export ZIP Button
                IconButton(
                    onClick = {
                        val safeName = uiState.name.trim().ifBlank { "web_tool" }
                            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                        zipExportLauncher.launch("$safeName.zip")
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("editor_export_zip_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = "Export as ZIP",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Save Button
                Button(
                    onClick = {
                        viewModel.saveTool { savedId ->
                            onSavedSuccessfully(savedId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("editor_save_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Tab Navigation (INFO, CODE, PREVIEW)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(3.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                EditorTab.INFO to "Settings",
                EditorTab.CODE to "Editor",
                EditorTab.PREVIEW to "Preview"
            ).forEach { (tab, label) ->
                val selected = uiState.currentTab == tab
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (selected) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.onTabChanged(tab) }
                        .testTag("tab_${tab.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (tab) {
                                EditorTab.INFO -> Icons.Rounded.Info
                                EditorTab.CODE -> Icons.Rounded.Code
                                EditorTab.PREVIEW -> Icons.Rounded.PlayArrow
                            },
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Tab Content
        when (uiState.currentTab) {
            EditorTab.INFO -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text("Tool Name", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { viewModel.onNameChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tool_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("e.g. Markdown Vault Studio") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Category / Group", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.group,
                        onValueChange = { viewModel.onGroupChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tool_group_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    val suggestedGroups = listOf("Utility", "Productivity", "Audio", "Graphics", "Security", "Dev", "Math", "Game")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestedGroups.forEach { grp ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate800,
                                modifier = Modifier.clickable { viewModel.onGroupChanged(grp) }
                            ) {
                                Text(
                                    text = grp,
                                    color = Slate200,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Description", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.onDescriptionChanged(it) },
                        placeholder = { Text("What does this web tool do?") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Multi-File vs Single File Mode Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (uiState.isMultiFile) Icons.Rounded.FolderZip else Icons.Rounded.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (uiState.isMultiFile) "Multi-File Project Architecture" else "Single HTML Document Mode",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (uiState.isMultiFile) "${uiState.projectFiles.size} project files in directory" else "All HTML, CSS, and JS in a single code block",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!uiState.isMultiFile) {
                                    Button(
                                        onClick = { viewModel.convertToMultiFileProject() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Transform, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Convert to Multi-File", fontSize = 11.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.setFileExplorerOpen(true) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Folder, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open File Explorer", fontSize = 11.sp)
                                    }
                                }
                                Button(
                                    onClick = {
                                        fileImportLauncher.launch(arrayOf("*/*", "application/zip", "text/html", "text/plain"))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.FileUpload, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import ZIP / HTML", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Quick Starter Templates", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.applyTemplate("BLANK") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Blank", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { viewModel.applyTemplate("CANVAS") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Canvas", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { viewModel.applyTemplate("NATIVE_BRIDGE") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Bridge", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    AppFooter()
                }
            }

            EditorTab.CODE -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Active File Bar (Multi-file switcher)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (uiState.isMultiFile) {
                                            viewModel.setFileExplorerOpen(true)
                                        } else {
                                            viewModel.convertToMultiFileProject()
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isMultiFile) Icons.Rounded.Folder else Icons.Rounded.Description,
                                    contentDescription = null,
                                    tint = if (uiState.isMultiFile) NeonCyan else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isMultiFile) uiState.selectedFileRelativePath else "index.html (Single File)",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (uiState.isMultiFile) "Switch File" else "Convert to Project",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Quick Import Shortcut
                            IconButton(
                                onClick = {
                                    fileImportLauncher.launch(arrayOf("*/*", "application/zip", "text/html", "text/plain"))
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FileUpload,
                                    contentDescription = "Import",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Code Snippets Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "<script>" to "<script>\n  // code\n</script>",
                            "<style>" to "<style>\n  /* css */\n</style>",
                            "<button>" to "<button onclick=\"action()\">Click</button>",
                            "AndroidBridge" to "window.AndroidBridge.showToast('Hello Native!');",
                            "Vibrate" to "window.AndroidBridge.vibrate(50);",
                            "Clipboard" to "window.AndroidBridge.copyToClipboard('Copied text');",
                            "Share" to "window.AndroidBridge.shareText('Title', 'Message body');",
                            "saveData" to "window.AndroidBridge.saveData('myKey', 'myVal');",
                            "getData" to "var val = window.AndroidBridge.getData('myKey');"
                        ).forEach { (tag, codeSnippet) ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable { viewModel.insertSnippet(codeSnippet) }
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Raw Code Editor
                    OutlinedTextField(
                        value = uiState.selectedFileContent,
                        onValueChange = { newText ->
                            viewModel.onHtmlCodeChanged(newText)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("code_editor_textarea"),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            }

            EditorTab.PREVIEW -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Slate950)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                WebEngineHelper.configureWebView(this)
                                webChromeClient = ToolWebChromeClient()
                                webViewClient = ToolWebViewClient()
                                val currentTool = Tool(
                                    id = uiState.toolId ?: 999999L,
                                    name = uiState.name,
                                    group = uiState.group,
                                    htmlCode = uiState.htmlCode,
                                    projectType = uiState.projectType,
                                    projectDir = uiState.projectDir,
                                    entryPoint = uiState.entryPoint
                                )
                                WebEngineHelper.loadTool(this, currentTool)
                                previewWebViewRef = this
                            }
                        },
                        update = { wv ->
                            previewWebViewRef = wv
                            val currentTool = Tool(
                                id = uiState.toolId ?: 999999L,
                                name = uiState.name,
                                group = uiState.group,
                                htmlCode = uiState.htmlCode,
                                projectType = uiState.projectType,
                                projectDir = uiState.projectDir,
                                entryPoint = uiState.entryPoint
                            )
                            WebEngineHelper.loadTool(wv, currentTool)
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Reload Preview Floating Pill
                    IconButton(
                        onClick = {
                            previewWebViewRef?.let { wv ->
                                val currentTool = Tool(
                                    id = uiState.toolId ?: 999999L,
                                    name = uiState.name,
                                    group = uiState.group,
                                    htmlCode = uiState.htmlCode,
                                    projectType = uiState.projectType,
                                    projectDir = uiState.projectDir,
                                    entryPoint = uiState.entryPoint
                                )
                                WebEngineHelper.loadTool(wv, currentTool)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reload Preview",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path?.substringAfterLast('/')
    }
    return result
}
