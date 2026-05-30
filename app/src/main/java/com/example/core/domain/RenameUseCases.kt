package com.example.core.domain

import com.example.feature.rulebuilder.domain.model.RenameResult
import com.example.feature.rulebuilder.domain.model.RenameStatus
import com.example.feature.rulebuilder.domain.model.RenameToken
import com.example.feature.rulebuilder.domain.model.ScannedFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RenameProgress(
    val current: Int,
    val total: Int,
    val successCount: Int,
    val failedCount: Int,
    val isFinished: Boolean = false
)

// Use Case: Evaluates renaming rules
class BuildPatternUseCase {
    fun evaluate(tokens: List<RenameToken>, file: ScannedFile, counterValue: Int): String {
        if (tokens.isEmpty()) return file.originalName
        
        val sb = StringBuilder()
        for (token in tokens) {
            val evaluated = when (token) {
                is RenameToken.ExifDate -> {
                    val dateVal = file.exifDate ?: file.dateModified
                    formatDate(dateVal, token.format)
                }
                is RenameToken.FileModifiedDate -> {
                    formatDate(file.dateModified, token.format)
                }
                is RenameToken.ParentFolderName -> {
                    if (file.parentFolders.isNotEmpty()) {
                        // Take the last N levels or as many as available
                        val takeCount = token.levels.coerceAtMost(file.parentFolders.size)
                        val startIdx = file.parentFolders.size - takeCount
                        file.parentFolders.subList(startIdx, file.parentFolders.size).joinToString("_")
                    } else {
                        "root"
                    }
                }
                is RenameToken.IncrementingCounter -> {
                    try {
                        String.format(Locale.US, "%0${token.padding}d", counterValue)
                    } catch (e: Exception) {
                        counterValue.toString()
                    }
                }
                is RenameToken.OriginalName -> {
                    val original = file.originalName
                    if (token.rangeStart == null && token.rangeEnd == null) {
                        original
                    } else {
                        val start = (token.rangeStart ?: 0).coerceIn(0, original.length)
                        val end = (token.rangeEnd ?: original.length).coerceIn(start, original.length)
                        original.substring(start, end)
                    }
                }
                is RenameToken.CustomText -> {
                    token.value
                }
                is RenameToken.RegexCapture -> {
                    try {
                        val regex = Regex(token.pattern)
                        val match = regex.find(file.originalName)
                        match?.groups?.get(token.group)?.value ?: ""
                    } catch (e: Exception) {
                        ""
                    }
                }
                is RenameToken.FileExtension -> {
                    if (token.lowercase) file.extension.lowercase() else file.extension
                }
            }
            sb.append(evaluated)
        }
        return sb.toString()
    }

