package com.youki.pawnforge

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PawnIncludePathsTest {
    private val temporaryFolder = TemporaryFolder()

    @Test
    fun projectRootAndGamemodeAreCompilerIncludePaths() {
        val root = createProject()
        val sourceDirectory = File(root, "gamemodes").apply { mkdirs() }
        File(root, "pawno/include").mkdirs()
        File(root, "gamemode").mkdirs()

        val arguments = PawnIncludePaths.compilerArguments(root, sourceDirectory)

        assertTrue(arguments.contains("-i" + File(root, "gamemode").absolutePath))
        assertTrue(arguments.contains("-i" + root.absolutePath))
        assertTrue(arguments.contains("-i" + File(root, "pawno/include").absolutePath))
        assertTrue(arguments.contains("-i" + sourceDirectory.absolutePath))
    }

    @Test
    fun customPathsCannotEscapeWorkspace() {
        val sanitized = PawnIncludePaths.sanitizeRelativePaths(
            listOf("gamemode", "custom/inc", "../private", "/sdcard", "C:/Windows", "")
        )

        assertEquals(listOf("gamemode", "custom/inc"), sanitized)
    }

    private fun createProject(): File = File(
        System.getProperty("java.io.tmpdir"),
        "pawnforge-test-${System.nanoTime()}"
    ).apply { mkdirs() }
}