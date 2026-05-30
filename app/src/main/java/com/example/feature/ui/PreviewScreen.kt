package com.example.feature.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.feature.common.FileForgeViewModel
import com.example.feature.rulebuilder.domain.model.RenameResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    viewModel: FileForgeViewModel,
    onRenameExecuted: (successCount: Int, failedCount: Int) -> Unit,
    onBack: () -> Unit
) {
    val previewResults by viewModel.previewResults.collectAsState()
    val operationProgress by viewModel.operationProgress.collectAsState()
    val excludedUris by viewModel.excludedUris.collectAsState()

    var showConfirmDialog by remember { mutableStateOf(false) }
    val conflictsCount = previewResults.count { it.conflict }
    val excludedCount = previewResults.count { it.excluded }
    val totalToRename = previewResults.size - excludedCount - conflictsCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safe Rename Simulation", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Conflicts alert
                    if (conflictsCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "$conflictsCount files have naming conflicts! Conflicted items will be skipped automatically to safeguard data.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { showConfirmDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        enabled = totalToRename > 0 && operationProgress == null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (conflictsCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("apply_rename_execution_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Apply Rename of $totalToRename Assets",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header overview counts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${previewResults.size} files total",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        "${totalToRename} to execute • ${excludedCount} excluded",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Grid list of items before/after
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(previewResults, key = { it.uriString }) { result ->
                        SimulationResultItem(
                            result = result,
                            onToggleExclude = { viewModel.toggleExcludeFile(result.uriString) }
                        )
                    }
                }
            }

            // Ongoing batch rename loading overlay dialog
            operationProgress?.let { progress ->
                Dialog(onDismissRequest = {}) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                progress = { if (progress.total > 0) progress.current.toFloat() / progress.total.toFloat() else 0f },
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                "Executing renaming rules...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Renamed: ${progress.current} / ${progress.total}\nSuccesses: ${progress.successCount} • Failures: ${progress.failedCount}",
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(
                                progress = { if (progress.total > 0) progress.current.toFloat() / progress.total.toFloat() else 0f },
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }

            // Validation confirmation Dialog before physical action
            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    title = { Text("Confirm Bulk Rename") },
                    text = { Text("You are about to execute renaming logic on $totalToRename physical files on device. This operation is fully offline and creates historic backups that let you undo the changes if needed. Proceed?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showConfirmDialog = false
                                viewModel.startRename { success, failed ->
                                    onRenameExecuted(success, failed)
                                }
                            },
                            modifier = Modifier.testTag("confirm_apply_rename_button")
                        ) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SimulationResultItem(
    result: RenameResult,
    onToggleExclude: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("preview_result_item_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                result.excluded -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                result.conflict -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exclude toggle checkbox
            Checkbox(
                checked = !result.excluded,
                onCheckedChange = { onToggleExclude() },
                enabled = !result.conflict,
                modifier = Modifier.testTag("exclude_item_checkbox")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                // BEFORE name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Before: ",
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${result.originalName}.${result.extension}",
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // AFTER name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "After:   ",
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${result.proposedName}.${result.extension}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (result.conflict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                // If conflict flagged
                if (result.conflict) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Conflict Alert",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Conflicting output filename!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
