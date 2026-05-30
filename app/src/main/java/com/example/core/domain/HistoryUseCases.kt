package com.example.core.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportHistoryCsvUseCase {
    fun execute(batches: List<HistoryBatch>): String {
        val sb = java.lang.StringBuilder()
        // Header
        sb.append("Batch ID,Timestamp,Rules Used,Original Name,New Name,Original URI,Undo Applied\n")
        
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        for (batch in batches) {
            val dateStr = sdf.format(Date(batch.timestamp))
            val rulesEscaped = escapeCsvValue(batch.rulesUsed)
            for (item in batch.items) {
                sb.append("${batch.batchId},")
                sb.append("$dateStr,")
                sb.append("$rulesEscaped,")
                sb.append("${escapeCsvValue(item.originalName)},")
                sb.append("${escapeCsvValue(item.newName)},")
                sb.append("${escapeCsvValue(item.originalUri)},")
                sb.append("${batch.undoApplied}\n")
            }
        }
        return sb.toString()
    }

    private fun escapeCsvValue(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            val replaced = value.replace("\"", "\"\"")
            return "\"$replaced\""
        }
        return value
    }
}
