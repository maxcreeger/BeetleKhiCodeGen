package org.beetlekhi.arduino.cli.dataclasses

import kotlinx.serialization.Serializable


@Serializable
data class ArduinoBoardId(
    val name: String = "",
    val fqbn: String = "",
    val vid: String = "",
    val pid: String = ""
)

data class ProcessOutput(
    val returnCode: Int,
    val stdOut: String,
    val errOut: String
)

@Serializable
data class ArduinoVersion(
    val Application: String,
    val VersionString: String,
    val Commit: String,
    val Status: String,
    val Date: String
)

@Serializable
data class ArduinoBoard(
    val address: String,
    val protocol: String,
    val protocol_label: String,
    val boards: List<ArduinoBoardId>? = null,
    val serial_number: String? = null
)

@Serializable
data class ArduinoCore(
    val id: String,
    val installed: String,
    val latest: String,
    val name: String,
    val maintainer: String,
    val website: String,
    val email: String,
    val boards: List<ArduinoBoardId>
)

@Serializable
data class ArduinoCompilation(
    val compiler_out: String,
    val compiler_err: String,
    val success: Boolean,
    val builder_result: ArduinoBuildResult
)

@Serializable
data class ArduinoSectionSize(
    val name: String,
    val size: Int,
    val max_size: Int
)

@Serializable
data class ArduinoBuildResult(
    val build_path: String,
    val executable_sections_size: List<ArduinoSectionSize>
)