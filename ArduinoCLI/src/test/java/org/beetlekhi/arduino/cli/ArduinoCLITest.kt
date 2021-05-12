package org.beetlekhi.arduino.cli

import io.mockk.*
import org.beetlekhi.arduino.cli.dataclasses.ArduinoBoard
import org.beetlekhi.arduino.cli.dataclasses.ArduinoBoardId
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets

fun madin() {
    val cliLocation = File("target/test-classes/")
    val cli = ArduinoCLI(cliLocation, "arduino-cli.exe")
    println("CLI started")
    val sketchFolder = File("target/test-classes/empty-sketch")
    if (!sketchFolder.exists()) {
        fail()
    }
    val update = cli.coreUpdateIndex()
    println("Update: `$update`")
    val boardList = cli.boardList()
    println("Boards: boardList")
    val uno = boardList.first { board ->
        board.boards != null && board.boards!!.any { id -> id.fqbn == "arduino:avr:uno" }
    }
    val unoId = uno.boards!!.first()
    val install = cli.install("arduino:avr")
    println("Install: $install")
    val cores = cli.coreList()
    println("All Cores: $cores")
    // val unoCore = cores.find{it.boards.any { id -> id.fqbn == "arduino:avr:uno" }}
    // println("Uno Core: $unoCore")
    val compile = cli.compile(sketchFolder, unoId)
    println("Compile: $compile")
    val upload = cli.upload(sketchFolder, uno, unoId)
    println("Upload: $upload")
}

class ArduinoCLITest {

    companion object {
        const val ARDUINO_VERSION = """{
  "Application": "arduino-cli",
  "VersionString": "0.18.1",
  "Commit": "b3cf8e19",
  "Status": "alpha",
  "Date": "2021-04-13T13:08:30Z"
}"""
        const val ARDUINO_INCOMPATIBLE_VERSION = """{
  "Application": "arduino-cli",
  "VersionString": "0.5.3",
  "Commit": "b3cf8e19",
  "Status": "alpha",
  "Date": "2021-04-13T13:08:30Z"
}
"""
        const val ARDUINO_ILL_DEFINED_VERSION = """{
  "Application": "arduino-cli",
  "VersionString": "0.5.3-SNAPSHOT",
  "Commit": "b3cf8e19",
  "Status": "alpha",
  "Date": "2021-04-13T13:08:30Z"
}
"""
        const val BOARD_LIST = """[
  {
    "address": "COM1",
    "protocol": "serial",
    "protocol_label": "Serial Port"
  },
  {
    "address": "COM3",
    "protocol": "serial",
    "protocol_label": "Serial Port (USB)",
    "boards": [
      {
        "name": "Arduino Uno",
        "fqbn": "arduino:avr:uno",
        "vid": "0x2341",
        "pid": "0x0043"
      }
    ],
    "serial_number": "55737323030351E0B0F1"
  }
]"""
        const val COMPILER_OUTPUT = """{
  "compiler_out": "Sketch uses 924 bytes (2%) of program storage space. Maximum is 32256 bytes.\nGlobal variables use 9 bytes (0%) of dynamic memory, leaving 2039 bytes for local variables. Maximum is 2048 bytes.\n",
  "compiler_err": "",
  "builder_result": {
    "build_path": "C:\\Users\\Marmotte\\AppData\\Local\\Temp\\arduino-sketch-91F26868CA13E35164DC49707DE71477",
    "executable_sections_size": [
      {
        "name": "text",
        "size": 924,
        "max_size": 32256
      },
      {
        "name": "data",
        "size": 9,
        "max_size": 2048
      }
    ]
  },
  "success": true
}"""
    }

    @Test
    fun `test init OK`() {
        // Prepare
        mockkConstructor(ProcessBuilder::class)
        val process = mockk<Process>()
        every { anyConstructed<ProcessBuilder>().start() } returns process
        every { process.waitFor(any(), any()) } returns true
        every { process.exitValue() } returns 0
        every { process.inputStream } returns ARDUINO_VERSION.byteInputStream(StandardCharsets.UTF_8)
        every { process.errorStream } returns "error msg".byteInputStream(StandardCharsets.UTF_8)
        every { process.destroy() } just Runs
        // Build
        val cliLocation = File("target/test-classes/")
        // Perform
        ArduinoCLI(cliLocation, "arduino-cli.exe") // should not raise exception
    }

