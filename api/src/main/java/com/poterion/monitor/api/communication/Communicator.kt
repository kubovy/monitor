package com.poterion.monitor.api.communication

import javafx.application.Platform
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Communicator.
 *
 * @author Jan Kubovy <jan@kubovy.eu>
 */
abstract class Communicator<ConnectionDescriptor>(private val channel: Channel) {

	companion object {
		private val LOGGER: Logger = LoggerFactory.getLogger(Communicator::class.java)
		//const val MAX_PACKET_SIZE = 32
		const val IDD_PING = false
	}

	private enum class State {
		DISCONNECTED,
		CONNECTING,
		CONNECTED,
		DISCONNECTING;
	}

	val isConnected: Boolean
		get() = state == State.CONNECTED

	val isConnecting: Boolean
		get() = state == State.CONNECTING

	val isDisconnecting: Boolean
		get() = state == State.DISCONNECTING

	val isDisconnected: Boolean
		get() = state == State.DISCONNECTED

	private var state = State.DISCONNECTED
	private var iddState = 0x00
	private var iddCounter = 0
	private var attempt = 0

	private val messageQueue: ConcurrentLinkedQueue<Pair<ByteArray, Long?>> = ConcurrentLinkedQueue()
	private val checksumQueue: ConcurrentLinkedQueue<Byte> = ConcurrentLinkedQueue()
	private var lastChecksum: Int? = null

	private val connectorExecutor = Executors.newSingleThreadExecutor()
	private val inboundExecutor = Executors.newSingleThreadExecutor()
	private val outboundExecutor = Executors.newSingleThreadExecutor()

	private var connectorThread: Thread? = null
	private var inboundThread: Thread? = null
	private var outboundThread: Thread? = null

	private val listeners = mutableListOf<CommunicatorListener>()

	protected var connectionDescriptor: ConnectionDescriptor? = null

	private val connectorRunnable: () -> Unit = {
		while (state == State.CONNECTING) {
			LOGGER.debug("${channel} ${connectionDescriptor}> Connection attempt ...")
			cleanUpConnection()

			inboundThread?.takeIf { it.isAlive }?.interrupt()
			outboundThread?.takeIf { it.isAlive }?.interrupt()

			while (state == State.CONNECTING) try {
				if (createConnection()) {
					state = State.CONNECTED
					iddCounter = 0
					iddState = 0x00

					inboundThread?.takeIf { !it.isInterrupted }?.interrupt()
					outboundThread?.takeIf { !it.isInterrupted }?.interrupt()

					inboundThread = Thread(inboundRunnable)
					inboundThread?.name = "${channel}-inbound"
					outboundThread = Thread(outboundRunnable)
					outboundThread?.name = "${channel}-outbound"

					inboundExecutor.execute(inboundThread)
					outboundExecutor.execute(outboundThread)
					listeners.forEach { Platform.runLater { it.onConnect(channel) } }
				}
			} catch (e: Exception) {
				LOGGER.error("${channel} ${connectionDescriptor}> ${e.message}", e)
				disconnect()
			}
		}
		LOGGER.debug("${channel} ${connectionDescriptor}> Connection thread exited")
	}

	private val inboundRunnable: () -> Unit = {
		try {
			while (!Thread.interrupted() && state == State.CONNECTED) try {
				val message = nextMessage()

				if (message != null) {
					val chksumReceived = message[0].toInt() and 0xFF
					val chksum = message.toList().subList(1, message.size).toByteArray().calculateChecksum()
					LOGGER.debug("${channel} ${connectionDescriptor}> Inbound  RAW [${"0x%02X".format(chksumReceived)}/${"0x%02X".format(chksum)}]:" +
							" ${message.joinToString(" ") { "0x%02X".format(it) }}")

					if (chksum == chksumReceived) {
						val messageKind = message[1]
								.let { byte -> MessageKind.values().find { it.code.toByte() == byte } }
								?: MessageKind.UNKNOWN

						if (messageKind != MessageKind.CRC) checksumQueue.add(chksum.toByte())

						when (messageKind) {
							MessageKind.CRC -> {
								lastChecksum = (message[2].toInt() and 0xFF)
								LOGGER.debug("${channel} ${connectionDescriptor}> Inbound  [CRC] ${"0x%02X".format(lastChecksum)}")
							}
							MessageKind.IDD -> {
								if (message.size > 3) iddState = message[3].toUInt() + 1
								LOGGER.debug("${channel} ${connectionDescriptor}> Inbound  [IDD] ${"0x%02X".format(iddState)}")
								listeners.forEach { Platform.runLater { it.onMessageReceived(channel, message.toIntArray()) } }
							}
							else -> {
								LOGGER.debug("${channel} ${connectionDescriptor}> Inbound "
										+ " [${"0x%02X".format(messageKind)}]"
										+ " ${message.joinToString(" ") { "0x%02X".format(it) }}")
								listeners.forEach { Platform.runLater { it.onMessageReceived(channel, message.toIntArray()) } }
							}
						}
					}
				}
			} catch (e: Exception) {
				LOGGER.error("${channel} ${connectionDescriptor}> ${e.message}")
				if (state == State.CONNECTED) reconnect() else disconnect()
			}
		} catch (e: Exception) {
			LOGGER.warn("${channel} ${connectionDescriptor}> ${e.message}")
			if (state == State.CONNECTED) reconnect() else disconnect()
		}
		LOGGER.debug("${channel} ${connectionDescriptor}> Inbound thread exited")
	}

