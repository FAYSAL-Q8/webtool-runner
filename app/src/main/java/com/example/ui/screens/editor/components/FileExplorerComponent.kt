/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.ui.screens.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Html
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Javascript
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ProjectFileInfo
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.VibrantEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerBottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    files: List<ProjectFileInfo>,
    selectedFilePath: String,
    entryPoint: String,
    onSelectFile: (String) -> Unit,
    onCreateFile: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onRenameFile: (String, String) -> Unit,
    onSetEntryPoint: (String) -> Unit,
    onConvertOrImportZip: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ProjectFileInfo?>(null) }
    var deleteTarget by remember { mutableStateOf<ProjectFileInfo?>(null) }

    var newFileNameInput by remember { mutableStateOf("") }
    var newFolderNameInput by remember { mutableStateOf("") }
    var renameInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Slate950,
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricViolet.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderZip,
                            contentDescription = null,
                            tint = ElectricViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Project File Tree",
                            color = Slate200,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${files.size} items • Multi-file project",
                            color = Slate400,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            newFileNameInput = ""
                            showNewFileDialog = true
                        },
                        modifier = Modifier.testTag("btn_new_file")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NoteAdd,
                            contentDescription = "New File",
                            tint = NeonCyan
                        )
                    }
                    IconButton(
                        onClick = {
                            newFolderNameInput = ""
                            showNewFolderDialog = true
                        },
                        modifier = Modifier.testTag("btn_new_folder")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CreateNewFolder,
                            contentDescription = "New Folder",
                            tint = Slate200
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Slate400
                        )
                    }
                }
            }

            HorizontalDivider(color = Slate800, thickness = 1.dp)

            if (files.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No project files found", color = Slate200, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Create files or convert your single-file tool into a multi-file project.", color = Slate400, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onConvertOrImportZip) {
                            Text("Initialize Project Structure")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    items(files, key = { it.relativePath }) { file ->
                        FileItemRow(
                            file = file,
                            isSelected = file.relativePath == selectedFilePath,
                            isEntryPoint = file.relativePath == entryPoint,
                            onClick = {
                                if (!file.isDirectory) {
                                    onSelectFile(file.relativePath)
                                    onDismiss()
                                }
                            },
                            onSetEntryPoint = { onSetEntryPoint(file.relativePath) },
                            onRename = {
                                renameTarget = file
                                renameInput = file.name
                            },
                            onDelete = { deleteTarget = file }
                        )
                    }
                }
            }
        }
    }

    // New File Dialog
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("New File") },
            text = {
                Column {
                    Text("Enter relative path (e.g. style.css, js/app.js):", fontSize = 13.sp, color = Slate400)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newFileNameInput,
                        onValueChange = { newFileNameInput = it },
                        placeholder = { Text("filename.ext") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_new_filename")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileNameInput.isNotBlank()) {
                            onCreateFile(newFileNameInput.trim())
                            showNewFileDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_new_file")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                Column {
                    Text("Enter folder name (e.g. css, js, assets):", fontSize = 13.sp, color = Slate400)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newFolderNameInput,
                        onValueChange = { newFolderNameInput = it },
                        placeholder = { Text("folder_name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_new_folder")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderNameInput.isNotBlank()) {
                            onCreateFolder(newFolderNameInput.trim())
                            showNewFolderDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_new_folder")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Dialog
    renameTarget?.let { file ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename ${file.name}") },
            text = {
                Column {
                    Text("New name:", fontSize = 13.sp, color = Slate400)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank() && renameInput != file.name) {
                            val parent = file.relativePath.substringBeforeLast("/", "")
                            val newRel = if (parent.isNotBlank()) "$parent/$renameInput" else renameInput
                            onRenameFile(file.relativePath, newRel)
                            renameTarget = null
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    deleteTarget?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${file.name}?") },
            text = {
                Text("Are you sure you want to delete ${file.relativePath}? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFile(file.relativePath)
                        deleteTarget = null
                    }
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FileItemRow(
    file: ProjectFileInfo,
    isSelected: Boolean,
    isEntryPoint: Boolean,
    onClick: () -> Unit,
    onSetEntryPoint: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val iconColor = when {
        file.isDirectory -> ElectricViolet
        file.extension in listOf("html", "htm") -> Color(0xFFFB923C)
        file.extension == "css" -> NeonCyan
        file.extension in listOf("js", "mjs") -> Color(0xFFFACC15)
        file.extension == "json" -> VibrantEmerald
        file.extension in listOf("png", "jpg", "jpeg", "svg", "webp", "gif") -> Color(0xFFA855F7)
        else -> Slate400
    }

    val iconVector = when {
        file.isDirectory -> Icons.Rounded.Folder
        file.extension in listOf("html", "htm") -> Icons.Rounded.Html
        file.extension == "css" -> Icons.Rounded.Palette
        file.extension in listOf("js", "mjs") -> Icons.Rounded.Javascript
        file.extension == "json" -> Icons.Rounded.Description
        file.extension in listOf("png", "jpg", "jpeg", "svg", "webp", "gif") -> Icons.Rounded.Image
        else -> Icons.Rounded.Code
    }

    val depth = file.relativePath.count { it == '/' }
    val indent = (depth * 16).dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .padding(start = indent)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Slate800 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = file.name,
                    color = if (isSelected) Color.White else Slate200,
                    fontWeight = if (file.isDirectory || isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isEntryPoint) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = VibrantEmerald.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "ENTRY",
                            color = VibrantEmerald,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (!file.isDirectory && file.size > 0) {
                Text(
                    text = "${file.size} bytes",
                    color = Slate400,
                    fontSize = 10.sp
                )
            }
        }

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Actions",
                    tint = Slate400,
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(Slate900)
            ) {
                if (!file.isDirectory && (file.extension == "html" || file.extension == "htm") && !isEntryPoint) {
                    DropdownMenuItem(
                        text = { Text("Set as Entry Point", color = Slate200) },
                        leadingIcon = { Icon(Icons.Rounded.Star, null, tint = VibrantEmerald) },
                        onClick = {
                            menuExpanded = false
                            onSetEntryPoint()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Rename", color = Slate200) },
                    leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, null, tint = NeonCyan) },
                    onClick = {
                        menuExpanded = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = Color(0xFFEF4444)) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = Color(0xFFEF4444)) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}
