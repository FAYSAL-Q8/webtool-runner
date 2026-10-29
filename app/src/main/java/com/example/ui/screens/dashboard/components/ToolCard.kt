/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.ui.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Tool
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
import com.example.ui.theme.glassmorphism
import java.io.File

@Composable
fun ToolCard(
    tool: Tool,
    onOpen: (Tool) -> Unit,
    onEdit: (Tool) -> Unit,
    onDelete: (Tool) -> Unit,
    onWipeStorage: (Tool) -> Unit,
    onExportZip: (Tool) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // Generative accent gradient for fallback preview
    val cardGradient = remember(tool.name, tool.group) {
        val hash = (tool.name.hashCode() + tool.group.hashCode()).let { if (it < 0) -it else it }
        val palettes = listOf(
            listOf(MidnightIndigo, ElectricViolet),
            listOf(Color(0xFF0F766E), NeonCyan),
            listOf(Color(0xFF7C3AED), Color(0xFFC084FC)),
            listOf(Color(0xFFBE185D), Color(0xFFFB7185)),
            listOf(Color(0xFF1E293B), Color(0xFF475569))
        )
        palettes[hash % palettes.size]
    }

    val thumbnailExists = remember(tool.thumbnailPath) {
        tool.thumbnailPath?.let { File(it).exists() } == true
    }

    Box(
        modifier = modifier
            .testTag("tool_card_${tool.id}")
            .aspectRatio(0.85f)
            .glassmorphism(shape = RoundedCornerShape(GlassDefaults.RadiusMedium))
            .clickable { onOpen(tool) }
    ) {
        // Thumbnail or Generative Fallback
        if (thumbnailExists && tool.thumbnailPath != null) {
            AsyncImage(
                model = File(tool.thumbnailPath),
                contentDescription = tool.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(GlassDefaults.RadiusMedium))
            )
        } else {
            // Elegant generative background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(cardGradient)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Code,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tool.group.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Dark gradient scrim overlay at the bottom for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x20000000),
                            Color(0xCC090D16)
                        ),
                        startY = 60f
                    )
                )
        )

        // Top Header: Group tag pill & Options Menu Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group and Project Type pill badges
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(GlassDefaults.RadiusSmall),
                    color = Slate950.copy(alpha = 0.65f),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = tool.group,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate200,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        maxLines = 1
                    )
                }

                if (tool.isMultiFile) {
                    Surface(
                        shape = RoundedCornerShape(GlassDefaults.RadiusSmall),
                        color = ElectricViolet.copy(alpha = 0.85f),
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderZip,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "ZIP",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // High-contrast quick options menu trigger
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(BorderStroke(1.2.dp, Color.White.copy(alpha = 0.55f)), CircleShape)
                        .testTag("tool_card_menu_${tool.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Launch", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {
                            menuExpanded = false
                            onOpen(tool)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Code", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {
                            menuExpanded = false
                            onEdit(tool)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export as ZIP", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Download, contentDescription = null, tint = NeonCyan)
                        },
                        onClick = {
                            menuExpanded = false
                            onExportZip(tool)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Wipe Storage", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(Icons.Rounded.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        onClick = {
                            menuExpanded = false
                            onWipeStorage(tool)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = CoralRose, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Delete, contentDescription = null, tint = CoralRose)
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete(tool)
                        }
                    )
                }
            }
        }

        // Bottom Info Bar with Glass badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(8.dp)
                .glassmorphism(
                    shape = RoundedCornerShape(10.dp),
                    customAlpha = 0.8f
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = tool.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
