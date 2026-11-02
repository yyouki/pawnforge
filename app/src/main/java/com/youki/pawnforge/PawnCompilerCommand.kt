package com.youki.pawnforge

import java.io.File

data class PawnCompileRequest(
    val sourceFile: File,
    val workspaceRoot: File,
    val outputFile: File,
    val customIncludePaths: List<String> = emptyList(),
    val compilerFlags: List<String> = emptyList()
)

/** Produces the argument vector for pawncc; execution is supplied by the native engine. */
object PawnCompilerCommand {
    fun buildArguments(request: PawnCompileRequest): List<String> {
        require(request.sourceFile.isFile) { "Pawn source file does not exist: ${request.sourceFile}" }
        require(request.workspaceRoot.isDirectory) { "Project workspace does not exist: ${request.workspaceRoot}" }

        val includeArguments = PawnIncludePaths.compilerArguments(
            workspaceRoot = request.workspaceRoot,
            sourceDirectory = request.sourceFile.parentFile,
            customRelativePaths = request.customIncludePaths
        )

        request.outputFile.parentFile?.let { parent ->
            if (!parent.exists() && !parent.mkdirs()) {
                throw IllegalStateException("Could not create output directory: $parent")
            }
        }

        return buildList {
            addAll(includeArguments)
            addAll(request.compilerFlags)
            add("-o" + request.outputFile.absolutePath)
            add(request.sourceFile.absolutePath)
        }
    }
}