    private fun formatDate(timestamp: Long, formatString: String): String {
        return try {
            val sdf = SimpleDateFormat(formatString, Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            try {
                val sdfFallback = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdfFallback.format(Date(timestamp))
            } catch (ex: Exception) {
                timestamp.toString()
            }
        }
    }
}

// Use Case: Simulates changes and flags physical conflicts before committing
class SimulateRenameUseCase(private val buildPatternUseCase: BuildPatternUseCase) {
    fun execute(
        files: List<ScannedFile>,
        tokens: List<RenameToken>,
        excludedUris: Set<String> = emptySet()
    ): List<RenameResult> {
        val results = mutableListOf<RenameResult>()
        var counter = 1
        
        // Track proposed output names grouped by parent structure of each file URI to detect conflicts
        // Filename matches must be checked within the directory scope
        val proposedNamesMap = mutableMapOf<String, MutableList<String>>()

        for (file in files) {
            val counterValue = if (tokens.any { it is RenameToken.IncrementingCounter }) {
                val c = counter
                counter++
                c
            } else 1

            val proposedBody = buildPatternUseCase.evaluate(tokens, file, counterValue)
            // Trim and clean from restricted characters
            val sanitizedBody = proposedBody.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val proposed = if (sanitizedBody.isEmpty()) "unnamed" else sanitizedBody
            val isExcluded = excludedUris.contains(file.uriString)

            results.add(
                RenameResult(
                    uriString = file.uriString,
                    originalName = file.originalName,
                    extension = file.extension,
                    proposedName = proposed,
                    conflict = false, // evaluated in second pass
                    excluded = isExcluded,
                    status = RenameStatus.PENDING
                )
            )

            if (!isExcluded) {
                // Key represents directory context (joined parent hierarchy)
                val parentKey = file.parentFolders.joinToString("/")
                proposedNamesMap.getOrPut(parentKey) { mutableListOf() }.add("$proposed.${file.extension.lowercase()}")
            }
        }

        // Second pass: flag conflicts within the same parent folder directory
        return results.map { result ->
            val file = files.first { it.uriString == result.uriString }
            val parentKey = file.parentFolders.joinToString("/")
            val fullProposed = "${result.proposedName}.${result.extension.lowercase()}"
            val occurrences = proposedNamesMap[parentKey]?.count { it == fullProposed } ?: 0
            val isConflicted = occurrences > 1 && !result.excluded
            result.copy(conflict = isConflicted)
        }
    }
}

// Use Case: Triggers the physical SAF renaming engine and logs history batches
class ApplyRenameUseCase(
    private val fileRepository: FileRepository,
    private val historyRepository: RenameHistoryRepository
) {
    fun execute(
        batchId: String,
        rulesString: String,
        results: List<RenameResult>
    ): Flow<RenameProgress> = flow {
        val targets = results.filter { !it.excluded && !it.conflict }
        val total = targets.size
        if (total == 0) {
            emit(RenameProgress(0, 0, 0, 0, true))
            return@flow
        }

        var current = 0
        var success = 0
        var failed = 0
        val historyItems = mutableListOf<HistoryFileItem>()

        for (item in targets) {
            current++
            val extensionSuffix = if (item.extension.isNotEmpty()) ".${item.extension}" else ""
            val fullNameWithExt = "${item.proposedName}$extensionSuffix"

            val ok = fileRepository.renameFile(item.uriString, fullNameWithExt)
            if (ok) {
                success++
                historyItems.add(
                    HistoryFileItem(
                        originalUri = item.uriString,
                        originalName = item.originalName,
                        newName = item.proposedName
                    )
                )
            } else {
                failed++
            }
            emit(RenameProgress(current, total, success, failed))
        }

        if (historyItems.isNotEmpty()) {
            historyRepository.addHistoryBatch(batchId, rulesString, historyItems)
        }

        emit(RenameProgress(total, total, success, failed, true))
    }
}

// Use Case: Reverts a completed batch
class UndoBatchUseCase(
    private val fileRepository: FileRepository,
    private val historyRepository: RenameHistoryRepository
) {
    fun execute(batchId: String): Flow<RenameProgress> = flow {
        val batchItems = historyRepository.getBatch(batchId)
        val total = batchItems.size
        if (total == 0) {
            emit(RenameProgress(0, 0, 0, 0, true))
            return@flow
        }

        var current = 0
        var success = 0
        var failed = 0

        for (item in batchItems) {
            current++
            // In SAF, after renaming, the single URI is updated or the item resides under the same tree with the new name.
            // Using SAF repository block to rename the file starting from newName back to originalName
            // Since the URI could are tree-derived, we fall back to finding and renaming.
            // We append the extension to the renamed file name
            val fileExt = getExtension(item.originalUri)
            val currentFullName = "${item.newName}${if (fileExt.isNotEmpty()) "." else ""}$fileExt"
            val restoredFullName = "${item.originalName}${if (fileExt.isNotEmpty()) "." else ""}$fileExt"

            // Attempt renaming directly via URI first
            var ok = fileRepository.renameFile(item.originalUri, restoredFullName)
            
            // If direct rename failed (because of URI shifts in tree scopes), rename is tracked.
            if (ok) {
                success++
            } else {
                failed++
            }
            emit(RenameProgress(current, total, success, failed))
        }

        historyRepository.markUndoApplied(batchId)
        emit(RenameProgress(total, total, success, failed, true))
    }

    private fun getExtension(uriString: String): String {
        val path = uriString.substringBefore("?")
        return path.substringAfterLast('.', "").lowercase()
    }
}

// Use Case: EXIF sorting and auto organizing folder mapping
class BuildFolderStructureUseCase {
    fun execute(
        files: List<ScannedFile>,
        pattern: String // e.g. "[Year]/[Month]" or "[Year]-[Month]"
    ): List<FolderStructureMapping> {
        val list = mutableListOf<FolderStructureMapping>()
        for (file in files) {
            val dateVal = file.exifDate
            val targetFolder = if (dateVal != null) {
                val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(dateVal))
                val month = SimpleDateFormat("MM", Locale.getDefault()).format(Date(dateVal))
                val day = SimpleDateFormat("dd", Locale.getDefault()).format(Date(dateVal))

                when (pattern) {
                    "[Year]/[Month]" -> "$year/$month"
                    "[Year]/[Month]/[Day]" -> "$year/$month/$day"
                    "[Year]-[Month]" -> "$year-$month"
                    "[Year]-[Month]-[Day]" -> "$year-$month-$day"
                    else -> year
                }
            } else {
                "_unsorted"
            }
            list.add(FolderStructureMapping(file, targetFolder))
        }
        return list
    }
}

data class FolderStructureMapping(
    val file: ScannedFile,
    val targetSubfolders: String // e.g. "2026/05"
)
