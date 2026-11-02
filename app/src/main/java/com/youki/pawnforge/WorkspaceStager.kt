package com.youki.pawnforge

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.io.FileOutputStream

/** Copies a selected SAF folder to app-private storage while preserving its directory tree. */
class WorkspaceStager(
    private val contentResolver: ContentResolver,
    private val appFilesDirectory: File
) {
    fun stage(treeUri: Uri): File {
        val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId)
        val rootName = queryDisplayName(rootDocumentUri) ?: "PawnForgeProject"
        val destination = File(File(appFilesDirectory, "workspaces"), safeName(rootName))

        if (destination.exists() && !destination.deleteRecursively()) {
            throw IllegalStateException("Could not clean previous staged workspace.")
        }
        if (!destination.mkdirs() && !destination.isDirectory) {
            throw IllegalStateException("Could not create staging directory.")
        }

        copyDirectory(treeUri, treeDocumentId, destination)
        return destination
    }

    private fun copyDirectory(treeUri: Uri, directoryId: String, destination: File) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, directoryId)
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        contentResolver.query(childrenUri, columns, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (cursor.moveToNext()) {
                val documentId = cursor.getString(idColumn)
                val name = safeName(cursor.getString(nameColumn) ?: continue)
                val mimeType = cursor.getString(mimeColumn)
                val target = File(destination, name)

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    if (!target.mkdirs() && !target.isDirectory) {
                        throw IllegalStateException("Could not create folder: $name")
                    }
                    copyDirectory(treeUri, documentId, target)
                } else {
                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                    copyFile(documentUri, target)
                }
            }
        } ?: throw IllegalStateException("Could not read the selected project folder.")
    }

    private fun copyFile(sourceUri: Uri, destination: File) {
        contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Could not read a project file: ${destination.name}")
    }

    private fun queryDisplayName(uri: Uri): String? {
        val column = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        return contentResolver.query(uri, column, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
            } else null
        }
    }

    private fun safeName(value: String): String {
        val name = buildString {
            value.forEach { character ->
                if (character == '/' || character == '\\\\' || character < ' ' || character == '\\u007f') {
                    append('_')
                } else {
                    append(character)
                }
            }
        }.trim()
        return name.takeIf { it.isNotEmpty() && it != "." && it != ".." } ?: "unnamed"
    }
}