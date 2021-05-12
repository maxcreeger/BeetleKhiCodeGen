package org.beetlekhi.arduino.cli

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.beetlekhi.arduino.cli.dataclasses.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.function.Function
import java.util.regex.Pattern
import java.util.stream.Collectors


class ArduinoCLI(private val exeLocation: File, private val exeName: String, var verbose: Boolean = false) {

    companion object {
        private val versionRegex = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)")
        private const val CLI_VERSION_MAJOR = 0
        private const val CLI_VERSION_MINOR = 18
        private const val CLI_VERSION_BUGFIX = 1
        private fun checkVersion(processOutput: ArduinoVersion): Boolean {
            val matcher = versionRegex.matcher(processOutput.VersionString)
            if (!matcher.matches()) {
                throw UnsupportedOperationException("Unable to obtain Arduino CLI Version from `${processOutput.VersionString}`")
            }
            val major = matcher.group(1).toInt()
            val minor = matcher.group(2).toInt()
            val bugfix = matcher.group(3).toInt()
            if (major < CLI_VERSION_MAJOR ||
                major == CLI_VERSION_MAJOR && (
                        minor < CLI_VERSION_MINOR ||
                                minor == CLI_VERSION_MINOR
                                && bugfix < CLI_VERSION_BUGFIX
                        )
            ) {
                return false
            }
            return true
        }
    }

    init {
        val processOutput = getVersion()
        if(!checkVersion(processOutput))
        throw UnsupportedOperationException("Incompatible Arduino CLI version. Version $CLI_VERSION_MAJOR.$CLI_VERSION_MINOR.$CLI_VERSION_BUGFIX or later is required. Current: ${processOutput.VersionString}")
    }

    fun getVersion(): ArduinoVersion =
        executeAndParseOrThrow("version")
        { "Could not test arduino-cli file in folder $exeLocation with name $exeName. Reason: $it" }


    fun coreUpdateIndex() =
        executeOrThrow("core", "update-index")
        { "Could not update core index: $it" }

    fun boardList(): List<ArduinoBoard> =
        executeAndParseOrThrow("board", "list")
        { "Could not list boards: $it" }

    fun install(platformCore: String) =
        executeOrThrow("core", "install", platformCore)

    fun coreList(): List<ArduinoCore> =
        executeAndParseOrThrow("core", "list")
        { "Could not list Cores: $it" }

    fun compile(sketchFolder: File, board: ArduinoBoardId): ArduinoCompilation =
        executeAndParseOrThrow("compile", "--fqbn", board.fqbn, sketchFolder.absolutePath)
        { "Could not compile: $it" }

    fun upload(sketchFolder: File, board: ArduinoBoard, boardId: ArduinoBoardId) =
        executeOrThrow("upload", sketchFolder.absolutePath, "--fqbn", boardId.fqbn, "--port", board.address)
        { "Could not Upload file $sketchFolder to $board. Error: $it" }

    fun libInstall(libName: String) =
        executeOrThrow("lib", "install", libName)

//---------------------------------- Internal ----------------------------------------------------------------------

    private inline fun <reified T> executeAndParseOrThrow(
        vararg args: String,
        exceptionTitleBuilder: Function<String, String>? = null
    ): T {
        val processOutput = executeOrThrow(*args, exceptionTitleBuilder = exceptionTitleBuilder)
        return Json { ignoreUnknownKeys = true }.decodeFromString(processOutput)
    }

    private fun executeOrThrow(
        vararg args: String,
        exceptionTitleBuilder: Function<String, String>? = null
    ): String {
        val processOutput = execute(*args)
        if (exceptionTitleBuilder != null && processOutput.returnCode != 0) {
            throw UnsupportedOperationException(exceptionTitleBuilder.apply(processOutput.errOut))
        }
        return processOutput.stdOut
    }

    private fun execute(vararg args: String): ProcessOutput {
        val processBuilder =
            ProcessBuilder(
                "${exeLocation.absoluteFile}${File.separatorChar}$exeName",
                *args,
                "--format",
                "json"
            )
        if (verbose)
            processBuilder.command().add("-v")
        val p: Process
        var stdOut = ""
        var errOut = ""
        var returnCode: Int
        try {
            p = processBuilder.start()
            val stdReader = BufferedReader(InputStreamReader(p.inputStream))
            val errReader = BufferedReader(InputStreamReader(p.errorStream))
            val hasCompleted = p.waitFor(10, TimeUnit.SECONDS)
            stdOut = stdReader.lines().collect(Collectors.joining(System.getProperty("line.separator")))
            errOut = errReader.lines().collect(Collectors.joining(System.getProperty("line.separator")))
            p.destroy()
            if (!hasCompleted) {
                return ProcessOutput(-2, stdOut, "Timed Out. stderr: $errOut")
            } else {
                returnCode = p.exitValue()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            returnCode = -1
        }
        return ProcessOutput(returnCode, stdOut, errOut)
    }

}