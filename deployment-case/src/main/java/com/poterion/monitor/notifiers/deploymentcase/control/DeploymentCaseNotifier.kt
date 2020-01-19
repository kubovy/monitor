package com.poterion.monitor.notifiers.deploymentcase.control

import com.poterion.communication.serial.*
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseModule
import com.poterion.monitor.notifiers.deploymentcase.api.DeploymentCaseMessageListener
import com.poterion.monitor.notifiers.deploymentcase.data.*
import com.poterion.monitor.notifiers.deploymentcase.ui.ConfigWindowController
import com.poterion.monitor.notifiers.deploymentcase.ui.StateCompareWindowController
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.control.TextField
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.FileWriter
import java.nio.charset.Charset

/**
 * Deployment case notifier.
 *
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class DeploymentCaseNotifier(override val controller: ControllerInterface, config: DeploymentCaseConfig) :
		Notifier<DeploymentCaseConfig>(config), CommunicatorListener {

	companion object {
		private val LOGGER: Logger = LoggerFactory.getLogger(DeploymentCaseNotifier::class.java)
		private val BUTTON_DOWNLOAD = ButtonType("Download")
		private val BUTTON_UPLOAD = ButtonType("Upload")
		private val BUTTON_REPAIR = ButtonType("Repair")
		// Chunk contains 2 bytes SM size, 2 bytes address and data
		private val lcdCache = mutableListOf("", "", "", "")
	}

	override val definition: Module<DeploymentCaseConfig, ModuleInstanceInterface<DeploymentCaseConfig>> = DeploymentCaseModule

	/** Bluetooth communicator */
	val bluetoothCommunicator: BluetoothCommunicator = BluetoothCommunicator()
	private val listeners = mutableListOf<DeploymentCaseMessageListener>()

	private var stateMachineTransfer = false
	private var stateMachineChunks = 0
	private var stateMachineBuffer: ByteArray = ByteArray(65_537)
	private var repairStateMachine = false
	private var lastStateMachineConfigurationCheck = 0L

	private val connectedIcon: Icon
		get() = if (bluetoothCommunicator.isConnected)
			DeploymentCaseIcon.CONNECTED else DeploymentCaseIcon.DISCONNECTED

	override val navigationRoot: NavigationItem
		get() = super.navigationRoot.apply {
			sub?.add(NavigationItem(
					title = "Reconnect",
					icon = connectedIcon,
					action = {
						bluetoothCommunicator.connect(BluetoothCommunicator.Descriptor(config.deviceAddress, 6))
					}))
		}

	override val configurationRows: List<Pair<Node, Node>>?
		get() = listOf(
				Label("Bluetooth Address").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to TextField(config.deviceAddress).apply {
					textProperty().addListener { _, _, address -> config.deviceAddress = address }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				})

	override var configurationTab: Parent? = null
		get() {
			field = field ?: ConfigWindowController.getRoot(config, this)
			return field
		}
		private set

	override fun initialize() {
		super.initialize()
		bluetoothCommunicator.register(this)
		if (config.enabled) {
			bluetoothCommunicator.connect(BluetoothCommunicator.Descriptor(config.deviceAddress, 6))
		}
	}

	override fun execute(action: NotifierAction) {
		when (action) {
			NotifierAction.ENABLE -> {
				config.enabled = true
				bluetoothCommunicator.connect(BluetoothCommunicator.Descriptor(config.deviceAddress, 6))
				controller.saveConfig()
			}
			NotifierAction.DISABLE -> {
				config.enabled = false
				bluetoothCommunicator.disconnect()
				controller.saveConfig()
			}
			NotifierAction.TOGGLE -> execute(if (config.enabled) NotifierAction.DISABLE else NotifierAction.ENABLE)
			NotifierAction.SHUTDOWN -> bluetoothCommunicator.disconnect()
		}
	}

	/**
	 * Register deployment case message listener.
	 *
	 * @param listener Listener to register
	 */
	fun register(listener: DeploymentCaseMessageListener) = listeners.add(listener)

	override fun onConnecting(channel: Channel) {
	}

	override fun onConnect(channel: Channel) {
		if (channel == Channel.BLUETOOTH) {
			LOGGER.info("${channel} Connected")
			listeners.forEach { Platform.runLater { it.onProgress(-1, 1, false) } }
			bluetoothCommunicator.send(MessageKind.SM_CONFIGURATION)
		}
	}

	override fun onDisconnect(channel: Channel) {
		if (channel == Channel.BLUETOOTH) {
			LOGGER.info("${channel} Disconnected")
			stateMachineTransfer = false
		}
	}

	override fun onMessageReceived(channel: Channel, message: IntArray) {
		//val chksumReceived = message[0].toInt() and 0xFF
		val kind: MessageKind = message[1]
				.let { kind -> MessageKind.values().find { it.code == kind } }
				?: MessageKind.UNKNOWN

		when (kind) {
			MessageKind.LCD -> if (message.size > 3) {
				//val num = message[2]
				//val backlight = message[3]
				val line = message[4]
				val length = message[5]
				lcdCache[line] = (0 until length)
						.map { message[6 + it].toByte() }
						.toByteArray()
						.toString(Charset.defaultCharset())
				val device = Device(kind = DeviceKind.LCD, key = "${LcdKey.MESSAGE.key}")
				val value = lcdCache.joinToString("\n")
				Platform.runLater { listeners.forEach { it.onAction(device, value) } }
			}
			MessageKind.MCP23017 -> {
				if (message.size == 5) {
					val address = message[2]
					message.toList()
							.subList(3, 5)
							.mapIndexed { index, byte -> (index * 8) to byte2Bools(byte) }
							.flatMap { (offset, bools) -> bools.mapIndexed { i, b -> (offset + i) to b } }
							.map { (i, b) -> ((address - 0x20) * 16 + i) to (if (b) "true" else "false") }
							.map { (key, value) -> Device(kind = DeviceKind.MCP23017, key = "${key}") to value }
							.forEach { (d, v) -> Platform.runLater { listeners.forEach { it.onAction(d, v) } } }
				}
			}
			MessageKind.WS281x -> {
				// val num = message[2]
				// val count = message[3]
				val index = message[4]
				val pattern = message[5]
				val red = message[6]
				val green = message[7]
				val blue = message[8]
				val delay = message[9] * 256 + message[10]
				val min = message[11]
				val max = message[12]
				(index to listOf(pattern, red, green, blue, delay, min, max))
						.let { (index, data) -> index to data.joinToString(",") }
						.let { (key, value) -> Device(kind = DeviceKind.WS281x, key = "${key}") to value }
						.let { (device, value) -> Platform.runLater { listeners.forEach { it.onAction(device, value) } } }
			}
			MessageKind.SM_CONFIGURATION -> {
				val receivedChecksum = message[2] and 0xFF
				val calculatedChecksum = config.configurations
						.find { it.isActive }
						?.toData()
						?.toByteArray()
						?.calculateChecksum()
				LOGGER.info("SM CHKSUM: received=0x%02X, calculated=0x%02X".format(receivedChecksum, calculatedChecksum))
				listeners.forEach { Platform.runLater { it.onProgress(0, 0, true) } }
				if (calculatedChecksum == null) { // No state machine selected
					val activeConfig = config.configurations
							.find { it.toData().toByteArray().calculateChecksum() == receivedChecksum }
					activeConfig?.isActive = true
					controller.saveConfig()
					listeners.forEach { Platform.runLater { it.onVerification(activeConfig != null) } }
				} else if (receivedChecksum != calculatedChecksum) { // Selected state machine does not match
					listeners.forEach { Platform.runLater { it.onVerification(false) } }
					if (!stateMachineTransfer && System.currentTimeMillis() - lastStateMachineConfigurationCheck > 30_000L) {
						lastStateMachineConfigurationCheck = System.currentTimeMillis()
						Platform.runLater {
							config.configurations.find { it.isActive }?.also { conf ->
								Alert(Alert.AlertType.CONFIRMATION).apply {
									title = "Wrong State Machine"
									headerText = "State machine configuration does not match deployment football's ones!"
									contentText = ("Do you want to upload the \"%s\" state machine to the deployment" +
											" football?").format(conf.name)
									//contentText = "Do you want to download the state machine from the deployment football or upload it there?"
									//buttonTypes.setAll(BUTTON_DOWNLOAD, BUTTON_UPLOAD, BUTTON_REPAIR, ButtonType.CANCEL)
									buttonTypes.setAll(BUTTON_UPLOAD, ButtonType.CANCEL)
								}.showAndWait().ifPresent { button ->
									when (button) {
										BUTTON_DOWNLOAD -> pullStateMachine(repair = false)
										BUTTON_UPLOAD -> pushStateMachine()
										BUTTON_REPAIR -> pullStateMachine(repair = true)
									}
								}
							}
						}
					}
				} else listeners.forEach { Platform.runLater { it.onVerification(true) } }
			}
			MessageKind.SM_PUSH -> {
				val length = ((message[2] shl 8) and 0xFF00) or (message[3] and 0xFF)
				val addr = ((message[4] shl 8) and 0xFF00) or (message[5] and 0xFF)
				if (addr == 0) stateMachineBuffer.indices.forEach { stateMachineBuffer[it] = 0xFF.toByte() }

				(0 until (message.size - 6)).forEach {
					stateMachineBuffer[it + addr] = message[it + 6].toByte()
				}

				listeners.forEach { Platform.runLater { it.onProgress(addr + message.size - 6, length, true) } }
				LOGGER.debug("State Machine address: 0x%02X - 0x%02X, size: %d bytes, transfered: %d bytes, total: %d bytes"
						.format(addr, addr + message.size - 7, message.size - 6, addr + message.size - 6, length))

				if (addr + message.size - 6 == length) {
					stateMachineTransfer = false
					val conf = (config.configurations.find { it.isActive } ?: config.configurations.firstOrNull())
					val currentStateMachine = conf?.toData()?.toByteArray()
					val matches = currentStateMachine?.contentEquals(stateMachineBuffer) ?: false

					if (!matches && repairStateMachine) {
						repairStateMachine = false
						stateMachineChunks = 0
						if (currentStateMachine != null && currentStateMachine.isNotEmpty()) currentStateMachine
								.indices
								.map { Triple(it, currentStateMachine[it], stateMachineBuffer.getOrNull(it)) }
								.filter { (_, current, received) -> current != received }
								//.intermediate { (reg, current, received) -> LOGGER.debug("%04X: 0x%02X != 0x%02X".format(reg, current, received)) }
								.map { (reg, current, _) -> reg to current }
								.let { diff ->
									val map = mutableMapOf<Int, ByteArray>()
									val data = mutableListOf<Byte>()
									var start = 0
									var seq = 0
									diff.forEach { (reg, byte) ->
										if (reg - seq != start) {
											if (data.isNotEmpty()) map[start] = data.toByteArray()
											start = reg
											seq = 0
										}
										if (seq == 0) {
											data.clear()
										}
										data.add(byte)
										seq++
									}
									if (start > 0) map[start] = data.toByteArray()
									map
								}
								//.intermediate { (addr, bytes) -> LOGGER.debug("%04X: ${bytes.joinToString(" ") { "0x%02X".format(it) }}".format(addr)) }
								.map { (addr, bytes) -> addr to bytes.toList() }
								.map { (addr, bytes) ->
									bytes.chunked(channel.maxPacketSize)
											.also { stateMachineChunks += it.size }
											.mapIndexed { index, data -> (index * channel.maxPacketSize) to data }
											.map { (shift, data) -> (currentStateMachine.size.to2Byte() + (addr + shift).to2Byte() + data) }
											.map { it.toByteArray() }
								}
								.flatten()
								.let { chunks ->
									// Add last byte as last chunk
									chunks + listOf((currentStateMachine.size.to2ByteArray()
											+ (currentStateMachine.size - 1).to2ByteArray()
											+ listOf(currentStateMachine.last()).toByteArray()))
								}
								//.intermediate { chunk -> LOGGER.debug("CHUNK: ${chunk.joinToString(" ") { "0x%02X".format(it) }}") }
								.takeIf { it.isNotEmpty() }
								?.also {
									stateMachineTransfer = true
									listeners.forEach { Platform.runLater { it.onProgress(-1, 1, true) } }
								}
								?.forEach { bluetoothCommunicator.send(MessageKind.SM_PUSH, it) }
					} else Platform.runLater {
						conf?.also {
							StateCompareWindowController.popup(
									it.stateMachine,
									stateMachineBuffer.toIntList().toStateMachine(it.stateMachine, it.devices, it.variables))
						}
					}
				}

				if (config.debug) {
					BufferedWriter(FileWriter("sm-in.txt")).use { writer ->
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
					FileOutputStream("sm-in.bin").use { out ->
						out.write(stateMachineBuffer)
					}
				}
			}
			MessageKind.SM_SET_STATE -> {
				val configuration = config.configurations.find { it.isActive }
				val states = configuration?.stateMachine ?: emptyList()
				val devices = configuration?.devices ?: emptyList()
				val variables = configuration?.variables ?: emptyList()
				message.copyOfRange(2, message.size)
						.toList()
						.toActions(states, devices, variables)
						.map { it.device?.toDevice(devices) to it.value }
						.filter { (d, v) -> d != null && v != null }
						.map { (d, v) -> d!! to v!! }
						.forEach { (d, v) -> Platform.runLater { listeners.forEach { it.onAction(d, v) } } }
			}
			MessageKind.SM_INPUT -> {
				val num = message[2]
				val length = message[3]
				val value = (0 until length)
						.map { message[4 + it].toByte() }
						.toByteArray()
						.toString(Charset.defaultCharset())
				val device = Device(kind = DeviceKind.VIRTUAL, key = VirtualKey.ENTER.key)
				Platform.runLater { listeners.forEach { it.onAction(device, "${num}|${value}") } }
			}
			else -> {
			}
		}
	}

	override fun onMessageSent(channel: Channel, message: IntArray, remaining: Int) {
		if (stateMachineChunks > 0) {
			if (remaining == 0 || remaining > stateMachineChunks) stateMachineChunks = remaining
			listeners.forEach { Platform.runLater { it.onProgress(stateMachineChunks - remaining, stateMachineChunks, false) } }
			if (remaining == 0) {
				if (stateMachineTransfer) synchronizeStateMachine()
				stateMachineTransfer = false
				synchronizeStateMachine()
			}
		}
	}

	internal fun synchronizeStateMachine() {
		bluetoothCommunicator
				.takeIf { it.isConnected }
				?.send(MessageKind.SM_CONFIGURATION)
		listeners.forEach { Platform.runLater { it.onProgress(-1, 1, false) } }
	}

	internal fun pullStateMachine(repair: Boolean = false) {
		repairStateMachine = repair
		if (bluetoothCommunicator.isConnected && !stateMachineTransfer) {
			stateMachineTransfer = true
			bluetoothCommunicator.send(MessageKind.SM_PULL)
			listeners.forEach { Platform.runLater { it.onProgress(-1, 1, true) } }
		}
	}

	internal fun pushStateMachine() {
		val channel: Channel = Channel.BLUETOOTH // TODO USB
		config.configurations
				.takeIf { bluetoothCommunicator.isConnected && !stateMachineTransfer }
				?.find { it.isActive }
				?.toData()
				?.toByteArray()
				?.toList()
				?.also { bytes ->
					if (config.debug) FileOutputStream("sm-out.bin").use { out -> out.write(bytes.toByteArray()) }
					bytes.chunked(channel.maxPacketSize)
							.also { stateMachineChunks = it.size }
							.mapIndexed { index, data -> (index * channel.maxPacketSize) to data }
							.map { (reg, data) -> bytes.size.to2Byte() + reg.to2Byte() + data }
							.map { it.toByteArray() }
							.also { chunks ->
								if (config.debug) BufferedWriter(FileWriter("sm-out.txt")).use { writer ->
									var chars = ""
									chunks.forEachIndexed { chunk, bytes ->
										bytes.copyOfRange(4, bytes.size).forEachIndexed { x, b ->
											val i = chunk * channel.maxPacketSize + x
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
							}
							.takeIf { it.isNotEmpty() }
							?.also {
								stateMachineTransfer = true
								listeners.forEach { Platform.runLater { it.onProgress(-1, 1, false) } }
							}
							?.forEach { bluetoothCommunicator.send(MessageKind.SM_PUSH, it) }
							?.also { bluetoothCommunicator.send(MessageKind.SM_CONFIGURATION) }
				}
	}
}
