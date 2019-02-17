package com.poterion.monitor.api.communication

import javafx.application.Platform
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import javax.bluetooth.UUID
import javax.microedition.io.Connector
import javax.microedition.io.StreamConnection

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class BluetoothCommunicatorEmbedded(private var address: String, var shouldConnect: Boolean) {

	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(BluetoothCommunicatorEmbedded::class.java)
	}

	private val url: String
		get() = "btspp://${address.replace(":", "")}:6;authenticate=false;encrypt=false;master=false"

	private val messageQueue: ConcurrentLinkedQueue<ByteArray> = ConcurrentLinkedQueue()
	private var messageQueueProgressSize = 0
	private val chksumQueue: ConcurrentLinkedQueue<Byte> = ConcurrentLinkedQueue()
	private var lastChecksum: Int? = null
	private val listeners = mutableListOf<BluetoothEmbeddedListener>()
	//private var isChanging = false


	private var streamConnection: StreamConnection? = null
	private var outputStream: OutputStream? = null
	private var inputStream: InputStream? = null

	private val connectorExecutor = Executors.newSingleThreadExecutor()
	private val inboundExecutor = Executors.newSingleThreadExecutor()
	private val outboundExecutor = Executors.newSingleThreadExecutor()

	private var connectorThread: Thread? = null
	private var inboundThread: Thread? = null
	private var outboundThread: Thread? = null
	private var stateMachineBuffer: ByteArray = ByteArray(32_768)

	var isConnected = false

	//private val interrupted: AtomicBoolean = AtomicBoolean(false)

	private val connectorRunnable: () -> Unit = {
		while (shouldConnect && !isConnected) {
			if (streamConnection != null || inputStream != null || outputStream != null) {
				disconnect()
			}

			LOGGER.debug("Connecting to ${address}...")

			inboundThread?.takeIf { it.isAlive }?.interrupt()
			outboundThread?.takeIf { it.isAlive }?.interrupt()

			while (streamConnection == null) try {
				//Create a UUID for SPP
				val uuid = UUID("1101", true) // 1101
				//open server url
				streamConnection = Connector.open(url) as StreamConnection //StreamConnectionNotifier //StreamConnection
				isConnected = true

				//Wait for client connection
				println("\nServer Started. Waiting for clients to connect...")

				//val connection = streamConnNotifier!!.acceptAndOpen()

				//println("Remote device address: " + RemoteDevice.getRemoteDevice(connection).bluetoothAddress)
				//println("Remote device name: " + RemoteDevice.getRemoteDevice(connection).getFriendlyName(true))

				//the stream is opened both in and out
				outputStream = streamConnection?.openOutputStream()
				inputStream = streamConnection?.openInputStream()
				isConnected = true
				listeners.forEach { Platform.runLater(it::onConnect) }

				inboundThread?.takeIf { !it.isInterrupted }?.interrupt()
				outboundThread?.takeIf { !it.isInterrupted }?.interrupt()

				inboundThread = Thread(inboundRunnable)
				outboundThread = Thread(outboundRunnable)

				inboundExecutor.execute(inboundThread)
				outboundExecutor.execute(outboundThread)
				listeners.forEach { Platform.runLater { it.onProgress(0, 0, false) } }
			} catch (e: IOException) {
				LOGGER.error(e.message, e)
				Thread.sleep(10_000L)
				//e.printStackTrace()
				//in case of problems, the connection is stopped
				disconnect()
			}
		}
	}

	private val outboundRunnable: () -> Unit = {
		try {
			while (!Thread.interrupted()) {
				if (messageQueue.isNotEmpty() || chksumQueue.isNotEmpty()) {
					if (chksumQueue.isNotEmpty()) {
						val chksum = chksumQueue.poll()
						var data = listOf(BluetoothMessageKind.CRC.byteCode, chksum).toByteArray()
						data = listOf(data.calculateChecksum().toByte(), BluetoothMessageKind.CRC.byteCode, chksum).toByteArray()
						//outputStream?.write(data.calculateChecksum())
						outputStream?.write(data)
						outputStream?.flush()
						LOGGER.debug("ACK CRC: ${"0x%02X".format(chksum)}")
					} else if (messageQueue.isNotEmpty()) {
						if (messageQueueProgressSize < messageQueue.size) messageQueueProgressSize = messageQueue.size
						val message = messageQueue.peek()
						var correctlyReceived = false
						var retries = 0
						while (correctlyReceived.not() && retries < 3) {
							val checksum = message.calculateChecksum()
							val data = listOf(checksum.toByte(), *message.toTypedArray()).toByteArray()
							outputStream?.write(data)
							outputStream?.flush()

							var timeout = 2_000L // ms
							while (lastChecksum != checksum && timeout > 0) {
								Thread.sleep(1)
								timeout--
							}

							correctlyReceived = checksum == lastChecksum
							retries++
							LOGGER.debug("Outbound CRC: calculated=${checksum}, received=${lastChecksum} => ${correctlyReceived}, remaining=${messageQueue.size}")
						}
						if (correctlyReceived) {
							messageQueue.poll()
							Thread.sleep(1_000L)
							listeners.forEach { Platform.runLater { it.onProgress(messageQueueProgressSize - messageQueue.size, messageQueueProgressSize, false) } }
							if (messageQueueProgressSize == messageQueue.size) messageQueueProgressSize = 0
							lastChecksum = null
						}
					}
				} else {
					Thread.sleep(100L)
				}
			}
		} catch (e: IOException) {
			LOGGER.warn(e.message)
			isConnected = false
		}
		disconnect()
	}

	private val inboundRunnable: () -> Unit = {
		try {
			while (!Thread.interrupted()) {
				val buffer = ByteArray(96)
				val read = inputStream?.read(buffer) ?: 0

				if (read > 0) {
					val chksumReceived = buffer[0].toInt() and 0xFF
					val chksum = buffer.toList().subList(1, read).toByteArray().calculateChecksum()

					if (chksum == chksumReceived) {
						val messageKind = buffer[1]
								.let { byte -> BluetoothMessageKind.values().find { it.byteCode == byte } }
								?: BluetoothMessageKind.UNKNOWN

						when (messageKind) {
							BluetoothMessageKind.CRC -> lastChecksum = (buffer[2].toInt() and 0xFF)
							BluetoothMessageKind.PUSH_STATE_MACHINE -> {
								val length = ((buffer[2].toInt() shl 8) and 0xFF00) or (buffer[3].toInt() and 0xFF)
								val addr = ((buffer[4].toInt() shl 8) and 0xFF00) or (buffer[5].toInt() and 0xFF)
								if (addr == 0) (0 until stateMachineBuffer.size)
										.forEach { stateMachineBuffer[it] = 0xFF.toByte() }

								(0 until (read - 6)).forEach {
									stateMachineBuffer[it + addr] = buffer[it + 6]
								}

								chksumQueue.add(chksum.toByte())

								listeners.forEach { Platform.runLater { it.onProgress(addr + read - 6, length, true) } }
								LOGGER.debug("State Machine address: 0x%02X - 0x%02X, size: %d bytes, transfered: %d bytes, total: %d bytes"
										.format(addr, addr + read - 7, read - 6, addr + read - 6, length))

								if (addr + read - 6 == length) listeners
										.forEach { Platform.runLater { it.onStateMachine(stateMachineBuffer) } }

								BufferedWriter(FileWriter("sm.bin")).use { writer ->
									var chars = ""
									stateMachineBuffer.forEachIndexed { i, b ->
										if (i % 8 == 0) writer.write("%04x: ".format(i))
										chars += if (b in 32..126) b.toChar() else '.'
										writer.write("0x%02X%s".format(b, if (b > 255) "!" else " "))
										if (i % 8 == 7) {
											writer.write(" ${chars} |\n")
											chars = ""
										}
									}
								}
							}
							else -> {
							}
						}
//						else -> {
						//						val lengthUpper = inputStream?.read() ?: 0
						//						val lengthLower = inputStream?.read() ?: 0
						//						val length = lengthUpper * 256 + lengthLower
						//						val message = ByteArray(length)
						//						inputStream?.read(message)
						//						val checksum = message.calculateChecksum()
						//						messageQueue.add(listOf(BluetoothMessageKind.CRC.byteCode, checksum).toByteArray())
						//
						//						LOGGER.info("Inbound[${"0x%02X".format(messageKind)}]:" +
						//								" ${message.map { "0x%02X".format(it) }.joinToString(" ")}" +
						//								" \"${message.map { if (it in 32..126) it.toChar() else '.' }}\"" +
						//								" (checksum: ${"0x%02X".format(checksum)})")
						//
						//						listeners.forEach { it.onMessage(messageKind, message) }
//						}
					}
				}
			}
		} catch (e: IOException) {
			LOGGER.info(e.message)
		}
		disconnect()
	}

	fun connect(address: String? = null) {
		if (shouldConnect && (address == null || address != this.address || !isConnected)) {
			listeners.forEach { Platform.runLater { it.onProgress(-1, 0, false) } }
			if (address != null) this.address = address
			if (connectorThread?.isAlive != true) {
				connectorThread = Thread(connectorRunnable)
				connectorExecutor.execute(connectorRunnable)
			}
		}
	}

	fun disconnect() {
		//isChanging = true
		LOGGER.debug("Disconnecting from ${address}...")
		listeners.forEach { Platform.runLater(it::onDisconnect) }
		try {
			if (inboundThread?.isAlive == true) inboundThread?.interrupt()
			if (outboundThread?.isAlive == true) outboundThread?.interrupt()
			if (connectorThread?.isAlive == true) connectorThread?.interrupt()
			inputStream?.close()
			inputStream = null
			outputStream?.close()
			outputStream = null
			streamConnection?.close()
			streamConnection = null
		} catch (e: IOException) {
			LOGGER.error(e.message, e)
		} finally {
			//set false and change bluetooth label to alert the entire program
			isConnected = false
		}
		//isChanging = false
	}

	fun send(kind: BluetoothMessageKind, message: ByteArray = emptyArray<Byte>().toByteArray()) = message
			.let { data ->
				ByteArray(data.size + 1) { i ->
					when (i) {
						0 -> kind.byteCode
						else -> data[i - 1]
					}
				}.also { messageQueue.offer(it) }
			}
			.also { listeners.forEach { Platform.runLater { it.onProgress(-1, 1, false) } } }

	fun register(listener: BluetoothEmbeddedListener) {
		if (!listeners.contains(listener)) listeners.add(listener)
	}

	fun unregister(listener: BluetoothEmbeddedListener) = listeners.remove(listener)

	private fun ByteArray.calculateChecksum() = (map { it.toInt() }.takeIf { it.isNotEmpty() }?.reduce { acc, i -> acc + i }
			?: 0) and 0xFF
}