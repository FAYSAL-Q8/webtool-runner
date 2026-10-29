/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.ui.screens.settings

import com.example.ui.components.AppFooter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.AppThemeManager
import com.example.ui.theme.GlassDefaults
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.glassmorphism

enum class SettingsTab {
    APPEARANCE,
    DATA_BACKUP
}

@Composable
fun BackupSettingsDialog(
    exportedJson: String?,
    onDismiss: () -> Unit,
    onImportJson: (String) -> Unit,
    onReseedDefaults: () -> Unit
) {
    val context = LocalContext.current
    val themeConfig by AppThemeManager.themeConfig.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(SettingsTab.APPEARANCE) }
    var isImportMode by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    val activeColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            border = BorderStroke(1.5.dp, activeColor.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphism(shape = RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(activeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedTab == SettingsTab.APPEARANCE) Icons.Rounded.Palette else Icons.Rounded.Storage,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isImportMode) "Import Tools" else "Settings & Theme",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = onSurfaceColor
                        )
                    }

                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(surfaceVariantColor)
                            .border(BorderStroke(1.dp, outlineColor.copy(alpha = 0.5f)), CircleShape)
                            .testTag("backup_dialog_close")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = onSurfaceColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Top Segmented Switcher: [ Appearance ] [ Data & Backup ]
                if (!isImportMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(surfaceVariantColor)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Appearance Tab Button
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (selectedTab == SettingsTab.APPEARANCE) activeColor else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = SettingsTab.APPEARANCE }
                                .testTag("tab_appearance")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = null,
                                    tint = if (selectedTab == SettingsTab.APPEARANCE) Color.White else onSurfaceColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Appearance",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == SettingsTab.APPEARANCE) Color.White else onSurfaceColor
                                )
                            }
                        }

                        // Data & Backup Tab Button
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (selectedTab == SettingsTab.DATA_BACKUP) activeColor else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = SettingsTab.DATA_BACKUP }
                                .testTag("tab_data_backup")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Storage,
                                    contentDescription = null,
                                    tint = if (selectedTab == SettingsTab.DATA_BACKUP) Color.White else onSurfaceColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Data & Backup",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == SettingsTab.DATA_BACKUP) Color.White else onSurfaceColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (isImportMode) {
                    // Import Mode View
                    Text(
                        text = "Paste tool backup JSON payload below:",
                        fontSize = 12.sp,
                        color = onSurfaceVariantColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("import_json_input"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = onSurfaceColor
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeColor,
                            unfocusedBorderColor = outlineColor.copy(alpha = 0.5f),
                            focusedContainerColor = surfaceVariantColor,
                            unfocusedContainerColor = surfaceVariantColor
                        ),
                        placeholder = {
                            Text("{\"tools\": [...] }", color = onSurfaceVariantColor, fontSize = 11.sp)
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { isImportMode = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = surfaceVariantColor,
                                contentColor = onSurfaceColor
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Back", color = onSurfaceColor)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (importJsonText.isNotBlank()) {
                                    onImportJson(importJsonText)
                                } else {
                                    Toast.makeText(context, "Please enter JSON data", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activeColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("submit_import_button")
                        ) {
                            Text("Restore Tools", fontWeight = FontWeight.Bold)
                        }
                    }
                } else when (selectedTab) {
                    // ============================================
                    // SECTION: APPEARANCE (White / Dark mode & Accents)
                    // ============================================
                    SettingsTab.APPEARANCE -> {
                        Text(
                            text = "SWITCH THEME MODE (WHITE / DARK)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = onSurfaceVariantColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // High-contrast theme mode buttons: White, Dark, System
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple(ThemeMode.LIGHT, "White", Icons.Rounded.LightMode),
                                Triple(ThemeMode.DARK, "Dark", Icons.Rounded.DarkMode),
                                Triple(ThemeMode.SYSTEM, "System", Icons.Rounded.SettingsBrightness)
                            ).forEach { (mode, label, icon) ->
                                val isSelected = themeConfig.mode == mode
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) activeColor else surfaceVariantColor,
                                    border = BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) activeColor else outlineColor.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { AppThemeManager.setThemeMode(mode) }
                                        .testTag("theme_mode_${mode.name.lowercase()}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else onSurfaceColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (isSelected) Color.White else onSurfaceColor
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Accent Color Palette
                        Text(
                            text = "CHOOSE APP THEME COLOR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = onSurfaceVariantColor
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // 2 rows of 4 colors for spacious touch targets
                        val allPalettes = AccentPalette.values().toList()
                        val rows = allPalettes.chunked(4)
                        rows.forEachIndexed { rowIndex, rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                rowItems.forEach { palette ->
                                    val isSelected = themeConfig.accent == palette
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable { AppThemeManager.setAccent(palette) }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(palette.color)
                                                .border(
                                                    BorderStroke(
                                                        if (isSelected) 3.dp else 1.5.dp,
                                                        if (isSelected) onSurfaceColor else outlineColor.copy(alpha = 0.4f)
                                                    ),
                                                    CircleShape
                                                )
                                                .testTag("accent_${palette.name.lowercase()}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = palette.title,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) activeColor else onSurfaceVariantColor
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = outlineColor.copy(alpha = 0.25f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Live Button Clarity Preview
                        Text(
                            text = "BUTTON CLARITY PREVIEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = onSurfaceVariantColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = surfaceVariantColor,
                            border = BorderStroke(1.dp, outlineColor.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Previewing buttons with high contrast:",
                                    fontSize = 11.sp,
                                    color = onSurfaceVariantColor
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Primary Filled Action Button
                                    Button(
                                        onClick = { },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = activeColor,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Action", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    // Secondary Outlined Button
                                    OutlinedButton(
                                        onClick = { },
                                        border = BorderStroke(1.5.dp, activeColor),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Outline", color = activeColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    // Circle Icon Button
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(activeColor.copy(alpha = 0.2f))
                                            .border(BorderStroke(1.5.dp, activeColor), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.ColorLens, contentDescription = null, tint = activeColor, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // ============================================
                    // SECTION: DATA & BACKUP
                    // ============================================
                    SettingsTab.DATA_BACKUP -> {
                        Text(
                            text = "PORTABILITY & BACKUP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = onSurfaceVariantColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Export and restore your offline web tools as single JSON backups without cloud dependencies.",
                            fontSize = 12.sp,
                            color = onSurfaceVariantColor,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Export Tools Action Button
                        Button(
                            onClick = {
                                exportedJson?.let { json ->
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, json)
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TITLE, "webtools_backup.json")
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Export WebTools Backup")
                                    context.startActivity(shareIntent)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_tools_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activeColor,
                                contentColor = Color.White
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export Backup JSON", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Copy to clipboard button
                        Button(
                            onClick = {
                                exportedJson?.let { json ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("webtools_backup.json", json)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied backup JSON to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.dp, outlineColor.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
                                .testTag("copy_json_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = surfaceVariantColor,
                                contentColor = onSurfaceColor
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = onSurfaceColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy Backup to Clipboard", color = onSurfaceColor, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Import Tools Button
                        Button(
                            onClick = { isImportMode = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.dp, activeColor.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
                                .testTag("open_import_mode_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = surfaceVariantColor,
                                contentColor = onSurfaceColor
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.FileUpload, contentDescription = null, tint = activeColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Import Tools from Backup", color = onSurfaceColor, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Restore Default Tools
                        Button(
                            onClick = {
                                onReseedDefaults()
                                Toast.makeText(context, "Default tools restored", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reseed_defaults_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = activeColor
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = activeColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restore Built-In Presets", color = activeColor, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        AppFooter()
                    }
                }
            }
        }
    }
}
