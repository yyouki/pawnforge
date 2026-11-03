package com.youki.pawnforge

/**
 * Text and cursor state stored by the editor's bounded undo/redo history.
 */
internal data class EditorSnapshot(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int
)

/**
 * Small, bounded history suitable for mobile editing sessions.
 */
internal class EditorHistory(private val maxDepth: Int = 80) {
    private val undoEntries = ArrayDeque<EditorSnapshot>()
    private val redoEntries = ArrayDeque<EditorSnapshot>()

    val canUndo: Boolean
        get() = undoEntries.isNotEmpty()

    val canRedo: Boolean
        get() = redoEntries.isNotEmpty()

    fun record(before: EditorSnapshot, after: EditorSnapshot) {
        if (before.text == after.text) return

        undoEntries.addLast(before)
        while (undoEntries.size > maxDepth) {
            undoEntries.removeFirst()
        }
        redoEntries.clear()
    }

    fun undo(current: EditorSnapshot): EditorSnapshot? {
        if (undoEntries.isEmpty()) return null
        redoEntries.addLast(current)
        return undoEntries.removeLast()
    }

    fun redo(current: EditorSnapshot): EditorSnapshot? {
        if (redoEntries.isEmpty()) return null
        undoEntries.addLast(current)
        while (undoEntries.size > maxDepth) {
            undoEntries.removeFirst()
        }
        return redoEntries.removeLast()
    }

    fun clear() {
        undoEntries.clear()
        redoEntries.clear()
    }
}
