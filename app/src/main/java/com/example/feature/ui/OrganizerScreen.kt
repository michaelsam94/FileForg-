package com.example.feature.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.feature.common.FileForgeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerScreen(
    viewModel: FileForgeViewModel,
    onOrganizerExecuted: (count: Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val operationProgress by viewModel.operationProgress.collectAsState()
    
    var destinationUriString by remember { mutableStateOf("") }
    var selectedPattern by remember { mutableStateOf("[Year]/[Month]") }
    var showConfirmOrganizeDialog by remember { mutableStateOf(false) }

    val destFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {}
            destinationUriString = uri.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EXIF Organizer", fontWeight = FontWeight.Bold) },
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
                Button(
                    onClick = { showConfirmOrganizeDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    enabled = destinationUriString.isNotEmpty() && operationProgress == null,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("apply_exif_organize_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Library Sorting Plan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Organize media libraries physically based on image capture dates.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 14.sp
            )

            // Step 1: Destination Selection
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Step 1: Choose Target Location", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (destinationUriString.isEmpty()) {
                        Button(
                            onClick = { destFolderLauncher.launch(null) },
                            modifier = Modifier.fillMaxWidth().testTag("select_organize_dest_button")
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Destination Directory")
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected: " + destinationUriString.substringAfterLast("%3A").replace("%2F", "/"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { destFolderLauncher.launch(null) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Change destination", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Step 2: Choose folder design tree layout rules
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Step 2: Define Storage Trees Strategy", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    val patternOptions = listOf(
                        "[Year]/[Month]" to "Year Subfolder / Month subfolder\n(e.g., 2026/05/pic.jpg)",
                        "[Year]/[Month]/[Day]" to "Year Subfolder / Month / Day\n(e.g., 2026/05/30/pic.jpg)",
                        "[Year]-[Month]" to "Flat Year-Month subfolder\n(e.g., 2026-05/pic.jpg)",
                        "[Year]-[Month]-[Day]" to "Flat Year-Month-Day subfolder\n(e.g., 2026-05-30/pic.jpg)"
                    )

                    patternOptions.forEach { (pattern, subtitle) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedPattern == pattern) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPattern == pattern,
                                onClick = { selectedPattern = pattern }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(pattern, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                Text(
                    "Note: Any items lacking EXIF timestamps will neatly reside under a newly created '_unsorted' backup root partition.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        // Execution progress block
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
                        Text("Copy and organizing files...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Moved: ${progress.current} / ${progress.total}\nSuccesses: ${progress.successCount}",
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (showConfirmOrganizeDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmOrganizeDialog = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Confirm Organization Drive") },
                text = { Text("Proceed to sort files according to $selectedPattern rules? Files will be physically copied into target directory branches and removed from their source paths afterwards.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmOrganizeDialog = false
                            viewModel.organizeByExif(selectedPattern, destinationUriString) { count ->
                                onOrganizerExecuted(count)
                            }
                        },
                        modifier = Modifier.testTag("confirm_apply_organize_button")
                    ) {
                        Text("Sort Library")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmOrganizeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


