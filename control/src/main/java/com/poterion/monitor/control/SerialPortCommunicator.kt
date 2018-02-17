package com.poterion.monitor.control

import jssc.SerialNativeInterface
import jssc.SerialPort
import jssc.SerialPortException
import org.slf4j.LoggerFactory

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class SerialPortCommunicator(portName: String) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger("SerCOM")

        fun findCommunicator(): SerialPortCommunicator? = SerialNativeInterface()
                .serialPortNames
                .map { it to SerialPortCommunicator(it) }
                .map { (portName, communicator) -> portName to communicator.sendMessage("ID")}
                .map { (portName, response) -> if (response.isNotEmpty()) SerialPortCommunicator(portName) else null }
                .filterNotNull()
                .firstOrNull()
                .also { if (it == null) LOGGER.warn("No communicator found!") else LOGGER.info("Communicator at ${it.portName}") }
    }

    var portName: String
        get() = serialPort.portName
        set(value) {
            serialPort = SerialPort(value)
        }

    private var serialPort = SerialPort(portName)

    private var success = true
    private var receivingConfirmation = false
    private val confirmation = mutableListOf<String>()

    fun sendMessage(vararg message: String) = sendMessage(message.toList())

    fun sendMessage(message: List<String>): List<String> {
        serialPort.openPort()
        serialPort.setParams(SerialPort.BAUDRATE_9600,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE)

        serialPort.addEventListener({ event ->
            if (event.isRXCHAR && event.eventValue > 0) try {
                val receivedData = serialPort.readString(event.eventValue)
                receivedData.split("[\\n\\r]+".toRegex()).filter { it.isNotEmpty() && it.isNotBlank() }.forEach {
                    //LOGGER.info(">> ${it}")
                    when (it) {
                        "CONFIRMATION_BEGIN" -> receivingConfirmation = true
                        "CONFIRMATION_END" -> receivingConfirmation = false
                        else -> if (receivingConfirmation) {
                            LOGGER.info("Received response: ${it}")
                            confirmation.add(it)
                        }
                    }
                }
            } catch (e: SerialPortException) {
                LOGGER.error("Error in receiving string from COM-port: ${e.message}", e)
                success = false
            }
        }, SerialPort.MASK_RXCHAR)

        var tries = 0
        val maxTries = if (message.isCommand()) 2 else 5
        do {
            sentData(serialPort, message)
            Thread.sleep(500L)
            tries++
        } while (!success && tries < maxTries)
        serialPort.closePort()

        return if (success) {
            LOGGER.info("SUCCESS after ${tries} retries")
            listOf(*confirmation.toTypedArray())
        } else {
            LOGGER.error("FAILURE with ${tries} retries!")
            emptyList()
        }
    }

    private fun sentData(serialPort: SerialPort, message: List<String>): Boolean {
        success = true
        confirmation.clear()
        serialPort.writeString("END\nEND\nEND\nEND\nEND\nEND\n")
        if (!message.isCommand()) serialPort.writeString("START\n")
        message.forEach { serialPort.writeString("${it}\n") }
        if (!message.isCommand()) serialPort.writeString("END\n")

        val sentTimestamp = System.currentTimeMillis()
        var confirmed: Boolean
        do {
            confirmed = confirmMessage(message, confirmation)
            if (!confirmed) Thread.sleep(500L)
        } while (!confirmed && !timedOut(sentTimestamp))

        success = success && confirmed
        if (success) LOGGER.info("Message send successfully") else LOGGER.warn("Message was not received correctly!")
        return success
    }

    private fun List<String>.isCommand(): Boolean = size == 1 && listOf("ID").contains(get(0))

    private fun timedOut(start: Long) = System.currentTimeMillis() - start > 2_000L

    private fun confirmMessage(sent: List<String>, received: List<String>) =
            sent.isEmpty()
                    || sent.size == 1 && sent[0] == "ID" && received.size == 1 && received[0].startsWith("POTERION IOT:")
                    || sent[0] != "ID" && sent.size == received.size && (0 until sent.size)
                    .map { it.also { LOGGER.info("Compare: ${sent[it]} - ${received[it]} > ${sent[it] == received[it]}") } }
                    .map { sent[it] == received[it] }.reduce { acc, b -> acc && b }

}