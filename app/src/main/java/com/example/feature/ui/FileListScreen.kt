package com.example.feature.ui

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Hand
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.common.FileForgeViewModel
import com.example.feature.common.UiState
import com.example.feature.rulebuilder.domain.model.ScannedFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    viewModel: FileForgeViewModel,
    onNavigateToRuleBuilder: () -> Unit,
    onNavigateToOrganizer: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scannedFilesState by viewModel.scannedFilesState.collectAsState()
    val filteredFiles by viewModel.filteredFiles.collectAsState()
    val availableExtensions by viewModel.availableExtensions.collectAsState()
    val selectedExtension by viewModel.selectedExtension.collectAsState()
    val folderUri by viewModel.currentFolderUri.collectAsState()

    val simpleUriName = folderUri.substringAfterLast("%3A", "Selected Folder").replace("%2F", "/")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Scanned Assets", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            simpleUriName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.scanFolder() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Re-scan")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Auto-Organizer configuration trigger
                ExtendedFloatingActionButton(
                    onClick = onNavigateToOrganizer,
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    text = { Text("EXIF Organizer") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.testTag("organizer_fab")
                )

                // Rule-based builder trigger
                ExtendedFloatingActionButton(
                    onClick = onNavigateToRuleBuilder,
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    text = { Text("Rename Rules") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("rule_builder_fab")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = scannedFilesState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Mapping files recursively...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        }
                    }
                }
                is UiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No files found in selected directory", fontWeight = FontWeight.Bold)
                            Text("Try picking a different folder or checking sub-directories", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        }
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Folder Scan Failed", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text(state.message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        }
                    }
                }
                is UiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Filters row
                        if (availableExtensions.isNotEmpty()) {
                            Surface(
                                tonalElevation = 3.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                    Text(
                                        "Filter by layout extension:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        item {
                                            FilterChip(
                                                selected = selectedExtension.isEmpty(),
                                                onClick = { viewModel.setExtensionFilter("") },
                                                label = { Text("All (${state.data.size})") }
                                            )
                                        }
                                        items(availableExtensions.toList().sorted()) { ext ->
                                            val count = state.data.count { it.extension.lowercase() == ext }
                                            FilterChip(
                                                selected = selectedExtension == ext,
                                                onClick = { viewModel.setExtensionFilter(ext) },
                                                label = { Text("${ext.uppercase()} ($count)") }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Files grid list
                        if (filteredFiles.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No files matching filtered criteria", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredFiles, key = { it.uriString }) { file ->
                                    FileCardItem(file = file)
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun FileCardItem(file: ScannedFile) {
    val context = LocalContext.current
    val formattedSize = Formatter.formatShortFileSize(context, file.size)
    val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.dateModified))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("file_item_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail icon indicator based on extension
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (file.extension.lowercase()) {
                    "jpg", "jpeg", "png", "webp" -> Icons.Default.Home
                    "mp4", "mkv", "mov" -> Icons.Default.PlayArrow
                    "pdf", "txt", "doc" -> Icons.Default.Menu
                    else -> Icons.Default.Settings
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${file.originalName}.${file.extension}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = formattedSize,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (file.exifDate != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "EXIF capture date present",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "EXIF Capture Date: " + SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(file.exifDate)),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
