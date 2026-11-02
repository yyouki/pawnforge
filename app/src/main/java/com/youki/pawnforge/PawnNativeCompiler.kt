package com.youki.pawnforge

import java.io.File

data class PawnNativeCompileResult(
    val exitCode: Int,
    val output: String,
    val outputFile: File
)

/** JNI bridge to the in-process Pawn Community Compiler. */
object PawnNativeCompiler {
    init {
        System.loadLibrary("pawnforge_jni")
    }

    @JvmStatic
    external fun nativeCompile(
        arguments: Array<String>,
        logFilePath: String,
        temporaryDirectoryPath: String,
        workingDirectoryPath: String
    ): Int

    fun compile(
        arguments: List<String>,
        logFile: File,
        temporaryDirectory: File,
        workingDirectory: File,
        outputFile: File
    ): PawnNativeCompileResult {
        require(arguments.size >= 2) { "Pawn compiler arguments are incomplete." }
        require(workingDirectory.isDirectory) { "Workspace directory does not exist." }
        require(temporaryDirectory.exists() || temporaryDirectory.mkdirs()) {
            "Could not create compiler temporary directory."
        }
        require(logFile.parentFile?.exists() == true || logFile.parentFile?.mkdirs() == true) {
            "Could not create compiler log directory."
        }

        val exitCode = nativeCompile(
            arguments.toTypedArray(),
            logFile.absolutePath,
            temporaryDirectory.absolutePath,
            workingDirectory.absolutePath
        )
        val output = if (logFile.isFile) logFile.readText(Charsets.UTF_8) else ""
        return PawnNativeCompileResult(exitCode, output, outputFile)
    }
}
