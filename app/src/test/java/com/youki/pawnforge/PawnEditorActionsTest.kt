package com.youki.pawnforge

import org.junit.Assert.assertEquals
import org.junit.Test

class PawnEditorActionsTest {

    @Test
    fun duplicateCurrentLineKeepsNewlineAndSelectsCopy() {
        val edit = PawnEditorActions.duplicate("one();\ntwo();", 2, 2)
        assertEquals("one();\none();\ntwo();", edit.text)
        assertEquals("one();", edit.text.substring(edit.selectionStart, edit.selectionEnd))
    }

    @Test
    fun duplicateSelectionCopiesOnlySelectedText() {
        val edit = PawnEditorActions.duplicate("alpha beta", 0, 5)
        assertEquals("alphaalpha beta", edit.text)
        assertEquals("alpha", edit.text.substring(edit.selectionStart, edit.selectionEnd))
    }

    @Test
    fun deleteLastLineAlsoRemovesPreviousNewline() {
        val edit = PawnEditorActions.deleteLines("one();\ntwo();", 8, 8)
        assertEquals("one();", edit.text)
        assertEquals(edit.text.length, edit.selectionStart)
    }

    @Test
    fun toggleCommentAddsAndRemovesPawnLineComments() {
        val first = PawnEditorActions.toggleComments("    return 1;", 4, 4)
        assertEquals("    // return 1;", first.text)

        val second = PawnEditorActions.toggleComments(first.text, 4, 4)
        assertEquals("    return 1;", second.text)
    }

    @Test
    fun toggleCommentWorksAcrossSelectedLines() {
        val source = "new a;\nnew b;\n"
        val commented = PawnEditorActions.toggleComments(source, 0, source.length)
        assertEquals("// new a;\n// new b;\n", commented.text)
        val uncommented = PawnEditorActions.toggleComments(commented.text, 0, commented.text.length)
        assertEquals(source, uncommented.text)
    }

    @Test
    fun goToLineClampsToAvailableSource() {
        val source = "one();\ntwo();\nthree();"
        assertEquals(7, PawnEditorActions.offsetForLine(source, 2))
        assertEquals(source.length, PawnEditorActions.offsetForLine(source, 99))
    }
}
