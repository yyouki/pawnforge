package com.youki.pawnforge

import java.io.File

data class PawnCompileRequest(
    val sourceFile: File,
    val workspaceRoot: File,
    val outputFile: File,
    val customIncludePaths: List<String> = emptyList(),
    val extraIncludeDirectories: List<File> = emptyList(),
    val compilerFlags: List<String> = listOf("-Z", "-O2")
)

/** Produces an argv array for pawncc without shell quoting or a spawned process. */
object PawnCompilerCommand {
    fun buildArguments(request: PawnCompileRequest): List<String> {
        require(request.sourceFile.isFile) {
            "Pawn source file does not exist: ${request.sourceFile}"
        }
        require(request.workspaceRoot.isDirectory) {
            "Project workspace does not exist: ${request.workspaceRoot}"
        }

        val projectIncludeArguments = PawnIncludePaths.compilerArguments(
            workspaceRoot = request.workspaceRoot,
            sourceDirectory = request.sourceFile.parentFile,
            customRelativePaths = request.customIncludePaths
        )
        val extraIncludeArguments = request.extraIncludeDirectories
            .filter { it.isDirectory }
            .map { "-i${it.canonicalPath}" }

        request.outputFile.parentFile?.let { parent ->
            if (!parent.exists() && !parent.mkdirs()) {
                throw IllegalStateException("Could not create output directory: $parent")
            }
        }

        return buildList {
            addAll(projectIncludeArguments)
            addAll(extraIncludeArguments)
            addAll(request.compilerFlags)
            add("-o${request.outputFile.absolutePath}")
            add(request.sourceFile.absolutePath)
        }
    }
}
