package com.example.core.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// Entities
@Entity(tableName = "rule_presets")
data class RulePresetEntity(
    @PrimaryKey val name: String,
    val serializedTokens: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "rename_history")
data class RenameHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fileCount: Int,
    val rulesUsed: String,
    val originalUri: String,
    val originalName: String,
    val newName: String,
    val undoApplied: Boolean = false
)

// DAOs
@Dao
interface RulePresetDao {
    @Query("SELECT * FROM rule_presets ORDER BY timestamp DESC")
    fun getAllPresets(): Flow<List<RulePresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: RulePresetEntity)

    @Query("DELETE FROM rule_presets WHERE name = :name")
    suspend fun deletePreset(name: String)
}

@Dao
interface RenameHistoryDao {
    @Query("SELECT * FROM rename_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<RenameHistoryEntity>>

    @Query("SELECT * FROM rename_history WHERE batchId = :batchId")
    suspend fun getHistoryByBatchId(batchId: String): List<RenameHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItems(items: List<RenameHistoryEntity>)

    @Query("UPDATE rename_history SET undoApplied = 1 WHERE batchId = :batchId")
    suspend fun markUndoApplied(batchId: String)

    @Query("DELETE FROM rename_history")
    suspend fun clearHistory()
}

// Database
@Database(
    entities = [RulePresetEntity::class, RenameHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rulePresetDao(): RulePresetDao
    abstract fun renameHistoryDao(): RenameHistoryDao
}