	private val outboundRunnable: () -> Unit = {
		try {
			while (!Thread.interrupted() && state == State.CONNECTED) try {
				if (checksumQueue.isNotEmpty()) {
					val chksum = checksumQueue.poll()
					var data = listOf(MessageKind.CRC.code.toByte(), chksum).toByteArray()
					data = listOf(data.calculateChecksum().toByte(), MessageKind.CRC.code.toByte(), chksum).toByteArray()
					sendMessage(data)
					LOGGER.debug("${channel} ${connectionDescriptor}> Outbound [CRC] ${"0x%02X".format(chksum)}"
							+ " (checksum queue: ${checksumQueue.size})")
					//listeners.forEach { Platform.runLater { it.onMessageSent(channel, data, messageQueue.size) } }
				} else if (messageQueue.isNotEmpty()) {
					val (message, delay) = messageQueue.peek()
					val kind = MessageKind.values().find { it.code.toByte() == message[0] }
					val checksum = message.calculateChecksum()
					val data = listOf(checksum.toByte(), *message.toTypedArray()).toByteArray()
					lastChecksum = null
					sendMessage(data)

					LOGGER.debug("${channel} ${connectionDescriptor}> Outbound"
							+ " [${"0x%02X".format(lastChecksum)}/${"0x%02X".format(checksum)}]"
							+ " ${data.joinToString(" ") { "0x%02X".format(it) }} (attempt: ${++attempt})")


					var timeout = delay ?: 500 // default delay in ms
					while (lastChecksum != checksum && timeout > 0) {
						Thread.sleep(1)
						timeout--
					}

					val correctlyReceived = checksum == lastChecksum
					if (correctlyReceived) {
						messageQueue.poll()
						attempt = 0
					}
					when (kind) {
						MessageKind.CRC -> {
						}
						MessageKind.IDD -> {
							if (correctlyReceived) {
								LOGGER.debug("${channel} ${connectionDescriptor}> Outbound [${"0x%02X".format(lastChecksum)}/${"0x%02X".format(checksum)}]:" +
										" ${data.joinToString(" ") { "0x%02X".format(it) }}" +
										" (remaining: ${messageQueue.size})")
								iddCounter = -5
							} else {
								LOGGER.debug("${channel} ${connectionDescriptor}> ${iddCounter + 1}. ping not returned")
								iddCounter++
							}
							if (iddCounter > 4) {
								if (state == State.CONNECTED) reconnect() else disconnect()
							}
						}
						else -> {
							LOGGER.debug("${channel} ${connectionDescriptor}> Outbound [${"0x%02X".format(lastChecksum)}/${"0x%02X".format(checksum)}]:" +
									" ${data.joinToString(" ") { "0x%02X".format(it) }}" +
									" SUCCESS (queue: ${messageQueue.size})")
							if (correctlyReceived) listeners
									.forEach { Platform.runLater { it.onMessageSent(channel, data.toIntArray(), messageQueue.size) } }
						}
					}
					if (correctlyReceived) lastChecksum = null
				} else {
					if (iddCounter < 0) {
						Thread.sleep(100L)
						iddCounter++
					} else if (iddCounter == 0) {

						val message = if (iddState < 0x02) arrayOf(MessageKind.IDD.code, Random.nextBits(4), iddState)
						else if (IDD_PING) arrayOf(MessageKind.IDD.code, Random.nextBits(4))
						else null

						if (message != null) messageQueue.add(message.map { it.toByte() }.toByteArray() to 500)
					}
				}
			} catch (e: Exception) {
				LOGGER.error("${channel} ${connectionDescriptor}> ${e.message}")
				if (state == State.CONNECTED) reconnect() else disconnect()
			}
		} catch (e: Exception) {
			LOGGER.warn("${channel} ${connectionDescriptor}> ${e.message}")
			if (state == State.CONNECTED) reconnect() else disconnect()
		}
		LOGGER.debug("${channel} ${connectionDescriptor}> Outbound thread exited")
	}

