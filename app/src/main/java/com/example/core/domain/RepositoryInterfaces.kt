package com.example.core.domain

import com.example.feature.rulebuilder.domain.model.RenameToken
import com.example.feature.rulebuilder.domain.model.ScannedFile
import kotlinx.coroutines.flow.Flow

// Presets Domain repository
interface RulePresetRepository {
    fun getAllPresets(): Flow<List<Pair<String, List<RenameToken>>>>
    suspend fun savePreset(name: String, tokens: List<RenameToken>)
    suspend fun deletePreset(name: String)
}

// History Domain repository
data class HistoryBatch(
    val batchId: String,
    val timestamp: Long,
    val fileCount: Int,
    val rulesUsed: String,
    val items: List<HistoryFileItem>,
    val undoApplied: Boolean
)

data class HistoryFileItem(
    val originalUri: String,
    val originalName: String,
    val newName: String
)

interface RenameHistoryRepository {
    fun getHistoryBatches(): Flow<List<HistoryBatch>>
    suspend fun getBatch(batchId: String): List<HistoryFileItem>
    suspend fun addHistoryBatch(batchId: String, rulesUsed: String, items: List<HistoryFileItem>)
    suspend fun markUndoApplied(batchId: String)
    suspend fun clearAll()
}

// File system interactions repository
interface FileRepository {
    fun scanFolder(folderUriString: String): Flow<List<ScannedFile>>
    suspend fun renameFile(uriString: String, newNameWithExt: String): Boolean
    suspend fun createFolderIfNotExist(parentUriString: String, folderName: String): String?
    suspend fun copyFile(sourceUriString: String, destFolderUriString: String, destNameWithExt: String): String?
    suspend fun deleteFile(uriString: String): Boolean
}

// Metadata extraction repository
interface ExifReader {
    fun getExifCaptureDate(uriString: String): Long?
}
