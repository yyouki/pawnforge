package com.youki.pawnforge

internal data class PawnReplaceAllResult(
    val text: String,
    val replacements: Int
)

/** Case-aware, literal-text find and replace helpers for Pawn source files. */
internal object PawnFindReplace {

    fun findNext(
        source: String,
        query: String,
        fromIndex: Int,
        forward: Boolean,
        matchCase: Boolean
    ): Int? {
        if (source.isEmpty() || query.isEmpty()) return null
        val ignoreCase = !matchCase
        val cursor = fromIndex.coerceIn(0, source.length)

        return if (forward) {
            source.indexOf(query, cursor, ignoreCase)
                .takeIf { it >= 0 }
                ?: source.indexOf(query, 0, ignoreCase).takeIf { it >= 0 && it < cursor }
        } else {
            source.lastIndexOf(query, cursor - 1, ignoreCase)
                .takeIf { it >= 0 }
                ?: source.lastIndexOf(query, source.length - 1, ignoreCase)
                    .takeIf { it >= cursor }
        }
    }

    fun replaceAll(
        source: String,
        query: String,
        replacement: String,
        matchCase: Boolean
    ): PawnReplaceAllResult {
        if (source.isEmpty() || query.isEmpty()) return PawnReplaceAllResult(source, 0)

        val output = StringBuilder(source.length)
        val ignoreCase = !matchCase
        var sourceCursor = 0
        var searchCursor = 0
        var replacements = 0

        while (searchCursor <= source.length - query.length) {
            val match = source.indexOf(query, searchCursor, ignoreCase)
            if (match < 0) break

            output.append(source.substring(sourceCursor, match))
            output.append(replacement)
            sourceCursor = match + query.length
            searchCursor = sourceCursor
            replacements++
        }

        if (replacements == 0) return PawnReplaceAllResult(source, 0)
        output.append(source.substring(sourceCursor))
        return PawnReplaceAllResult(output.toString(), replacements)
    }
}