	/**
	 * Wherther the communicator can connect or not.
	 *
	 * @return True, all conditions to establish a connection are met.
	 */
	open fun canConnect(descriptor: ConnectionDescriptor): Boolean = state == State.DISCONNECTED
			|| connectionDescriptor != descriptor

	/**
	 * Creates a new connection.
	 *
	 * @return True, if a new connection was established.
	 */
	abstract fun createConnection(): Boolean

	/**
	 * Cleans up connection after disconnecting.
	 */
	abstract fun cleanUpConnection()

	/**
	 * Next message getter.
	 *
	 * @return New available message on the channel or <code>null</code> if no new message is available.
	 */
	abstract fun nextMessage(): ByteArray?

	/**
	 * Sends a message through the channel.
	 *
	 * @param data The message.
	 */
	abstract fun sendMessage(data: ByteArray)

	/**
	 * Queues a new message to be sent to target device.
	 *
	 * @param kind Message kind.
	 * @param message Message.
	 */
	fun send(kind: MessageKind, message: ByteArray = byteArrayOf()) = message
			.let { data ->
				ByteArray(data.size + 1) { i ->
					when (i) {
						0 -> kind.code.toByte()
						else -> data[i - 1]
					}
				}.also { messageQueue.offer(it to kind.delay) }
			}

	/**
	 * Register a new listener.
	 *
	 * @param listener Listener to register.
	 */
	fun register(listener: CommunicatorListener) {
		if (!listeners.contains(listener)) listeners.add(listener)
	}

	/**
	 * Unregister an existing listener.
	 *
	 * @param listener Listener to unregister.
	 */
	fun unregister(listener: CommunicatorListener) = listeners.remove(listener)

	/**
	 * Connects to a device.
	 *
	 * @param descriptor Descriptor of the device.
	 */
	fun connect(descriptor: ConnectionDescriptor): Boolean {
		if (canConnect(descriptor)) {
			LOGGER.debug("${channel} ${connectionDescriptor}> Connecting ...")
			if (state == State.CONNECTED) disconnect()

			state = State.CONNECTING
			messageQueue.clear()
			checksumQueue.clear()

			listeners.forEach { Platform.runLater { it.onConnecting(channel) } }

			connectionDescriptor = descriptor

			if (connectorThread?.isAlive == true) connectorThread?.interrupt()
			if (connectorThread?.isAlive != true) {
				connectorThread = Thread(connectorRunnable)
				connectorThread?.name = "${channel}-connector"
				connectorExecutor.execute(connectorThread)
			}
			return true
		}
		return false
	}

	/**
	 * Reconnects to a currently connected device.
	 */
	private fun reconnect() {
		disconnect()
		Thread.sleep(100L)
		connectionDescriptor?.also { connect(it) }
	}

	/** Disconnects from a device. */
	fun disconnect() {
		LOGGER.debug("${channel} ${connectionDescriptor}> Disconnecting ...")
		state = State.DISCONNECTING
		messageQueue.clear()
		checksumQueue.clear()

		try {
			if (inboundThread?.isAlive == true) inboundThread?.interrupt()
			if (outboundThread?.isAlive == true) outboundThread?.interrupt()
			if (connectorThread?.isAlive == true) connectorThread?.interrupt()
			cleanUpConnection()
		} catch (e: IOException) {
			LOGGER.error("${channel} ${connectionDescriptor}> ${e.message}", e)
		} finally {
			state = State.DISCONNECTED
			listeners.forEach { Platform.runLater { it.onDisconnect(channel) } }
		}
	}

	/**
	 * Shuts the communicator down completely.
	 */
	open fun shutdown() {
		disconnect()
		LOGGER.debug("${channel} ${connectionDescriptor}> Shutting down communicator ...")
		connectorThread?.takeIf { it.isAlive }?.interrupt()
		connectorExecutor.shutdown()
		connectorExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)
		inboundThread?.takeIf { it.isAlive }?.interrupt()
		inboundExecutor.shutdown()
		inboundExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)
		outboundThread?.takeIf { it.isAlive }?.interrupt()
		outboundExecutor.shutdown()
		inboundExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)
	}

	@Suppress("EXPERIMENTAL_API_USAGE")
	private fun Byte.toUInt() = toUByte().toInt()

	private fun ByteArray.toIntArray() = map { it.toUInt() }.toIntArray()
}