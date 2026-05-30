package com.example.core.di

import android.content.Context
import androidx.room.Room
import com.example.core.db.AppDatabase
import com.example.core.db.AndroidRenameHistoryRepository
import com.example.core.db.AndroidRulePresetRepository
import com.example.core.domain.*
import com.example.core.exif.AndroidExifReader
import com.example.core.saf.SAFFileRepository

class AppContainer(private val context: Context) {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "fileforge_db"
        ).fallbackToDestructiveMigration().build()
    }

    val exifReader: ExifReader by lazy {
        AndroidExifReader(context.applicationContext)
    }

    val fileRepository: FileRepository by lazy {
        SAFFileRepository(context.applicationContext, exifReader)
    }

    val rulePresetRepository: RulePresetRepository by lazy {
        AndroidRulePresetRepository(database.rulePresetDao())
    }

    val renameHistoryRepository: RenameHistoryRepository by lazy {
        AndroidRenameHistoryRepository(database.renameHistoryDao())
    }

    // Use cases
    val buildPatternUseCase = BuildPatternUseCase()
    val simulateRenameUseCase by lazy { SimulateRenameUseCase(buildPatternUseCase) }
    val applyRenameUseCase by lazy { ApplyRenameUseCase(fileRepository, renameHistoryRepository) }
    val undoBatchUseCase by lazy { UndoBatchUseCase(fileRepository, renameHistoryRepository) }
    val buildFolderStructureUseCase = BuildFolderStructureUseCase()
    val exportHistoryCsvUseCase = ExportHistoryCsvUseCase()
}
