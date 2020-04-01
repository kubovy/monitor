/******************************************************************************
 * Copyright (C) 2020 Jan Kubovy (jan@kubovy.eu)                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.      *
 ******************************************************************************/
package com.poterion.monitor.notifiers.deploymentcase.control

import com.poterion.communication.serial.byte2Bools
import com.poterion.communication.serial.calculateChecksum
import com.poterion.communication.serial.communicator.BluetoothCommunicator
import com.poterion.communication.serial.communicator.Channel
import com.poterion.communication.serial.extensions.*
import com.poterion.communication.serial.listeners.*
import com.poterion.communication.serial.payload.DeviceCapabilities
import com.poterion.communication.serial.payload.LcdCommand
import com.poterion.communication.serial.payload.RgbIndicatorConfiguration
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseModule
import com.poterion.monitor.notifiers.deploymentcase.api.DeploymentCaseMessageListener
import com.poterion.monitor.notifiers.deploymentcase.data.*
import com.poterion.monitor.notifiers.deploymentcase.ui.ConfigWindowController
import com.poterion.monitor.notifiers.deploymentcase.ui.StateCompareWindowController
import com.poterion.utils.javafx.Icon
import com.poterion.utils.javafx.mapped
import com.poterion.utils.kotlin.noop
import javafx.application.Platform
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
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

