package com.youki.pawnforge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PawnFindReplaceTest {

    @Test
    fun findNextWrapsAroundInBothDirections() {
        val source = "one two one"
        assertEquals(8, PawnFindReplace.findNext(source, "one", 3, true, false))
        assertEquals(8, PawnFindReplace.findNext(source, "one", 0, false, false))
    }

    @Test
    fun findNextCanMatchCaseSensitively() {
        val source = "Player player PLAYER"
        assertEquals(7, PawnFindReplace.findNext(source, "player", 0, true, true))
        assertEquals(0, PawnFindReplace.findNext(source, "PLAYER", 0, true, false))
        assertNull(PawnFindReplace.findNext(source, "missing", 0, true, false))
    }

    @Test
    fun replaceAllUsesLiteralTextAndCountsOccurrences() {
        val result = PawnFindReplace.replaceAll(
            source = "SendClientMessage(playerid); sendclientmessage(playerid);",
            query = "SendClientMessage",
            replacement = "SendPlayerMessage",
            matchCase = false
        )
        assertEquals(2, result.replacements)
        assertEquals(
            "SendPlayerMessage(playerid); SendPlayerMessage(playerid);",
            result.text
        )
    }

    @Test
    fun replaceAllHonoursCaseSensitiveMode() {
        val result = PawnFindReplace.replaceAll(
            source = "pawn Pawn PAWN",
            query = "Pawn",
            replacement = "Code",
            matchCase = true
        )
        assertEquals(1, result.replacements)
        assertEquals("pawn Code PAWN", result.text)
    }

    @Test
    fun emptyQueryDoesNotChangeSource() {
        val source = "keep this source"
        val result = PawnFindReplace.replaceAll(source, "", "x", false)
        assertEquals(source, result.text)
        assertEquals(0, result.replacements)
    }
}
