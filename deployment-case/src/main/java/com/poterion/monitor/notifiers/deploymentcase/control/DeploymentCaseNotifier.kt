package com.poterion.monitor.notifiers.deploymentcase.control

import com.poterion.monitor.api.communication.*
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon
import com.poterion.monitor.notifiers.deploymentcase.api.DeploymentCaseMessageKind
import com.poterion.monitor.notifiers.deploymentcase.api.DeploymentCaseMessageListener
import com.poterion.monitor.notifiers.deploymentcase.data.DeploymentCaseConfig
import com.poterion.monitor.notifiers.deploymentcase.data.State
import com.poterion.monitor.notifiers.deploymentcase.ui.ConfigWindowController
import com.poterion.monitor.notifiers.deploymentcase.ui.StateCompareWindowController
import dorkbox.systemTray.MenuItem
import javafx.application.Platform
import javafx.scene.Parent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.FileWriter

/**
 * Deployment case notifier.
 *
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class DeploymentCaseNotifier(override val controller: ControllerInterface, config: DeploymentCaseConfig) :
		Notifier<DeploymentCaseConfig>(config), BluetoothEmbeddedListener {

	companion object {
		private val LOGGER: Logger = LoggerFactory.getLogger(DeploymentCaseNotifier::class.java)
	}

	/** Bluetooth communicator */
	val communicator: BluetoothCommunicatorEmbedded = BluetoothCommunicatorEmbedded(config.deviceAddress, config.enabled)
	private val listeners = mutableListOf<DeploymentCaseMessageListener>()

	private var stateMachineBuffer: ByteArray = ByteArray(32_768)

	override val icon: Icon = DeploymentCaseIcon.NUCLEAR_FOOTBALL
	private val connectedIcon: Icon
		get() = if (communicator.isConnected)
			DeploymentCaseIcon.CONNECTED else DeploymentCaseIcon.DISCONNECTED

	override val navigationRoot: NavigationItem
		get() = super.navigationRoot.apply {
			sub?.add(NavigationItem(
					title = "Reconnect",
					icon = connectedIcon,
					action = { communicator.connect() },
					update = { entry, _ ->
						(entry as? MenuItem)?.enabled = config.enabled
						connectedIcon.inputStream.use { (entry as? MenuItem)?.setImage(it) }
					}
			))
		}

	override val configurationTab: Parent?
		get() = ConfigWindowController.getRoot(config, this)

	override fun initialize() {
		super.initialize()
		communicator.register(this)
	}

	override fun execute(action: NotifierAction) {
		when (action) {
			NotifierAction.ENABLE -> {
				config.enabled = true
				communicator.shouldConnect = true
				communicator.connect()
				controller.saveConfig()
			}
			NotifierAction.DISABLE -> {
				config.enabled = false
				communicator.shouldConnect = false
				communicator.disconnect()
				controller.saveConfig()
			}
			NotifierAction.TOGGLE -> execute(if (config.enabled) NotifierAction.DISABLE else NotifierAction.ENABLE)
			NotifierAction.SHUTDOWN -> communicator.disconnect()
		}
	}

	/**
	 * Register deployment case message listener.
	 *
	 * @param listener Listener to register
	 */
	fun register(listener: DeploymentCaseMessageListener) = listeners.add(listener)

	override fun onConnect() {
		super.onConnect()
		controller.check(true)
		controller.triggerUpdate()
	}

	override fun onDisconnect() {
		super.onDisconnect()
		controller.check(true)
		controller.triggerUpdate()
	}

	override fun onMessage(message: ByteArray) {
		super.onMessage(message)
		//val chksumReceived = message[0].toInt() and 0xFF
		val kind: MessageKind = message[1]
				.let { byte -> DeploymentCaseMessageKind.values().find { it.byteCode == byte } }
				?: BluetoothMessageKind.UNKNOWN

		when (kind) {
			DeploymentCaseMessageKind.CONFIGURATION -> {
				val receivedChecksum = message[3].toInt() and 0xFF
				val calculatedChecksum = config.configurations
						.find { it.isActive }
						?.stateMachine
						?.toData()
						?.toByteArray()
						?.calculateChecksum()
				if (calculatedChecksum == null) { // No state machine selected
					config.configurations
							.find { it.stateMachine.toData().toByteArray().calculateChecksum() == receivedChecksum }
							?.isActive = true
					controller.saveConfig()
				} else if (receivedChecksum != calculatedChecksum) { // Selected state machine does not match
					config.configurations
							.find { it.isActive }
							?.stateMachine
							?.also { sendStateMachine(it) }
				}
			}
			DeploymentCaseMessageKind.PUSH_STATE_MACHINE -> {
				val length = ((message[2].toInt() shl 8) and 0xFF00) or (message[3].toInt() and 0xFF)
				val addr = ((message[4].toInt() shl 8) and 0xFF00) or (message[5].toInt() and 0xFF)
				if (addr == 0) (0 until stateMachineBuffer.size)
						.forEach { stateMachineBuffer[it] = 0xFF.toByte() }

				(0 until (message.size - 6)).forEach {
					stateMachineBuffer[it + addr] = message[it + 6]
				}

				listeners.forEach { Platform.runLater { it.onProgress(addr + message.size - 6, length, true) } }
				LOGGER.debug("State Machine address: 0x%02X - 0x%02X, size: %d bytes, transfered: %d bytes, total: %d bytes"
						.format(addr, addr + message.size - 7, message.size - 6, addr + message.size - 6, length))

				if (addr + message.size - 6 == length) Platform.runLater {
					(config.configurations.find { it.isActive } ?: config.configurations.firstOrNull())?.also { conf ->
						StateCompareWindowController.popup(
								conf.stateMachine,
								stateMachineBuffer.toIntList().toStateMachine(conf.devices, conf.variables))
					}
				}

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
			DeploymentCaseMessageKind.SET_STATE -> {
				val configuration = config.configurations.find { it.isActive }
				val devices = configuration?.devices ?: emptyList()
				val variables = configuration?.variables ?: emptyList()
				message.copyOfRange(2, message.size)
						.toIntList()
						.toActions(devices, variables)
						.forEach { action -> listeners.forEach { Platform.runLater { it.onAction(action) } } }
			}
			else -> LOGGER.warn("Unknown BT message [${"0x%02X".format(kind.code)}]:" +
					" ${message.joinToString(" ") { "0x%02".format(it) }}")
		}
	}

	internal fun sendStateMachine(stateMachine: List<State>) = stateMachine
			.toData()
			.toByteArray()
			.toList()
			.chunked(61)
			.map { data -> data.toTypedArray() }
			.mapIndexed { index, data -> (index * 61) to data }
			.map { (reg, data) -> listOf(Math.floor(reg / 256.0).toByte(), (reg % 256).toByte(), *data) }
			.map { it.toByteArray() }
			.forEach { communicator.send(DeploymentCaseMessageKind.PUSH_STATE_MACHINE, it) }
}