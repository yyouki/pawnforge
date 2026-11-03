package com.youki.pawnforge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PawnNativeCatalogTest {

    @Test
    fun parsesNativeNameAndSignature() {
        val entries = PawnNativeCatalog.parseLines(
            sequenceOf("native SendClientMessage(playerid, color, const message[]);"),
            "a_samp.inc"
        )

        assertEquals(1, entries.size)
        assertEquals("SendClientMessage", entries.single().name)
        assertEquals("SendClientMessage(playerid, color, const message[]);", entries.single().signature)
        assertEquals("a_samp.inc", entries.single().sourcePath)
    }

    @Test
    fun parsesTaggedNativeReturnTypes() {
        val entries = PawnNativeCatalog.parseLines(
            sequenceOf("native Float:GetPlayerHealth(playerid);"),
            "open.mp.inc"
        )

        assertEquals("GetPlayerHealth", entries.single().name)
        assertTrue(entries.single().signature.startsWith("Float:"))
    }

    @Test
    fun ignoresLineAndBlockComments() {
        val entries = PawnNativeCatalog.parseLines(
            sequenceOf(
                "// native HiddenByLineComment();",
                "/* native HiddenByBlockStart();",
                "native HiddenInsideBlock();",
                "*/",
                "native VisibleNative(playerid);"
            ),
            "example.inc"
        )

        assertEquals(listOf("VisibleNative"), entries.map { it.name })
    }

    @Test
    fun ignoresMalformedOrNonNativeLines() {
        val entries = PawnNativeCatalog.parseLines(
            sequenceOf(
                "stock Helper(value);",
                "native MissingSemicolon(value)",
                "native NoSignature;",
                "native GoodNative(value); // trailing comment"
            ),
            "example.inc"
        )

        assertEquals(listOf("GoodNative"), entries.map { it.name })
    }
}
