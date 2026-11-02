package com.youki.pawnforge

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File

/** Writes compiled outputs and the active edited source back to the SAF-selected project folder. */
class WorkspaceExporter(
    private val contentResolver: ContentResolver
) {
    fun exportFiles(treeUri: Uri, workspaceRoot: File, files: List<File>) {
        val root = workspaceRoot.canonicalFile
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocumentId)

        files.forEach { file ->
            val canonicalFile = file.canonicalFile
            require(canonicalFile.isFile) { "Export file does not exist: $canonicalFile" }
            require(canonicalFile.path.startsWith(root.path + File.separator)) {
                "Export file is outside the selected workspace."
            }
            exportOne(treeUri, rootUri, root, canonicalFile)
        }
    }

    private fun exportOne(treeUri: Uri, rootUri: Uri, root: File, sourceFile: File) {
        val relativePath = root.toPath().relativize(sourceFile.toPath()).toString()
            .replace(File.separatorChar, '/')
        val segments = relativePath.split('/').filter { it.isNotBlank() }
        require(segments.isNotEmpty()) { "Invalid project-relative export path." }

        var parentUri = rootUri
        segments.dropLast(1).forEach { directoryName ->
            val existing = findChild(treeUri, parentUri, directoryName)
            parentUri = if (existing != null) {
                require(existing.mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    "A file blocks the required export folder: $directoryName"
                }
                existing.uri
            } else {
                DocumentsContract.createDocument(
                    contentResolver,
                    parentUri,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    directoryName
                ) ?: throw IllegalStateException("Could not create export folder: $directoryName")
            }
        }

        val fileName = segments.last()
        findChild(treeUri, parentUri, fileName)?.let { existing ->
            if (!DocumentsContract.deleteDocument(contentResolver, existing.uri)) {
                throw IllegalStateException("Could not replace existing project file: $fileName")
            }
        }

        val targetUri = DocumentsContract.createDocument(
            contentResolver,
            parentUri,
            "application/octet-stream",
            fileName
        ) ?: throw IllegalStateException("Could not create exported file: $fileName")

        sourceFile.inputStream().use { input ->
            contentResolver.openOutputStream(targetUri, "wt")?.use { output ->
                input.copyTo(output)
            } ?: throw IllegalStateException("Could not write exported file: $fileName")
        }
    }

    private fun findChild(treeUri: Uri, parentUri: Uri, displayName: String): ChildDocument? {
        val parentId = DocumentsContract.getDocumentId(parentUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == displayName) {
                    val documentId = cursor.getString(idIndex)
                    return ChildDocument(
                        uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId),
                        mimeType = cursor.getString(mimeIndex)
                    )
                }
            }
        }
        return null
    }

    private data class ChildDocument(
        val uri: Uri,
        val mimeType: String
    )
}