/**
 * Deployment case notifier.
 *
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class DeploymentCaseNotifier(override val controller: ControllerInterface, config: DeploymentCaseConfig) :
		Notifier<DeploymentCaseConfig>(config),
		DataCommunicatorListener, LcdCommunicatorListener,
		RegistryCommunicatorListener, RgbIndicatorCommunicatorListener, StateMachineCommunicatorListener {

	companion object {
		private val LOGGER: Logger = LoggerFactory.getLogger(DeploymentCaseNotifier::class.java)
		private val BUTTON_DOWNLOAD = ButtonType("Download")
		private val BUTTON_UPLOAD = ButtonType("Upload")
		private val BUTTON_REPAIR = ButtonType("Repair")
		private const val SM_DATA_PART = 1

		// Chunk contains 2 bytes SM size, 2 bytes address and data
		private val lcdCache = mutableListOf("", "", "", "")
	}

	override val definition: Module<DeploymentCaseConfig, ModuleInstanceInterface<DeploymentCaseConfig>> = DeploymentCaseModule

	/** Bluetooth communicator */
	val bluetoothCommunicator: BluetoothCommunicator = BluetoothCommunicator()
	private val dataCommunicator = DataCommunicatorExtension(bluetoothCommunicator)
	val lcdCommunicator = LcdCommunicatorExtension(bluetoothCommunicator)
	private val registryCommunicator = RegistryCommunicatorExtension(bluetoothCommunicator)
	private val rgbIndicatorCommunicator = RgbIndicatorCommunicatorExtension(bluetoothCommunicator)
	val stateMachineCommunicator = StateMachineCommunicatorExtension(bluetoothCommunicator)

	private val listeners = mutableListOf<DeploymentCaseMessageListener>()

	private var stateMachineTransfer = false
	private var stateMachineChunks = 0
	private var stateMachineBuffer: ByteArray = ByteArray(65_537)
	private var repairStateMachine = false
	private var lastStateMachineConfigurationCheck = 0L

	private val connectedProperty: BooleanProperty = SimpleBooleanProperty(bluetoothCommunicator.isConnected)

	private var _navigationRoot: NavigationItem? = null
	override val navigationRoot: NavigationItem
		get() = _navigationRoot ?: NavigationItem(
				uuid = config.uuid,
				titleProperty = config.nameProperty,
				icon = definition.icon,
				sub = listOf(
						NavigationItem(
								title = "Enabled",
								checkedProperty = config.enabledProperty.asObject()),
						NavigationItem(
								titleProperty = SimpleStringProperty().also { property ->
									property.bind(connectedProperty.asObject()
											.mapped { if (it == true) "Disconnect" else "Connect" })
								},
								checkedProperty = connectedProperty.asObject(),
								iconProperty = SimpleObjectProperty<Icon?>().also { property ->
									property.bind(connectedProperty.asObject().mapped {
										if (it == true) DeploymentCaseIcon.CONNECTED else DeploymentCaseIcon.DISCONNECTED
									})
								},
								action = {
									if (connectedProperty.get()) bluetoothCommunicator.disconnect()
									else bluetoothCommunicator
											.connect(BluetoothCommunicator.Descriptor(config.deviceAddress, 6))
								})
				))
				.also { _navigationRoot = it }


	override val configurationRows: List<Pair<Node, Node>>
		get() = super.configurationRows + listOf(
				Label("Bluetooth Address").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to TextField(config.deviceAddress).apply {
					textProperty().bindBidirectional(config.deviceAddressProperty)
					focusedProperty().addListener { _, _, focused -> if (!focused) controller.saveConfig() }
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
		config.enabledProperty.addListener { _, _, enabled ->
			if (enabled) bluetoothCommunicator.connect(BluetoothCommunicator.Descriptor(config.deviceAddress, 6))
			else bluetoothCommunicator.disconnect()
		}
	}

	override fun update() = noop()

	override fun shutdown() = bluetoothCommunicator.disconnect()

	/**
	 * Register deployment case message listener.
	 *
	 * @param listener Listener to register
	 */
	fun register(listener: DeploymentCaseMessageListener) = listeners.add(listener)

	override fun onConnecting(channel: Channel) {
		if (channel == Channel.BLUETOOTH) {
			connectedProperty.set(false)
		}
	}

	override fun onConnect(channel: Channel) = Platform.runLater {
		if (channel == Channel.BLUETOOTH) {
			LOGGER.info("${channel} Connected")
			connectedProperty.set(true)
			listeners.forEach { it.onProgress(-1, 1, false) }
		}
	}

	override fun onConnectionReady(channel: Channel) {
		dataCommunicator.sendConsistencyCheckRequest(SM_DATA_PART)
	}

	override fun onDisconnect(channel: Channel) = Platform.runLater {
		if (channel == Channel.BLUETOOTH) {
			LOGGER.info("${channel} Disconnected")
			connectedProperty.set(false)
			stateMachineTransfer = false
		}
	}

	override fun onMessageReceived(channel: Channel, message: IntArray) = noop()

	override fun onMessagePrepare(channel: Channel) = noop()

	override fun onMessageSent(channel: Channel, message: IntArray, remaining: Int) = Platform.runLater {
		if (stateMachineChunks > 0) {
			if (remaining == 0 || remaining > stateMachineChunks) stateMachineChunks = remaining
			listeners.forEach { it.onProgress(stateMachineChunks - remaining, stateMachineChunks, false) }
			if (remaining == 0) {
				if (stateMachineTransfer) synchronizeStateMachine()
				stateMachineTransfer = false
				synchronizeStateMachine()
			}
		}
	}

	override fun onDeviceCapabilitiesChanged(channel: Channel, capabilities: DeviceCapabilities) = noop()

	override fun onDeviceNameChanged(channel: Channel, name: String) = noop()

	override fun onConsistencyCheckReceived(channel: Channel, part: Int, checksum: Int) = Platform.runLater {
		if (part == SM_DATA_PART) {
			val calculatedChecksum = config.configurations
					.find { it.isActive }
					?.toData()
					?.toByteArray()
					?.calculateChecksum()
			LOGGER.info("SM CHKSUM: received=0x%02X, calculated=0x%02X".format(checksum, calculatedChecksum))
			listeners.forEach { it.onProgress(0, 0, true) }
			if (calculatedChecksum == null) { // No state machine selected
				val activeConfig = config.configurations
						.find { it.toData().toByteArray().calculateChecksum() == checksum }
				activeConfig?.isActive = true
				controller.saveConfig()
				listeners.forEach { it.onVerification(activeConfig != null) }
			} else if (checksum != calculatedChecksum) { // Selected state machine does not match
				listeners.forEach { it.onVerification(false) }
				if (!stateMachineTransfer && System.currentTimeMillis() - lastStateMachineConfigurationCheck > 30_000L) {
					lastStateMachineConfigurationCheck = System.currentTimeMillis()
					config.configurations.find { it.isActive }?.also { conf ->
						Alert(Alert.AlertType.CONFIRMATION).apply {
							title = "Wrong State Machine"
							headerText = "State machine configuration does not match deployment football's ones!"
							contentText = "Do you want to upload the \"%s\" state machine to the deployment football?"
									.format(conf.name)
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
			} else listeners.forEach { it.onVerification(true) }
		}
	}

	override fun onDataReceived(channel: Channel, part: Int, address: Int, length: Int, data: IntArray) =
			Platform.runLater {
				if (part == SM_DATA_PART) {
					if (address == 0) stateMachineBuffer.indices.forEach { stateMachineBuffer[it] = 0xFF.toByte() }

					(0 until (data.size)).forEach {
						stateMachineBuffer[it + address] = data[it].toByte()
					}

					listeners.forEach { it.onProgress(address + data.size, length, true) }
					LOGGER.debug("State Machine address: 0x%02X - 0x%02X, size: %d bytes, transferred: %d bytes, total: %d bytes"
							.format(address, address + data.size - 1, data.size, address + data.size, length))

					if (address + data.size == length) {
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
										val buffer = mutableListOf<Byte>()
										var start = 0
										var seq = 0
										diff.forEach { (reg, byte) ->
											if (reg - seq != start) {
												if (buffer.isNotEmpty()) map[start] = buffer.toByteArray()
												start = reg
												seq = 0
											}
											if (seq == 0) {
												buffer.clear()
											}
											buffer.add(byte)
											seq++
										}
										if (start > 0) map[start] = buffer.toByteArray()
										map
									}
									//.intermediate { (address, bytes) -> LOGGER.debug("%04X: ${bytes.joinToString(" ") { "0x%02X".format(it) }}".format(address)) }
									.map { (address, bytes) -> address to bytes.toList() }
									.map { (address, bytes) ->
										bytes.chunked(channel.maxPacketSize)
												.also { stateMachineChunks += it.size }
												.mapIndexed { index, data -> (index * channel.maxPacketSize) to data }
												.map { (shift, data) -> (currentStateMachine.size.to2Byte() + (address + shift).to2Byte() + data) }
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
										listeners.forEach { it.onProgress(-1, 1, true) }
									}
									?.forEach { dataCommunicator.sendData(SM_DATA_PART, it) }
						} else {
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
			}

	override fun onLcdCountReceived(channel: Channel, count: Int) = noop()

	override fun onLcdCommandReceived(channel: Channel, num: Int, command: LcdCommand) {
		LOGGER.debug("LCD[${num}] Command: ${command}")
	}

	override fun onLcdContentChanged(channel: Channel, num: Int, backlight: Boolean, line: Int, content: String) =
			Platform.runLater {
				if (num == 0) { // only one LCD
					lcdCache[line] = content
					val device = Device(kind = DeviceKind.LCD, key = "${LcdKey.MESSAGE.key}")
					val value = lcdCache.joinToString("\n")
					listeners.forEach { it.onAction(device, value) }
				}
			}

	override fun onRegistryValue(channel: Channel, address: Int, registry: Int, vararg values: Int) =
			Platform.runLater {
				values
						.mapIndexed { index, byte -> (index * 8) to byte2Bools(byte) }
						.flatMap { (offset, bools) -> bools.mapIndexed { i, b -> (offset + i) to b } }
						.map { (i, b) -> ((address - 0x20) * 16 + i) to (if (b) "true" else "false") }
						.map { (key, value) -> Device(kind = DeviceKind.MCP23017, key = "${key}") to value }
						.forEach { (d, v) -> listeners.forEach { it.onAction(d, v) } }
			}

	override fun onRgbIndicatorCountChanged(channel: Channel, count: Int) {
		LOGGER.debug("RGB Indicator count: ${count}")
	}

	override fun onRgbIndicatorConfiguration(channel: Channel, num: Int, count: Int, index: Int,
											 configuration: RgbIndicatorConfiguration) = Platform.runLater {
		if (num == 0) { // only one indicator
			val device = Device(kind = DeviceKind.WS281x, key = "${index}")
			val value = listOf(configuration.pattern.code, configuration.color.red, configuration.color.green,
					configuration.color.blue, configuration.delay, configuration.minimum, configuration.maximum)
					.joinToString(",")
			listeners.forEach { it.onAction(device, value) }
		}
	}

	override fun onStateMachineActionReceived(channel: Channel, actions: List<Pair<Int, IntArray>>) =
			Platform.runLater {
				val configuration = config.configurations.find { it.isActive }
				val states: List<State> = configuration?.stateMachine ?: emptyList()
				val devices: List<Device> = configuration?.devices ?: emptyList()
				val variables: List<Variable> = configuration?.variables ?: emptyList()
				actions.asSequence()
						.map { (device, v) -> Triple((device and 0x80) == 0x80, (device and 0x7F).toDevice(devices), v) }
						.map { (e, d, v) -> Triple(e, d, v.toList().toVariableWhole(states, d, variables)) }
						.map { (e, d, v) -> Action(d.toData(), v.name, e) }
						.map { it.device?.toDevice(devices) to it.value }
						.filter { (d, v) -> d != null && v != null }
						.map { (d, v) -> d!! to v!! }
						.forEach { (d, v) -> listeners.forEach { it.onAction(d, v) } }
				//message.copyOfRange(2, message.size)
				//	.toList()
				//	.toActions(states, devices, variables)
				//	.map { it.device?.toDevice(devices) to it.value }
				//	.filter { (d, v) -> d != null && v != null }
				//	.map { (d, v) -> d!! to v!! }
				//	.forEach { (d, v) -> listeners.forEach { it.onAction(d, v) } }
			}

	override fun onStateMachineInputReceived(channel: Channel, num: Int, value: String) = Platform.runLater {
		val device = Device(kind = DeviceKind.VIRTUAL, key = VirtualKey.ENTER.key)
		listeners.forEach { it.onAction(device, "${num}|${value}") }
	}

	internal fun synchronizeStateMachine() {
		dataCommunicator.sendConsistencyCheckRequest(SM_DATA_PART)
		listeners.forEach { it.onProgress(-1, 1, false) }
	}

	internal fun pullStateMachine(repair: Boolean = false) {
		repairStateMachine = repair
		if (bluetoothCommunicator.isConnected && !stateMachineTransfer) {
			stateMachineTransfer = true
			dataCommunicator.sendDataRequest(SM_DATA_PART)
			listeners.forEach { it.onProgress(-1, 1, true) }
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
							.map { (reg, data) -> reg.to2Byte() + bytes.size.to2Byte() + data }
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
								listeners.forEach { it.onProgress(-1, 1, false) }
							}
							?.forEach { dataCommunicator.sendData(SM_DATA_PART, it) }
							?.also { dataCommunicator.sendConsistencyCheckRequest(SM_DATA_PART) }
				}
	}
}
