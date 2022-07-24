package com.utsman.nadia.util

import com.github.ajalt.clikt.core.CliktError
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object Commander {

    fun execString(vararg commands: String): String {
        return ProcessBuilder(*commands.toList().toTypedArray())
            .start()
            .inputStream
            .bufferedReader()
            .readText()
    }

    private fun readProcess(inputStream: InputStream, onContinue: (String) -> Unit) {
        val inputReader = InputStreamReader(inputStream)
        val bufferInputReader = BufferedReader(inputReader)
        val inputLine = bufferInputReader.readLine()
        while (inputLine != null) {
            if (inputLine.contains("Error: ") || inputLine.contains("Exception: ")) {
                throw CliktError(inputLine)
            } else if (inputLine.isNotBlank() || inputLine.isNotEmpty()) {
                onContinue.invoke(inputLine)
                break
            } else {
                continue
            }
        }
    }

    fun exec(vararg commands: String, disableExternalReader: Boolean = false) {
        val process = ProcessBuilder(*commands.toList().toTypedArray())
            .apply {
                if (disableExternalReader) {
                    redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    redirectError(ProcessBuilder.Redirect.INHERIT)
                }
            }
            .start()

        if (!disableExternalReader) {
            readProcess(process.inputStream) {
                println("> $it")
            }

            readProcess(process.errorStream) {
                println("> $it")
            }
        }
        process.waitFor()
    }

    fun execInherit(vararg commands: String) {
        ProcessBuilder(*commands.toList().toTypedArray())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor()
    }
}