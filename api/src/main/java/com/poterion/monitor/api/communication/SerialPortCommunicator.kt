package com.poterion.monitor.api.communication

import jssc.*
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.zip.CRC32

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class SerialPortCommunicator(portName: String) : SerialPortEventListener {
	companion object {
		private val LOGGER = LoggerFactory.getLogger(SerialPortCommunicator::class.java)
		private const val WRITE_TIMEOUT = 2_000L
		private const val STX = "STX" //: Byte = 0x02
		private const val ETX = "ETX" //: Byte = 0x03
		private const val ENQ = "ENQ" //: Byte = 0x05
		private const val ACK = "ACK" //: Byte = 0x06

		fun findPort(): String? = SerialPortList.getPortNames()
				.map { it to SerialPortCommunicator(it) }
				.mapNotNull { (portName, communicator) ->
					var tries = 0
					var result: Pair<String, Boolean>?
					var exception: SerialPortException? = null
					do {
						result = try {
							portName to communicator.sendMessage(ENQ).get()
						} catch (e: SerialPortException) {
							LOGGER.error(e.message, e)
							exception = e
							null
						}
						tries++
					} while (result == null && tries <= 3 && exception?.exceptionType == SerialPortException.TYPE_PORT_BUSY)
					result
				}
				.mapNotNull { (portName, response) -> if (response) portName else null }
				.firstOrNull()
				.also { if (it == null) LOGGER.warn("No communicator found!") else LOGGER.info("Communicator at ${it}") }
	}

	private enum class Mode {
		READ, WRITE
	}

	private val logTag
		get() = "${serialPort.portName}:${Thread.currentThread().name}(${Thread.currentThread().id})"
	private var listenerAdded = false
	private val serialPort = SerialPort(portName)
	private var checksum: Long? = null
	private var rest = ""
	private val executor = Executors.newSingleThreadExecutor()

	fun sendMessage(message: String): Future<Boolean> = executor.submit(Callable { sendMessageInternal(message) })

	private fun sendMessageInternal(message: String): Boolean {
		Thread.currentThread().name = "Serial Port Communicator Message Sender"
		if (!serialPort.isOpened) try {
			serialPort.openPort()

			serialPort.setParams(SerialPort.BAUDRATE_115200,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE)

			serialPort.purgePort(SerialPort.PURGE_TXCLEAR or SerialPort.PURGE_RXCLEAR or SerialPort.PURGE_RXABORT or SerialPort.PURGE_TXABORT)
		} catch (e: SerialPortException) {
			LOGGER.error(e.message, e)
			return false
		}
		setMode(Mode.WRITE)

		var iterations = 0
		var success: Boolean
		do {
			LOGGER.debug("[${logTag}] Sending tryout ${iterations + 1}")
			try {
				serialPort.purgePort(SerialPort.PURGE_TXCLEAR or SerialPort.PURGE_RXCLEAR or SerialPort.PURGE_RXABORT or SerialPort.PURGE_TXABORT)
			} catch (e: SerialPortException) {
				LOGGER.error(e.message, e)
			}
			val checksum = if (message == ENQ) 20180214L else CRC32().apply {
				update(message.replace("[\\n\\r]".toRegex(), "").toByteArray())
			}.value

			writeMode {
				(1..8).forEach { serialPort.writeString("$ETX\n\r") }
				serialPort.writeString("$STX\n\r")
				serialPort.writeString("${message}\n\r")
				serialPort.writeString("$ETX\n\r")
			}
			setMode(Mode.READ)

			val sent = System.currentTimeMillis()
			while (this.checksum == null && System.currentTimeMillis() - sent < WRITE_TIMEOUT) {
				Thread.sleep(500L)
			}

			LOGGER.info("[${logTag}] Iteration ${iterations}: Received: ${this.checksum}, Calculated: ${checksum}")
			success = checksum == this.checksum
			this.checksum = null
			iterations++
		} while (!success && iterations < 5)

		if (success) writeMode { (1..5).forEach { serialPort.writeString("$ACK\n\r") } }

		if (serialPort.isOpened) try {
			serialPort.closePort()
		} catch (e: SerialPortException) {
			LOGGER.error(e.message, e)
		}

		LOGGER.info("[${logTag}] Sending ${if (success) "SUCCESSFUL" else "FAILED"} after ${iterations} iterations")
		return success
	}

	override fun serialEvent(event: SerialPortEvent?) {
		if (event != null && event.isRXCHAR && event.eventValue > 0) try { // Received Data - Carries data from DCE to DTE.
			val lines = serialPort.readBytes(event.eventValue)
					.toString(Charsets.UTF_8)
					.split("[\\n\\r]+".toRegex())
			LOGGER.debug("[${logTag}] Received ${event.eventValue} bytes, ${lines.size} lines:${lines.joinToString("\n\t- ", "\n\t- ")}")

			lines.forEachIndexed { index, line ->
				when (index) {
					0 -> "${rest}${line}".also { rest = "" }
					lines.lastIndex -> {
						rest = line
						""
					}
					else -> line
				}.also {
					LOGGER.debug("[${logTag}] Processing: \"${it}\"")
					try {
						processLine(it)
					} catch (e: SerialPortException) {
						LOGGER.error(e.message, e)
					}
				}
			}

		} catch (e: SerialPortException) {
			LOGGER.error(e.message, e)
		}
	}

	private fun processLine(line: String): Boolean = if (line.matches("$ACK:\\d+".toRegex())) {
		line.substring(ACK.length + 1).toLongOrNull()
				.also { LOGGER.debug("[${logTag}] Checksum: \"${it}\"") }
				.also { this.checksum = it } != null
	} else false

	private inline fun <T> writeMode(block: () -> T?): T? {
		setMode(Mode.WRITE)
		val result = try {
			block.invoke()
		} catch (e: SerialPortException) {
			LOGGER.error(e.message, e)
			null
		}
		//setMode(Mode.READ)
		return result
	}

	private fun setMode(mode: Mode) {
		listenerAdded = if (serialPort.isOpened) when (mode) {
			Mode.READ -> {
				try {
					serialPort.purgePort(SerialPort.PURGE_TXCLEAR or SerialPort.PURGE_RXCLEAR or SerialPort.PURGE_RXABORT or SerialPort.PURGE_TXABORT)
					serialPort.purgePort(SerialPort.PURGE_TXCLEAR or SerialPort.PURGE_RXCLEAR)
				} catch (e: SerialPortException) {
					LOGGER.error(e.message, e)
				}
				if (!listenerAdded) try {
					serialPort.addEventListener(this)
				} catch (e: SerialPortException) {
					LOGGER.error(e.message, e)
				}
				true
			}
			Mode.WRITE -> {
				try {
					serialPort.purgePort(SerialPort.PURGE_TXCLEAR or SerialPort.PURGE_RXCLEAR)
				} catch (e: SerialPortException) {
					LOGGER.error(e.message, e)
				}
				if (listenerAdded) try {
					serialPort.removeEventListener()
				} catch (e: SerialPortException) {
					LOGGER.error(e.message, e)
				}
				false
			}
		} else false
	}

}