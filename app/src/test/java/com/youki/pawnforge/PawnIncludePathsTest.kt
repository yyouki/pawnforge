package com.youki.pawnforge

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PawnIncludePathsTest {
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


    @Test
    fun compilerCommandIncludesProjectPathsAndOutput() {
        val root = createProject()
        val sourceDir = File(root, "gamemodes").apply { mkdirs() }
        File(root, "gamemode").mkdirs()
        File(root, "include").mkdirs()
        val source = File(sourceDir, "main.pwn").apply { writeText("main() {}") }
        val output = File(root, "output/main.amx")

        val arguments = PawnCompilerCommand.buildArguments(
            PawnCompileRequest(
                sourceFile = source,
                workspaceRoot = root,
                outputFile = output,
                customIncludePaths = listOf("gamemode", "include")
            )
        )

        assertTrue(arguments.contains("-i" + File(root, "gamemode").absolutePath))
        assertTrue(arguments.contains("-i" + File(root, "include").absolutePath))
        assertTrue(arguments.contains("-Z"))
        assertTrue(arguments.contains("-O2"))
        assertTrue(arguments.contains("-o" + output.absolutePath))
        assertEquals(source.absolutePath, arguments.last())
        assertTrue(output.parentFile?.isDirectory == true)
    }

    private fun createProject(): File = File(
        System.getProperty("java.io.tmpdir"),
        "pawnforge-test-${System.nanoTime()}"
    ).apply { mkdirs() }
}