package com.example.feature.rulebuilder.domain.model

sealed class RenameToken {
    data class ExifDate(val format: String = "yyyy-MM-dd") : RenameToken()
    data class FileModifiedDate(val format: String = "yyyy-MM-dd") : RenameToken()
    data class ParentFolderName(val levels: Int = 1) : RenameToken()
    data class IncrementingCounter(val padding: Int = 3, val start: Int = 1) : RenameToken()
    data class OriginalName(val rangeStart: Int? = null, val rangeEnd: Int? = null) : RenameToken()
    data class CustomText(val value: String) : RenameToken()
    data class RegexCapture(val pattern: String, val group: Int = 1) : RenameToken()
    data class FileExtension(val lowercase: Boolean = true) : RenameToken()

    companion object {
        fun serializeList(tokens: List<RenameToken>): String {
            return tokens.joinToString(";;") { token ->
                when (token) {
                    is ExifDate -> "exif_date|${token.format}"
                    is FileModifiedDate -> "modified_date|${token.format}"
                    is ParentFolderName -> "parent_folder|${token.levels}"
                    is IncrementingCounter -> "counter|${token.padding}|${token.start}"
                    is OriginalName -> "original_name|${token.rangeStart ?: ""}|${token.rangeEnd ?: ""}"
                    is CustomText -> "custom_text|${token.value}"
                    is RegexCapture -> "regex|${token.pattern}|${token.group}"
                    is FileExtension -> "extension|${token.lowercase}"
                }
            }
        }

        fun deserializeList(serialized: String): List<RenameToken> {
            if (serialized.isEmpty()) return emptyList()
            return serialized.split(";;").mapNotNull { part ->
                val segments = part.split("|")
                if (segments.isEmpty()) return@mapNotNull null
                val type = segments[0]
                try {
                    when (type) {
                        "exif_date" -> ExifDate(segments.getOrElse(1) { "yyyy-MM-dd" })
                        "modified_date" -> FileModifiedDate(segments.getOrElse(1) { "yyyy-MM-dd" })
                        "parent_folder" -> ParentFolderName(segments.getOrElse(1) { "1" }.toIntOrNull() ?: 1)
                        "counter" -> {
                            val padding = segments.getOrElse(1) { "3" }.toIntOrNull() ?: 3
                            val start = segments.getOrElse(2) { "1" }.toIntOrNull() ?: 1
                            IncrementingCounter(padding, start)
                        }
                        "original_name" -> {
                            val startStr = segments.getOrNull(1)
                            val endStr = segments.getOrNull(2)
                            val start = if (startStr.isNullOrEmpty()) null else startStr.toIntOrNull()
                            val end = if (endStr.isNullOrEmpty()) null else endStr.toIntOrNull()
                            OriginalName(start, end)
                        }
                        "custom_text" -> CustomText(segments.getOrElse(1) { "" })
                        "regex" -> {
                            val pattern = segments.getOrElse(1) { "" }
                            val group = segments.getOrElse(2) { "1" }.toIntOrNull() ?: 1
                            RegexCapture(pattern, group)
                        }
                        "extension" -> FileExtension(segments.getOrElse(1) { "true" }.toBoolean())
                        else -> null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}