    @Test
    fun `test init KO by process internal error`() {
        // Prepare
        mockkConstructor(ProcessBuilder::class)
        val process = mockk<Process>()
        every { anyConstructed<ProcessBuilder>().start() } returns process
        every { process.waitFor(any(), any()) } returns false
        every { process.exitValue() } returns -1
        every { process.inputStream } returns ARDUINO_VERSION.byteInputStream(StandardCharsets.UTF_8)
        every { process.errorStream } returns "error msg".byteInputStream(StandardCharsets.UTF_8)
        every { process.destroy() } just Runs
        // Build
        val cliLocation = File("target/test-classes/")
        // Perform
        assertThrows(UnsupportedOperationException::class.java) {
            ArduinoCLI(cliLocation, "arduino-cli.exe") // should not raise exception
        }
    }

    @Test
    fun `test init KO by Exception`() {
        // Prepare
        mockkConstructor(ProcessBuilder::class)
        val process = mockk<Process>()
        every { anyConstructed<ProcessBuilder>().start() } returns process
        every { process.waitFor(any(), any()) } throws RuntimeException("huh, oh")
        every { process.exitValue() } returns -1
        every { process.inputStream } returns ARDUINO_VERSION.byteInputStream(StandardCharsets.UTF_8)
        every { process.errorStream } returns "error msg".byteInputStream(StandardCharsets.UTF_8)
        every { process.destroy() } just Runs
        // Build
        val cliLocation = File("target/test-classes/")
        // Perform
        assertThrows(UnsupportedOperationException::class.java) {
            ArduinoCLI(cliLocation, "arduino-cli.exe") // should not raise exception
        }
    }

    @Test
    fun `test init KO by ill-defined version`() {
        // Prepare
        mockkConstructor(ProcessBuilder::class)
        val process = mockk<Process>()
        every { anyConstructed<ProcessBuilder>().start() } returns process
        every { process.waitFor(any(), any()) } returns true
        every { process.exitValue() } returns 0
        every { process.inputStream } answers { ARDUINO_ILL_DEFINED_VERSION.byteInputStream(StandardCharsets.UTF_8) }
        every { process.errorStream } answers { "error msg".byteInputStream(StandardCharsets.UTF_8) }
        every { process.destroy() } just Runs
        // Build
        val cliLocation = File("target/test-classes/")
        // Perform
        // Verify
        assertThrows(UnsupportedOperationException::class.java) {
            ArduinoCLI(cliLocation, "arduino-cli.exe")
        }
    }

    @Test
    fun `test init KO by bad version`() {
        // Prepare
        mockkConstructor(ProcessBuilder::class)
        val process = mockk<Process>()
        every { anyConstructed<ProcessBuilder>().start() } returns process
        every { process.waitFor(any(), any()) } returns true
        every { process.exitValue() } returns 0
        every { process.inputStream } returns ARDUINO_INCOMPATIBLE_VERSION.byteInputStream(StandardCharsets.UTF_8)
        every { process.errorStream } returns "error msg".byteInputStream(StandardCharsets.UTF_8)
        every { process.destroy() } just Runs
        // Build
        val cliLocation = File("target/test-classes/")
        // Perform
        assertThrows(UnsupportedOperationException::class.java) {
            ArduinoCLI(cliLocation, "arduino-cli.exe")
        }
    }

    @Test
    fun `test getVersion`() {
        // Prepare
        mockkConstructor(ProcessBuilder::class)
        val process = mockk<Process>()
        every { anyConstructed<ProcessBuilder>().start() } returns process
        every { process.waitFor(any(), any()) } returns true
        every { process.exitValue() } returns 0
        every { process.inputStream } answers { ARDUINO_VERSION.byteInputStream(StandardCharsets.UTF_8) }
        every { process.errorStream } answers { "error msg".byteInputStream(StandardCharsets.UTF_8) }
        every { process.destroy() } just Runs
        // Build
        val cliLocation = File("target/test-classes/")
        val cli = ArduinoCLI(cliLocation, "arduino-cli.exe")
        // Perform
        val version = cli.getVersion()
        // Verify
        assertEquals("arduino-cli", version.Application)
        assertEquals("0.18.1", version.VersionString)
        assertEquals("b3cf8e19", version.Commit)
        assertEquals("alpha", version.Status)
        assertEquals("2021-04-13T13:08:30Z", version.Date)
    }

