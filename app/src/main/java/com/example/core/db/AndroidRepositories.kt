package com.example.core.db

import com.example.core.domain.HistoryBatch
import com.example.core.domain.HistoryFileItem
import com.example.core.domain.RenameHistoryRepository
import com.example.core.domain.RulePresetRepository
import com.example.feature.rulebuilder.domain.model.RenameToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AndroidRulePresetRepository(private val dao: RulePresetDao) : RulePresetRepository {
    override fun getAllPresets(): Flow<List<Pair<String, List<RenameToken>>>> {
        return dao.getAllPresets().map { list ->
            list.map { entity ->
                entity.name to RenameToken.deserializeList(entity.serializedTokens)
            }
        }
    }

    override suspend fun savePreset(name: String, tokens: List<RenameToken>) {
        val serialized = RenameToken.serializeList(tokens)
        dao.insertPreset(RulePresetEntity(name, serialized))
    }

    override suspend fun deletePreset(name: String) {
        dao.deletePreset(name)
    }
}

class AndroidRenameHistoryRepository(private val dao: RenameHistoryDao) : RenameHistoryRepository {
    override fun getHistoryBatches(): Flow<List<HistoryBatch>> {
        return dao.getAllHistory().map { list ->
            list.groupBy { it.batchId }.map { (batchId, entities) ->
                val first = entities.first()
                HistoryBatch(
                    batchId = batchId,
                    timestamp = first.timestamp,
                    fileCount = first.fileCount,
                    rulesUsed = first.rulesUsed,
                    undoApplied = first.undoApplied,
                    items = entities.map {
                        HistoryFileItem(
                            originalUri = it.originalUri,
                            originalName = it.originalName,
                            newName = it.newName
                        )
                    }
                )
            }.sortedByDescending { it.timestamp }
        }
    }

    override suspend fun getBatch(batchId: String): List<HistoryFileItem> {
        return dao.getHistoryByBatchId(batchId).map {
            HistoryFileItem(
                originalUri = it.originalUri,
                originalName = it.originalName,
                newName = it.newName
            )
        }
    }

    override suspend fun addHistoryBatch(batchId: String, rulesUsed: String, items: List<HistoryFileItem>) {
        val fileCount = items.size
        val entities = items.map {
            RenameHistoryEntity(
                batchId = batchId,
                fileCount = fileCount,
                rulesUsed = rulesUsed,
                originalUri = it.originalUri,
                originalName = it.originalName,
                newName = it.newName,
                undoApplied = false
            )
        }
        dao.insertHistoryItems(entities)
    }

    override suspend fun markUndoApplied(batchId: String) {
        dao.markUndoApplied(batchId)
    }

    override suspend fun clearAll() {
        dao.clearHistory()
    }
}
