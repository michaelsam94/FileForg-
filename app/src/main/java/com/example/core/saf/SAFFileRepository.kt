package com.example.core.saf

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.example.core.domain.ExifReader
import com.example.core.domain.FileRepository
import com.example.feature.rulebuilder.domain.model.ScannedFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SAFFileRepository(
    private val context: Context,
    private val exifReader: ExifReader
) : FileRepository {

    override fun scanFolder(folderUriString: String): Flow<List<ScannedFile>> = flow {
        if (folderUriString.isEmpty()) {
            emit(emptyList())
            return@flow
        }
        val uri = Uri.parse(folderUriString)
        val files = mutableListOf<ScannedFile>()
        val rootDoc = DocumentFile.fromTreeUri(context, uri)
        if (rootDoc != null) {
            traverseFolder(rootDoc, files, emptyList())
        }
        emit(files)
    }

    private fun traverseFolder(
        doc: DocumentFile,
        resultList: MutableList<ScannedFile>,
        parentDirs: List<String>
    ) {
        val list = doc.listFiles()
        for (item in list) {
            if (item.isDirectory) {
                val nextParentDirs = parentDirs + (item.name ?: "")
                traverseFolder(item, resultList, nextParentDirs)
            } else if (item.isFile) {
                val fullDisplayName = item.name ?: ""
                val ext = getExtension(fullDisplayName)
                val originalNameWithoutExt = fullDisplayName.removeSuffix(".$ext")
                
                val exifTime = exifReader.getExifCaptureDate(item.uri.toString())

                resultList.add(
                    ScannedFile(
                        uriString = item.uri.toString(),
                        originalName = originalNameWithoutExt,
                        extension = ext,
                        size = item.length(),
                        dateModified = item.lastModified(),
                        exifDate = exifTime,
                        parentFolders = parentDirs
                    )
                )
            }
        }
    }

    private fun getExtension(filename: String): String {
        return filename.substringAfterLast('.', "").lowercase()
    }

    override suspend fun renameFile(uriString: String, newNameWithExt: String): Boolean {
        return try {
            val fileDoc = DocumentFile.fromSingleUri(context, Uri.parse(uriString))
            if (fileDoc != null && fileDoc.exists()) {
                fileDoc.renameTo(newNameWithExt)
            } else false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun createFolderIfNotExist(parentUriString: String, folderName: String): String? {
        return try {
            val parentDoc = DocumentFile.fromTreeUri(context, Uri.parse(parentUriString))
            if (parentDoc != null && parentDoc.exists()) {
                val existing = parentDoc.findFile(folderName)
                if (existing != null && existing.isDirectory) {
                    existing.uri.toString()
                } else {
                    val newDir = parentDoc.createDirectory(folderName)
                    newDir?.uri?.toString()
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun copyFile(
        sourceUriString: String,
        destFolderUriString: String,
        destNameWithExt: String
    ): String? {
        return try {
            val sourceUri = Uri.parse(sourceUriString)
            val destFolderUri = Uri.parse(destFolderUriString)
            
            val sourceDoc = DocumentFile.fromSingleUri(context, sourceUri) ?: return null
            val destFolderDoc = DocumentFile.fromTreeUri(context, destFolderUri) ?: return null
            
            val ext = getExtension(sourceDoc.name ?: "")
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
            
            val newFileDoc = destFolderDoc.createFile(mimeType, destNameWithExt) ?: return null
            
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                context.contentResolver.openOutputStream(newFileDoc.uri)?.use { output ->
                    input.copyTo(output)
                }
            }
            newFileDoc.uri.toString()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun deleteFile(uriString: String): Boolean {
        return try {
            val fileDoc = DocumentFile.fromSingleUri(context, Uri.parse(uriString))
            if (fileDoc != null && fileDoc.exists()) {
                fileDoc.delete()
            } else false
        } catch (e: Exception) {
            false
        }
    }
}
