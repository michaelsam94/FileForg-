package com.example.core.exif

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.core.domain.ExifReader
import java.text.SimpleDateFormat
import java.util.Locale

class AndroidExifReader(private val context: Context) : ExifReader {
    override fun getExifCaptureDate(uriString: String): Long? {
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val dateTimeStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                if (dateTimeStr != null) {
                    val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                    sdf.parse(dateTimeStr)?.time
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
