package com.youki.pawnforge

import java.io.File

/**
 * Builds the include search paths passed to pawncc.
 * Pawn compiler accepts repeated -i<path> arguments for include directories.
 */
object PawnIncludePaths {
    val defaultRelativeDirectories = listOf(
        "pawno/include",
        "qawno/include",
        "include",
        "includes",
        "gamemode",
        "."
    )

    fun sanitizeRelativePaths(paths: List<String>): List<String> = paths
        .map { it.trim().replace(File.separatorChar, '/') }
        .filter { path ->
            path.isNotBlank() &&
                !path.startsWith("/") &&
                !Regex("^[A-Za-z]:").containsMatchIn(path) &&
                path.split('/').none { it == ".." }
        }
        .distinct()

    fun existingDirectories(
        workspaceRoot: File,
        sourceDirectory: File? = null,
        customRelativePaths: List<String> = emptyList()
    ): List<File> {
        val root = workspaceRoot.canonicalFile
        val candidates = buildList {
            if (sourceDirectory != null) add(sourceDirectory)
            defaultRelativeDirectories.forEach { relative ->
                add(if (relative == ".") root else File(root, relative))
            }
            sanitizeRelativePaths(customRelativePaths).forEach { relative ->
                add(File(root, relative))
            }
        }

        return candidates.mapNotNull { candidate ->
            runCatching { candidate.canonicalFile }.getOrNull()?.takeIf { directory ->
                val path = directory.path
                directory.isDirectory && (path == root.path || path.startsWith(root.path + File.separator))
            }
        }.distinctBy { it.path }
    }

    /** Returns arguments in pawncc's documented -i<directory> format. */
    fun compilerArguments(
        workspaceRoot: File,
        sourceDirectory: File? = null,
        customRelativePaths: List<String> = emptyList()
    ): List<String> = existingDirectories(
        workspaceRoot = workspaceRoot,
        sourceDirectory = sourceDirectory,
        customRelativePaths = customRelativePaths
    ).map { directory -> "-i" + directory.absolutePath }
}