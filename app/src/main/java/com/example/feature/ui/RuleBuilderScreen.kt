package com.example.feature.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.feature.common.FileForgeViewModel
import com.example.feature.rulebuilder.domain.model.RenameToken
import com.example.feature.rulebuilder.domain.model.ScannedFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleBuilderScreen(
    viewModel: FileForgeViewModel,
    onNavigateToPreview: () -> Unit,
    onBack: () -> Unit
) {
    val activeTokens by viewModel.activeTokens.collectAsState()
    val previewResults by viewModel.previewResults.collectAsState()
    val presets by viewModel.presetsState.collectAsState()

    var showPresetDialog by remember { mutableStateOf(false) }
    var showAddTokenSheet by remember { mutableStateOf(false) }
    var activeTokenToConfigure by remember { mutableStateOf<Pair<Int, RenameToken>?>(null) }
    var presetNameInput by remember { mutableStateOf("") }
    var showSavePresetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pattern Rules Builder", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                },
                actions = {
                    // Presets trigger
                    TextButton(
                        onClick = { showPresetDialog = true },
                        modifier = Modifier.testTag("presets_lib_button")
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Presets")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showSavePresetDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(52.dp).testTag("save_preset_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Preset")
                    }

                    Button(
                        onClick = onNavigateToPreview,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f).height(52.dp).testTag("preview_rename_button")
                    ) {
                        Text("Generate Preview", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Live Output Card
            Text(
                "Live Output Previews:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().height(160.dp)
            ) {
                if (previewResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Add format tokens to construct file schema", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(previewResults.take(4).size) { index ->
                            val result = previewResults[index]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${result.originalName}.${result.extension}",
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp).padding(horizontal = 6.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${result.proposedName}.${result.extension}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1.2f),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (previewResults.size > 4) {
                            item {
                                Text(
                                    "And ${previewResults.size - 4} more files...",
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Horizonally scrollable Format Builder Row
            Text(
                "Rename Schema Format Bar:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activeTokens.forEachIndexed { index, token ->
                    TokenBlock(
                        token = token,
                        onClick = { activeTokenToConfigure = index to token },
                        onMoveLeft = if (index > 0) { {
                            val mutable = activeTokens.toMutableList()
                            val temp = mutable[index]
                            mutable[index] = mutable[index - 1]
                            mutable[index - 1] = temp
                            viewModel.updateTokens(mutable)
                        } } else null,
                        onMoveRight = if (index < activeTokens.size - 1) { {
                            val mutable = activeTokens.toMutableList()
                            val temp = mutable[index]
                            mutable[index] = mutable[index + 1]
                            mutable[index + 1] = temp
                            viewModel.updateTokens(mutable)
                        } } else null,
                        onDelete = {
                            val mutable = activeTokens.toMutableList()
                            mutable.removeAt(index)
                            viewModel.updateTokens(mutable)
                        }
                    )
                }

                // Add token visual placeholder card
                Box(
                    modifier = Modifier
                        .size(height = 68.dp, width = 96.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable { showAddTokenSheet = true }
                        .testTag("add_token_placeholder"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add naming token",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "Add Rule",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "💡 Tip: Tap on any active block to modify its formatting parameters (e.g. counter padding, EXIF patterns, substrings, or text characters). Use side buttons on panels to slide reorder sequences.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }

    // Modal overlay: Presets List
    if (showPresetDialog) {
        Dialog(onDismissRequest = { showPresetDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Preset Library", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
                    
                    if (presets.isEmpty()) {
                        Text("No saved presets found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 280.dp)) {
                            items(presets.size) { idx ->
                                val (name, tokens) = presets[idx]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.loadPreset(tokens)
                                            showPresetDialog = false
                                        }
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${tokens.size} tokens", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { viewModel.deletePreset(name) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete preset", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showPresetDialog = false },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }

    // Modal overlay: Save Preset Name Dialog
    if (showSavePresetDialog) {
        Dialog(onDismissRequest = { showSavePresetDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Save Preset", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = presetNameInput,
                        onValueChange = { presetNameInput = it },
                        label = { Text("Preset Identifier Name") },
                        modifier = Modifier.fillMaxWidth().testTag("preset_name_input")
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showSavePresetDialog = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (presetNameInput.trim().isNotEmpty()) {
                                    viewModel.savePreset(presetNameInput.trim())
                                    presetNameInput = ""
                                    showSavePresetDialog = false
                                }
                            },
                            enabled = presetNameInput.trim().isNotEmpty(),
                            modifier = Modifier.testTag("confirm_save_preset")
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // Modal overlay: Add Token Palette picker
    if (showAddTokenSheet) {
        Dialog(onDismissRequest = { showAddTokenSheet = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Choose Rule Token", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
                    
                    val options = listOf(
                        "Original Filename" to RenameToken.OriginalName(),
                        "Incrementing Counter" to RenameToken.IncrementingCounter(),
                        "EXIF Creation Date" to RenameToken.ExifDate(),
                        "File Modified Date" to RenameToken.FileModifiedDate(),
                        "Custom Static Text" to RenameToken.CustomText(""),
                        "Parent Directory Name" to RenameToken.ParentFolderName(),
                        "Regex Group Capture" to RenameToken.RegexCapture("", 1),
                        "Output Extension" to RenameToken.FileExtension()
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(options.size) { idx ->
                            val (label, token) = options[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val mutable = activeTokens.toMutableList()
                                        mutable.add(token)
                                        viewModel.updateTokens(mutable)
                                        showAddTokenSheet = false
                                    }
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (token) {
                                        is RenameToken.OriginalName -> Icons.Default.List
                                        is RenameToken.IncrementingCounter -> Icons.Default.List
                                        is RenameToken.ExifDate -> Icons.Default.Info
                                        is RenameToken.FileModifiedDate -> Icons.Default.Refresh
                                        is RenameToken.CustomText -> Icons.Default.Create
                                        is RenameToken.ParentFolderName -> Icons.Default.Home
                                        is RenameToken.RegexCapture -> Icons.Default.Settings
                                        is RenameToken.FileExtension -> Icons.Default.Build
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { showAddTokenSheet = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    // Modal overlay: Config individual token values
    activeTokenToConfigure?.let { (index, token) ->
        Dialog(onDismissRequest = { activeTokenToConfigure = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                var currentToken by remember(token) { mutableStateOf(token) }

                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Configure Rule Token", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    when (val ct = currentToken) {
                        is RenameToken.CustomText -> {
                            OutlinedTextField(
                                value = ct.value,
                                onValueChange = { currentToken = ct.copy(value = it) },
                                label = { Text("Custom Text Characters") },
                                modifier = Modifier.fillMaxWidth().testTag("token_custom_text_input")
                            )
                        }
                        is RenameToken.IncrementingCounter -> {
                            var padInput by remember { mutableStateOf(ct.padding.toString()) }
                            var startInput by remember { mutableStateOf(ct.start.toString()) }

                            OutlinedTextField(
                                value = padInput,
                                onValueChange = {
                                    padInput = it
                                    currentToken = ct.copy(padding = it.toIntOrNull() ?: ct.padding)
                                },
                                label = { Text("Padding Size (e.g., 3 for 001)") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            )
                            OutlinedTextField(
                                value = startInput,
                                onValueChange = {
                                    startInput = it
                                    currentToken = ct.copy(start = it.toIntOrNull() ?: ct.start)
                                },
                                label = { Text("Starting Counter Index") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is RenameToken.ExifDate -> {
                            OutlinedTextField(
                                value = ct.format,
                                onValueChange = { currentToken = ct.copy(format = it) },
                                label = { Text("Date Pattern Format (e.g. yyyy-MM-dd)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is RenameToken.FileModifiedDate -> {
                            OutlinedTextField(
                                value = ct.format,
                                onValueChange = { currentToken = ct.copy(format = it) },
                                label = { Text("Date Pattern Format (e.g. yyyy-MM-dd)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is RenameToken.OriginalName -> {
                            var startInput by remember { mutableStateOf(ct.rangeStart?.toString() ?: "") }
                            var endInput by remember { mutableStateOf(ct.rangeEnd?.toString() ?: "") }

                            Text("Exclude boundaries or slice (Blank for Full Range):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = startInput,
                                onValueChange = {
                                    startInput = it
                                    currentToken = ct.copy(rangeStart = it.toIntOrNull())
                                },
                                label = { Text("Substring Start Character Index") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            )
                            OutlinedTextField(
                                value = endInput,
                                onValueChange = {
                                    endInput = it
                                    currentToken = ct.copy(rangeEnd = it.toIntOrNull())
                                },
                                label = { Text("Substring End Character Index") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is RenameToken.ParentFolderName -> {
                            var levelsInput by remember { mutableStateOf(ct.levels.toString()) }
                            OutlinedTextField(
                                value = levelsInput,
                                onValueChange = {
                                    levelsInput = it
                                    currentToken = ct.copy(levels = it.toIntOrNull() ?: ct.levels)
                                },
                                label = { Text("Parent Folder Depth Index") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is RenameToken.RegexCapture -> {
                            OutlinedTextField(
                                value = ct.pattern,
                                onValueChange = { currentToken = ct.copy(pattern = it) },
                                label = { Text("Regex Matching String") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            )
                            var groupInput by remember { mutableStateOf(ct.group.toString()) }
                            OutlinedTextField(
                                value = groupInput,
                                onValueChange = {
                                    groupInput = it
                                    currentToken = ct.copy(group = it.toIntOrNull() ?: ct.group)
                                },
                                label = { Text("Regex Capturing Group Index") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is RenameToken.FileExtension -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Force Lowercase")
                                Switch(
                                    checked = ct.lowercase,
                                    onCheckedChange = { currentToken = ct.copy(lowercase = it) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { activeTokenToConfigure = null }) {
                            Text("Dismiss")
                        }
                        Button(
                            onClick = {
                                val mutable = activeTokens.toMutableList()
                                mutable[index] = currentToken
                                viewModel.updateTokens(mutable)
                                activeTokenToConfigure = null
                            },
                            modifier = Modifier.testTag("apply_token_config")
                        ) {
                            Text("Apply Settings")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TokenBlock(
    token: RenameToken,
    onClick: () -> Unit,
    onMoveLeft: (() -> Unit)?,
    onMoveRight: (() -> Unit)?,
    onDelete: () -> Unit
) {
    val blockTitle = when (token) {
        is RenameToken.OriginalName -> {
            if (token.rangeStart == null && token.rangeEnd == null) "Original Name"
            else "Original [${token.rangeStart ?: 0}:${token.rangeEnd ?: ""}]"
        }
        is RenameToken.IncrementingCounter -> "Counter (pad:${token.padding})"
        is RenameToken.ExifDate -> "EXIF Date"
        is RenameToken.FileModifiedDate -> "Modified Date"
        is RenameToken.CustomText -> if (token.value.isEmpty()) "Text (Empty)" else "Text: \"${token.value}\""
        is RenameToken.ParentFolderName -> "Parent (depth:${token.levels})"
        is RenameToken.RegexCapture -> "Regex"
        is RenameToken.FileExtension -> "Extension"
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .size(height = 68.dp, width = 164.dp)
            .clickable(onClick = onClick)
            .testTag("token_block_item")
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Accessible Reorder Controls: Move Left
            if (onMoveLeft != null) {
                IconButton(onClick = onMoveLeft, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Move left",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }

            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = blockTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Configure ⚙️",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }

            // Accessible Reorder Controls: Move Right
            if (onMoveRight != null) {
                IconButton(onClick = onMoveRight, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Move right",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }

            // Delete control
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove block",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
