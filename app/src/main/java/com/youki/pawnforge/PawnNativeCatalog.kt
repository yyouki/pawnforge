package com.youki.pawnforge

import java.io.File

internal data class PawnNativeEntry(
    val name: String,
    val signature: String,
    val sourcePath: String
)

/**
 * Reads one-line native declarations from local Pawn include files.
 * It deliberately does not download libraries or interpret commented-out code.
 */
internal object PawnNativeCatalog {
    private val functionPattern = Regex("(?:[A-Za-z_][A-Za-z0-9_]*:)?([A-Za-z_][A-Za-z0-9_]*)\\s*\\(")

    fun parseLines(lines: Sequence<String>, sourcePath: String): List<PawnNativeEntry> {
        val results = mutableListOf<PawnNativeEntry>()
        var insideBlockComment = false

        for (line in lines) {
            val trimmed = line.trimStart()

            if (insideBlockComment) {
                if (trimmed.contains("*/")) insideBlockComment = false
                continue
            }

            if (trimmed.startsWith("/*")) {
                if (!trimmed.contains("*/")) insideBlockComment = true
                continue
            }
            if (trimmed.startsWith("//") || !trimmed.startsWith("native ")) continue

            val declaration = trimmed.substringBefore("//").trim()
            if (!declaration.endsWith(";")) continue

            val signature = declaration.removePrefix("native").trim()
            val match = functionPattern.find(signature) ?: continue
            val name = match.groupValues[1]

            results.add(PawnNativeEntry(name, signature, sourcePath))
        }

        return results
    }

    fun scan(includeRoots: List<File>, maxFiles: Int = 500, maxFileBytes: Long = 2_000_000): List<PawnNativeEntry> {
        val entries = mutableListOf<PawnNativeEntry>()
        var visitedFiles = 0
        val excluded = setOf(".git", "build", ".gradle", "node_modules", ".idea")

        for (root in includeRoots.filter { it.isDirectory }) {
            val files = root.walkTopDown()
                .onEnter { directory -> directory == root || directory.name !in excluded }
                .filter { file ->
                    file.isFile && file.extension.equals("inc", ignoreCase = true) &&
                        file.length() > 0L && file.length() <= maxFileBytes
                }
                .take((maxFiles - visitedFiles).coerceAtLeast(0))

            for (file in files) {
                visitedFiles++
                val source = runCatching {
                    file.bufferedReader(Charsets.UTF_8).use { reader ->
                        parseLines(reader.lineSequence(), file.name)
                    }
                }.getOrDefault(emptyList())
                entries.addAll(source)
            }

            if (visitedFiles >= maxFiles) break
        }

        return entries
            .distinctBy { it.name.lowercase() + "|" + it.signature.lowercase() }
            .sortedWith(compareBy<PawnNativeEntry> { it.name.lowercase() }.thenBy { it.signature.lowercase() })
    }
}