    @Test
    fun `test board list`() {
        // Prepare
        mockkConstructor(ProcessBuilder::class)
        val process = mockk<Process>()
        every { anyConstructed<ProcessBuilder>().start() } returns process
        every { process.waitFor(any(), any()) } returns true
        every { process.exitValue() } returns 0
        every { process.inputStream } returns
                ARDUINO_VERSION.byteInputStream(StandardCharsets.UTF_8) andThen
                BOARD_LIST.trimIndent().byteInputStream(StandardCharsets.UTF_8)
        every { process.errorStream } returns "error msg".byteInputStream(StandardCharsets.UTF_8)
        every { process.destroy() } just Runs
        // Build
        val cliLocation = File("target/test-classes/")
        val cli = ArduinoCLI(cliLocation, "arduino-cli.exe")
        // Perform
        val uploadOutcome = cli.boardList()
        assertEquals(2, uploadOutcome.size)
        // First USB device (not arduino)
        assertEquals("COM1", uploadOutcome[0].address)
        assertEquals("serial", uploadOutcome[0].protocol)
        assertEquals("Serial Port", uploadOutcome[0].protocol_label)
        assertNull(uploadOutcome[0].serial_number)
        assertNull(uploadOutcome[0].boards)
        // Second USB device (Uno)
        assertEquals("COM3", uploadOutcome[1].address)
        assertEquals("serial", uploadOutcome[1].protocol)
        assertEquals("Serial Port (USB)", uploadOutcome[1].protocol_label)
        assertEquals("55737323030351E0B0F1", uploadOutcome[1].serial_number)
        assertNotNull(uploadOutcome[1].boards)
        assertEquals(1, uploadOutcome[1].boards!!.size)
        assertEquals("Arduino Uno", uploadOutcome[1].boards!![0].name)
        assertEquals("arduino:avr:uno", uploadOutcome[1].boards!![0].fqbn)
        assertEquals("0x2341", uploadOutcome[1].boards!![0].vid)
        assertEquals("0x0043", uploadOutcome[1].boards!![0].pid)
    }

    @Test
    fun `test compile`() {
        // Prepare
        mockkConstructor(ProcessBuilder::class)
        val process = mockk<Process>()
        every { anyConstructed<ProcessBuilder>().start() } returns process
        every { process.waitFor(any(), any()) } returns true
        every { process.exitValue() } returns 0
        every { process.inputStream } returns
                ARDUINO_VERSION.byteInputStream(StandardCharsets.UTF_8) andThen
                COMPILER_OUTPUT.trimIndent().byteInputStream(StandardCharsets.UTF_8)
        every { process.errorStream } returns "error msg".byteInputStream(StandardCharsets.UTF_8)
        every { process.destroy() } just Runs
        // Build
        val cliLocation = File("target/test-classes/")
        val cli = ArduinoCLI(cliLocation, "arduino-cli.exe")
        val boardId = ArduinoBoardId(fqbn = "fqbn")
        // Perform
        val compilation = cli.compile(File("src/test/resources/"), boardId)
        // First USB device (not arduino)
        assertEquals(true, compilation.success)
        assertEquals("", compilation.compiler_err)
        assertEquals(
            "Sketch uses 924 bytes (2%) of program storage space. Maximum is 32256 bytes.\nGlobal variables use 9 bytes (0%) of dynamic memory, leaving 2039 bytes for local variables. Maximum is 2048 bytes.\n",
            compilation.compiler_out
        )
        assertTrue(File(compilation.builder_result.build_path).exists())
    }

    @Test
    fun `test upload ok`() {
        // Prepare
        mockkConstructor(ProcessBuilder::class)
        val process = mockk<Process>()
        every { anyConstructed<ProcessBuilder>().start() } returns process
        every { process.waitFor(any(), any()) } returns true
        every { process.exitValue() } returns 0
        every { process.inputStream } returns
                ARDUINO_VERSION.byteInputStream(StandardCharsets.UTF_8) andThen
                "success".byteInputStream(StandardCharsets.UTF_8)
        every { process.errorStream } returns "error msg".byteInputStream(StandardCharsets.UTF_8)
        every { process.destroy() } just Runs
        // Build
        val cliLocation = File("target/test-classes/")
        val cli = ArduinoCLI(cliLocation, "arduino-cli.exe")
        val boardId = ArduinoBoardId(fqbn = "fqbn")
        val board = ArduinoBoard("COM1", "serial", "Serial Port (USB)", listOf(boardId), "123456")
        // Perform
        val uploadOutcome = cli.upload(File("src/test/resources/"), board, boardId)
        assertEquals("success", uploadOutcome)
    }
}