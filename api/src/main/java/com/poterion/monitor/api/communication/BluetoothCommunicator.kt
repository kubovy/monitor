package com.poterion.monitor.api.communication

import javafx.application.Platform
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.CRC32
import javax.microedition.io.Connector
import javax.microedition.io.StreamConnection

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class BluetoothCommunicator(private var prefix: String, private var address: String, private val outboundPort: Int, private val inboundPort: Int) {

	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(BluetoothCommunicator::class.java)
		private const val SERVICE_NAME = "Poterion Monitor"
		private const val STX = "STX"
		private const val ETX = "ETX"
		private const val ENQ = "ENQ"
		private const val IDD = "IDD"
		private const val ACK = "ACK"
		private const val NOP = "NOP"

		private const val BUFFER = 1024
	}

	private val outboundUrl: String
		get() = "btspp://${address.replace(":", "")}:${outboundPort};authenticate=false;encrypt=false;master=false"
	private val inboundUrl: String
		get() = "btspp://${address.replace(":", "")}:${inboundPort};authenticate=false;encrypt=false;master=false"

	private val queue: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()
	private val listeners = mutableListOf<BluetoothListener>()
	private var isChanging = false
	var shouldConnect = true

	private var inboundThread: Thread? = null
	private var outboundThread: Thread? = null
	var isInboundConnected = false
	var isOutboundConnected = false

	private val interrupted: AtomicBoolean = AtomicBoolean(false)

	private val connectorRunnable = {
		while (true) {
			if (shouldConnect && !isChanging && (!isInboundConnected || !isOutboundConnected)) {
				Platform.runLater { connect() }
			}
			Thread.sleep(10_000)
		}
	}

	private val outboundRunnable = {
		try {
			val streamConnection = Connector.open(outboundUrl) as StreamConnection
			isOutboundConnected = true
			listeners.forEach { Platform.runLater(it::onOutboundConnect) }
			val buffer = ByteArray(BUFFER)

			streamConnection.openOutputStream().use { output ->
				streamConnection.openInputStream().use { input ->
					output.write("IDD:${SERVICE_NAME}\n".toByteArray())

					try {
						var loop = 0
						while (!interrupted.get()) {
							if (queue.isNotEmpty()) {
								queue.poll()?.takeIf { it.isNotEmpty() }?.also { message ->
									var correctlyReceived = false
									var retries = 0
									while (correctlyReceived.not() && retries < 3) {
										val checksum = CRC32()
										output.write("${STX}\n".toByteArray())
										"${prefix}:${message}".chunked(BUFFER).forEach { chunk ->
											output.write(chunk.toByteArray())
											val bytes = chunk.toByteArray()
											checksum.update(bytes, 0, bytes.size)
										}
										output.write("\n$ETX\n".toByteArray())

										val crc32Calculated = checksum.value

										val read = input.read(buffer)
										val data = String(buffer.sliceArray(0 until read))
										LOGGER.debug("Outbound received: \"${data.trim()}\"")
										val ack = data.trim().split(":", limit = 2)
										correctlyReceived = crc32Calculated == ack[1].toLong()
										retries++
										LOGGER.debug("Outbound ${ack[0]}: calculated=${crc32Calculated}, received=${ack[1]} => ${correctlyReceived}")
									}
								}
							} else {
								if (loop == 0) output.write("$NOP\n".toByteArray())
								loop = (loop + 1) % 10
								Thread.sleep(1_000)
							}
						}
					} catch (e: IOException) {
						LOGGER.warn(e.message)
						isOutboundConnected = false
					}
				}
			}
			try {
				streamConnection.close()
			} catch (e: IOException) {
				LOGGER.warn(e.message)
			}
			isOutboundConnected = false
			listeners.forEach { Platform.runLater(it::onOutboundDisconnect) }
		} catch (e: IOException) {
			LOGGER.info(e.message)
		}
	}

	private val inboundRunnable = {
		try {
			val streamConnection = Connector.open(inboundUrl) as StreamConnection
			isInboundConnected = true
			listeners.forEach { Platform.runLater(it::onInboundConnect) }
			var incomplete = ""
			var bufferIncoming = ""

			streamConnection.openInputStream().use { input ->
				streamConnection.openOutputStream().use { output ->

					while (!interrupted.get()) {
						val buffer = ByteArray(BUFFER)
						val read = input.read(buffer)
						val data = String(buffer.sliceArray(0 until read))
						val lines = data.split("\n")

						for ((idx, l) in lines.withIndex()) {
							var line = l
							if (idx == 0) {  // Fist line is prepended with "incomplete" message from last transmission
								line = incomplete + line
								incomplete = ""
							}

							when {
								idx == lines.size - 1 -> // Last is incomplete, may be empty in case of complete transmission
									incomplete = line
								line == STX -> bufferIncoming = ""
								line == ETX -> {
									//self.notify(self.buffer_incoming)
									LOGGER.debug("Inbound message: >>>${bufferIncoming}<<<")

									val checksum = CRC32()
									val bytes = bufferIncoming.toByteArray()
									checksum.update(bytes, 0, bytes.size)
									val crc32 = checksum.value
									output.write("ACK:${crc32}\n".toByteArray())
									LOGGER.debug("Inbound ACK:${crc32}")
									listeners.forEach {
										val message = bufferIncoming // Needs to be duplicated otherwise gets deleted.
										Platform.runLater { it.onMessage(message) }
									}
									bufferIncoming = ""
								}
								line == ENQ -> output.write("IDD:$SERVICE_NAME\n".toByteArray())
								line.startsWith("$IDD:") -> {
									// pass
								}
								line.startsWith("$ACK:") -> {
									// pass
								}
								line == NOP -> {
									// pass
								}
								else -> {
									if (bufferIncoming.isNotEmpty()) bufferIncoming += "\n"
									bufferIncoming += line
								}
							}
						}
					}
				}
			}
			try {
				streamConnection.close()
			} catch (e: IOException) {
				LOGGER.warn(e.message)
			}
			isOutboundConnected = false
			listeners.forEach { Platform.runLater(it::onInboundDisconnect) }
		} catch (e: IOException) {
			LOGGER.info(e.message)
		}
	}

	init {
		Thread(connectorRunnable).start()
	}

	fun connect(address: String? = null) {
		if (address == null || address != this.address || !isInboundConnected || !isOutboundConnected) {
			if (address != null) this.address = address
			isChanging = true
			disconnect()
			LOGGER.debug("Connecting...")
			interrupted.set(false)
			inboundThread = Thread(inboundRunnable)
			outboundThread = Thread(outboundRunnable)
			inboundThread?.start()
			outboundThread?.start()
			isChanging = false
		}
	}

	fun disconnect() {
		isChanging = true
		LOGGER.debug("Disconnecting...")
		interrupted.set(true)
		inboundThread?.join(5_000)
		outboundThread?.join(5_000)
		isChanging = false
	}

	fun send(message: String) = queue.offer(message)

	fun register(listener: BluetoothListener) {
		if (!listeners.contains(listener)) listeners.add(listener)
	}

	fun unregister(listener: BluetoothListener) = listeners.remove(listener)
}