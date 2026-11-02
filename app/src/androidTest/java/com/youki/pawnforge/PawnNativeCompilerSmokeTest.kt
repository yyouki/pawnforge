package com.youki.pawnforge

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PawnNativeCompilerSmokeTest {

    @Test
    fun compilesMinimalPawnSourceToAmx() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val workspace = File(context.cacheDir, "pawn-native-smoke").apply {
            deleteRecursively()
            check(mkdirs()) { "Could not create compiler smoke-test workspace." }
        }
        val source = File(workspace, "main.pwn").apply {
            writeText(
                """
                main()
                {
                    return 1;
                }
                """.trimIndent(),
                Charsets.UTF_8
            )
        }
        val output = File(workspace, "output/main.amx")
        val logFile = File(context.cacheDir, "pawn-native-smoke.log")
        val temporaryDirectory = File(context.cacheDir, "pawn-native-smoke-tmp")

        val result = PawnNativeCompiler.compile(
            arguments = listOf(
                "pawncc",
                "-Z",
                "-O2",
                "-o${output.absolutePath}",
                source.absolutePath
            ),
            logFile = logFile,
            temporaryDirectory = temporaryDirectory,
            workingDirectory = workspace,
            outputFile = output
        )

        assertEquals("Pawn compiler log:
${result.output}", 0, result.exitCode)
        assertTrue("Compiler did not create the AMX output file.", output.isFile)
        assertTrue("Compiler generated an empty AMX file.", output.length() > 0)
    }
}
