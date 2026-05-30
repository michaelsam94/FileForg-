package com.example.feature.rulebuilder.domain.model

data class ScannedFile(
    val uriString: String,
    val originalName: String,
    val extension: String,
    val size: Long,
    val dateModified: Long,
    val exifDate: Long? = null,
    val parentFolders: List<String> = emptyList()
)

data class RenameResult(
    val uriString: String,
    val originalName: String,
    val extension: String,
    val proposedName: String,
    val conflict: Boolean,
    val excluded: Boolean = false,
    val status: RenameStatus = RenameStatus.PENDING
)

enum class RenameStatus {
    PENDING, SUCCESS, FAILED, SKIPPED
}
