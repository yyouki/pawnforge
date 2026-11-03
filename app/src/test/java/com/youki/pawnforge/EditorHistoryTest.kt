package com.youki.pawnforge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorHistoryTest {

    @Test
    fun undoAndRedoRestoreTextAndCursor() {
        val history = EditorHistory()
        val before = EditorSnapshot("print(1);", 4, 4)
        val after = EditorSnapshot("print(2);", 8, 8)

        history.record(before, after)

        val undone = history.undo(after)
        assertEquals(before, undone)
        assertTrue(history.canRedo)

        val redone = history.redo(before)
        assertEquals(after, redone)
        assertTrue(history.canUndo)
    }

    @Test
    fun newEditClearsRedoHistory() {
        val history = EditorHistory()
        val first = EditorSnapshot("one", 3, 3)
        val second = EditorSnapshot("two", 3, 3)
        val third = EditorSnapshot("three", 5, 5)

        history.record(first, second)
        assertNotNull(history.undo(second))
        history.record(first, third)

        assertFalse(history.canRedo)
    }

    @Test
    fun unchangedTextIsNotRecorded() {
        val history = EditorHistory()
        val same = EditorSnapshot("same", 2, 2)

        history.record(same, same)

        assertFalse(history.canUndo)
        assertNull(history.undo(same))
    }

    @Test
    fun historyIsBounded() {
        val history = EditorHistory(maxDepth = 2)
        history.record(EditorSnapshot("0", 0, 0), EditorSnapshot("1", 1, 1))
        history.record(EditorSnapshot("1", 1, 1), EditorSnapshot("2", 1, 1))
        history.record(EditorSnapshot("2", 1, 1), EditorSnapshot("3", 1, 1))

        assertEquals(EditorSnapshot("2", 1, 1), history.undo(EditorSnapshot("3", 1, 1)))
        assertEquals(EditorSnapshot("1", 1, 1), history.undo(EditorSnapshot("2", 1, 1)))
        assertNull(history.undo(EditorSnapshot("1", 1, 1)))
    }
}
