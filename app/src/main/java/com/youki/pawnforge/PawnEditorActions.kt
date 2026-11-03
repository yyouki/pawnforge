package com.youki.pawnforge

internal data class PawnTextEdit(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int
)

/**
 * Pure Pawn source editing operations. Kept separate from Android views so the
 * text transformations can be verified with ordinary JVM unit tests.
 */
internal object PawnEditorActions {

    fun duplicate(source: String, selectionStart: Int, selectionEnd: Int): PawnTextEdit {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, source.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(start, source.length)

        if (start != end) {
            val selected = source.substring(start, end)
            val result = source.substring(0, end) + selected + source.substring(end)
            return PawnTextEdit(result, end, end + selected.length)
        }

        if (source.isEmpty()) return PawnTextEdit(source, 0, 0)

        val lineStart = lineStart(source, start)
        val lineEnd = lineEndIncludingNewline(source, start)
        val line = source.substring(lineStart, lineEnd)
        val insertion = if (line.endsWith("\n")) line else "\n" + line
        val duplicateStart = lineEnd + if (line.endsWith("\n")) 0 else 1
        val result = source.substring(0, lineEnd) + insertion + source.substring(lineEnd)
        return PawnTextEdit(result, duplicateStart, duplicateStart + line.length)
    }

    fun deleteLines(source: String, selectionStart: Int, selectionEnd: Int): PawnTextEdit {
        if (source.isEmpty()) return PawnTextEdit(source, 0, 0)

        val start = minOf(selectionStart, selectionEnd).coerceIn(0, source.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(start, source.length)
        val lastSelected = if (end > start) end - 1 else start
        val blockStart = lineStart(source, start)
        val blockEnd = lineEndIncludingNewline(source, lastSelected)

        // When deleting the final line, also remove the preceding newline.
        val removeStart = if (blockEnd == source.length && blockStart > 0) blockStart - 1 else blockStart
        val result = source.removeRange(removeStart, blockEnd)
        val cursor = removeStart.coerceIn(0, result.length)
        return PawnTextEdit(result, cursor, cursor)
    }

    fun toggleComments(source: String, selectionStart: Int, selectionEnd: Int): PawnTextEdit {
        if (source.isEmpty()) return PawnTextEdit(source, 0, 0)

        val start = minOf(selectionStart, selectionEnd).coerceIn(0, source.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(start, source.length)
        val lastSelected = if (end > start) end - 1 else start
        val blockStart = lineStart(source, start)
        val blockEnd = lineEndWithoutNewline(source, lastSelected)
        val block = source.substring(blockStart, blockEnd)
        val lines = block.split("\n")
        val contentLines = lines.filter { it.isNotBlank() }
        val shouldUncomment = contentLines.isNotEmpty() && contentLines.all { line ->
            val indentation = line.takeWhile { it == ' ' || it == '\t' }
            line.substring(indentation.length).startsWith("//")
        }

        val transformed = lines.joinToString("\n") { line ->
            if (line.isBlank()) {
                line
            } else {
                val indentation = line.takeWhile { it == ' ' || it == '\t' }
                val content = line.substring(indentation.length)
                if (shouldUncomment) {
                    val uncommented = content.removePrefix("//").removePrefix(" ")
                    indentation + uncommented
                } else {
                    indentation + "// " + content
                }
            }
        }

        val result = source.substring(0, blockStart) + transformed + source.substring(blockEnd)
        return PawnTextEdit(result, blockStart, blockStart + transformed.length)
    }

    fun lineStart(source: String, position: Int): Int {
        val cursor = position.coerceIn(0, source.length)
        val previousNewline = source.lastIndexOf('\n', cursor - 1)
        return if (previousNewline < 0) 0 else previousNewline + 1
    }

    fun lineEndIncludingNewline(source: String, position: Int): Int {
        val cursor = position.coerceIn(0, source.length)
        val nextNewline = source.indexOf('\n', cursor)
        return if (nextNewline < 0) source.length else nextNewline + 1
    }

    private fun lineEndWithoutNewline(source: String, position: Int): Int {
        val cursor = position.coerceIn(0, source.length)
        val nextNewline = source.indexOf('\n', cursor)
        return if (nextNewline < 0) source.length else nextNewline
    }

    fun offsetForLine(source: String, requestedLine: Int): Int {
        val targetLine = requestedLine.coerceAtLeast(1)
        if (targetLine == 1) return 0

        var line = 1
        for (index in source.indices) {
            if (source[index] == '\n') {
                line++
                if (line == targetLine) return index + 1
            }
        }
        return source.length
    }
}
