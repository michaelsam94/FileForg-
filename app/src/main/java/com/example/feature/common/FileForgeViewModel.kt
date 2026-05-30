package com.example.feature.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.di.AppContainer
import com.example.core.domain.*
import com.example.feature.rulebuilder.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class FileForgeViewModel(
    private val fileRepository: FileRepository,
    private val rulePresetRepository: RulePresetRepository,
    private val renameHistoryRepository: RenameHistoryRepository,
    private val buildPatternUseCase: BuildPatternUseCase,
    private val simulateRenameUseCase: SimulateRenameUseCase,
    private val applyRenameUseCase: ApplyRenameUseCase,
    private val undoBatchUseCase: UndoBatchUseCase,
    private val buildFolderStructureUseCase: BuildFolderStructureUseCase,
    private val exportHistoryCsvUseCase: ExportHistoryCsvUseCase
) : ViewModel() {

    // Folder selection
    private val _currentFolderUri = MutableStateFlow<String>("")
    val currentFolderUri: StateFlow<String> = _currentFolderUri.asStateFlow()

    // File lists and processing states
    private val _scannedFilesState = MutableStateFlow<UiState<List<ScannedFile>>>(UiState.Empty)
    val scannedFilesState: StateFlow<UiState<List<ScannedFile>>> = _scannedFilesState.asStateFlow()

    private val _rawFiles = MutableStateFlow<List<ScannedFile>>(emptyList())
    
    // UI Filter choices
    private val _selectedExtension = MutableStateFlow<String>("")
    val selectedExtension: StateFlow<String> = _selectedExtension.asStateFlow()

    // Active tokens for format construction
    private val _activeTokens = MutableStateFlow<List<RenameToken>>(
        listOf(
            RenameToken.OriginalName(),
            RenameToken.CustomText("_renamed"),
            RenameToken.IncrementingCounter(3, 1)
        )
    )
    val activeTokens: StateFlow<List<RenameToken>> = _activeTokens.asStateFlow()

    // Excluded individual files (by URI)
    private val _excludedUris = MutableStateFlow<Set<String>>(emptySet())
    val excludedUris: StateFlow<Set<String>> = _excludedUris.asStateFlow()

    // Simulation previews & conflicts
    val previewResults: StateFlow<List<RenameResult>> = combine(
        _rawFiles,
        _activeTokens,
        _excludedUris
    ) { files, tokens, excluded ->
        if (files.isEmpty()) emptyList()
        else {
            simulateRenameUseCase.execute(files, tokens, excluded)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered lists for the main File List display
    val filteredFiles: StateFlow<List<ScannedFile>> = combine(
        _rawFiles,
        _selectedExtension
    ) { files, ext ->
        if (ext.isEmpty()) files
        else files.filter { it.extension.lowercase() == ext.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // List of all file extensions present in scanned dataset
    val availableExtensions: StateFlow<Set<String>> = _rawFiles.map { files ->
        files.map { it.extension.lowercase() }.filter { it.isNotEmpty() }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Presets database states
    val presetsState: StateFlow<List<Pair<String, List<RenameToken>>>> = rulePresetRepository.getAllPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // History database states
    val historyState: StateFlow<List<HistoryBatch>> = renameHistoryRepository.getHistoryBatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active operational progress
    private val _operationProgress = MutableStateFlow<RenameProgress?>(null)
    val operationProgress: StateFlow<RenameProgress?> = _operationProgress.asStateFlow()

    init {
        // Preset initializer setup - insert a few default presets on startup if empty
        viewModelScope.launch {
            presetsState.collect { list ->
                if (list.isEmpty()) {
                    rulePresetRepository.savePreset(
                        "Camera IMG Auto-Date",
                        listOf(
                            RenameToken.CustomText("IMG_"),
                            RenameToken.ExifDate("yyyyMMdd_HHmmss"),
                            RenameToken.FileExtension(true)
                        )
                    )
                    rulePresetRepository.savePreset(
                        "Numbered Document Portfolio",
                        listOf(
                            RenameToken.ParentFolderName(1),
                            RenameToken.CustomText("_DOC_"),
                            RenameToken.IncrementingCounter(3, 1),
                            RenameToken.FileExtension(true)
                        )
                    )
                }
            }
        }
    }

    fun selectFolder(uriString: String) {
        _currentFolderUri.value = uriString
        _excludedUris.value = emptySet()
        _selectedExtension.value = ""
        scanFolder()
    }

    fun scanFolder() {
        val uri = _currentFolderUri.value
        if (uri.isEmpty()) return

        _scannedFilesState.value = UiState.Loading
        viewModelScope.launch {
            fileRepository.scanFolder(uri)
                .flowOn(Dispatchers.IO)
                .catch { e ->
                    _scannedFilesState.value = UiState.Error(e.message ?: "Failed to scan directory")
                }
                .collect { files ->
                    _rawFiles.value = files
                    if (files.isEmpty()) {
                        _scannedFilesState.value = UiState.Empty
                    } else {
                        _scannedFilesState.value = UiState.Success(files)
                    }
                }
        }
    }

    fun setExtensionFilter(ext: String) {
        _selectedExtension.value = ext
    }

    fun updateTokens(tokens: List<RenameToken>) {
        _activeTokens.value = tokens
    }

    fun toggleExcludeFile(uriString: String) {
        val current = _excludedUris.value.toMutableSet()
        if (current.contains(uriString)) {
            current.remove(uriString)
        } else {
            current.add(uriString)
        }
        _excludedUris.value = current
    }

    fun savePreset(name: String) {
        viewModelScope.launch {
            rulePresetRepository.savePreset(name, _activeTokens.value)
        }
    }

    fun loadPreset(tokens: List<RenameToken>) {
        _activeTokens.value = tokens
    }

    fun deletePreset(name: String) {
        viewModelScope.launch {
            rulePresetRepository.deletePreset(name)
        }
    }

    fun startRename(onComplete: (success: Int, failed: Int) -> Unit) {
        val currentResults = previewResults.value
        val batchId = UUID.randomUUID().toString()
        val rulesString = RenameToken.serializeList(_activeTokens.value)

        viewModelScope.launch {
            applyRenameUseCase.execute(batchId, rulesString, currentResults)
                .collect { progress ->
                    _operationProgress.value = progress
                    if (progress.isFinished) {
                        _operationProgress.value = null
                        // Refresh tree scanner after physical name change
                        scanFolder()
                        onComplete(progress.successCount, progress.failedCount)
                    }
                }
        }
    }

    fun undoBatch(batchId: String, onComplete: (success: Int, failed: Int) -> Unit) {
        viewModelScope.launch {
            undoBatchUseCase.execute(batchId)
                .collect { progress ->
                    _operationProgress.value = progress
                    if (progress.isFinished) {
                        _operationProgress.value = null
                        scanFolder()
                        onComplete(progress.successCount, progress.failedCount)
                    }
                }
        }
    }

    fun organizeByExif(pattern: String, destFolderUri: String, onComplete: (count: Int) -> Unit) {
        val files = _rawFiles.value
        if (files.isEmpty()) return

        _operationProgress.value = RenameProgress(0, files.size, 0, 0)
        viewModelScope.launch(Dispatchers.IO) {
            val mappings = buildFolderStructureUseCase.execute(files, pattern)
            var current = 0
            var success = 0

            for (mapping in mappings) {
                current++
                val file = mapping.file
                
                // 1. Create subfolders dynamically inside destination uri string
                var currentParentUriString = destFolderUri
                val parts = mapping.targetSubfolders.split("/")
                for (part in parts) {
                    if (part.isNotEmpty()) {
                        val created = fileRepository.createFolderIfNotExist(currentParentUriString, part)
                        if (created != null) {
                            currentParentUriString = created
                        }
                    }
                }

                // 2. Resolve duplicates in target directory using simple numeric validation suffix
                val extSuffix = if (file.extension.isNotEmpty()) ".${file.extension}" else ""
                val fullDestName = "${file.originalName}$extSuffix"
                
                // 3. Move file (Copy-then-delete in scoped storage constraints)
                val newUri = fileRepository.copyFile(file.uriString, currentParentUriString, fullDestName)
                if (newUri != null) {
                    fileRepository.deleteFile(file.uriString)
                    success++
                }

                val progressValue = RenameProgress(current, files.size, success, files.size - success)
                withContext(Dispatchers.Main) {
                    _operationProgress.value = progressValue
                }
            }

            withContext(Dispatchers.Main) {
                _operationProgress.value = null
                scanFolder()
                onComplete(success)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            renameHistoryRepository.clearAll()
        }
    }

    fun exportHistoryCsv(): String {
        return exportHistoryCsvUseCase.execute(historyState.value)
    }
}

class FileForgeViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileForgeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FileForgeViewModel(
                container.fileRepository,
                container.rulePresetRepository,
                container.renameHistoryRepository,
                container.buildPatternUseCase,
                container.simulateRenameUseCase,
                container.applyRenameUseCase,
                container.undoBatchUseCase,
                container.buildFolderStructureUseCase,
                container.exportHistoryCsvUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
