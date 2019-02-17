package com.poterion.monitor.notifiers.deploymentcase.control

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.poterion.monitor.api.communication.BluetoothCommunicatorEmbedded
import com.poterion.monitor.api.communication.BluetoothEmbeddedListener
import com.poterion.monitor.api.communication.BluetoothMessageKind
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon
import com.poterion.monitor.notifiers.deploymentcase.data.DeploymentCaseConfig
import com.poterion.monitor.notifiers.deploymentcase.ui.ConfigWindowController
import com.poterion.monitor.notifiers.deploymentcase.ui.StateCompareWindowController
import dorkbox.systemTray.MenuItem
import javafx.scene.Parent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicBoolean

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

	val communicator: BluetoothCommunicatorEmbedded = BluetoothCommunicatorEmbedded(config.deviceAddress, config.enabled)

	private val objectMapper = ObjectMapper()
	private var isRunning = AtomicBoolean(false)

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

	override fun onMessage(kind: BluetoothMessageKind, message: ByteArray) {
		super.onMessage(kind, message)
		val data = message.map { it.toUByte().toInt() }

		when(kind) {
			BluetoothMessageKind.PUSH_STATE_MACHINE -> {
				val configuration = config.configurations[0]
				StateCompareWindowController.popup(
						configuration.stateMachine,
						data.toStateMachine(configuration.devices, configuration.variables))
			}
			BluetoothMessageKind.VALUE_CHANGED -> {
				val device = data[0].toDevice()
				val variable = data.subList(1, data.size).toVariable(device)
				LOGGER.info("${device}: ${variable}")
			}
			BluetoothMessageKind.CURRENT_STATE -> {
				//data.subList(1, data[0]).
			}
			else -> LOGGER.warn("Unknown BT message [${"0x%02X".format(kind.code)}]:" +
					" ${message.map { "0x%02".format(it) }.joinToString(" ")}")
		}

//		try {
//			val data = objectMapper.readTree(message)
//			config.configurations // selects a message,
//					.find { data.has(it.name) } // which name was provided as a key
//					?.takeUnless { isRunning.getAndSet(true) }
//					?.let { it to data.get(it.name) } // and use the key's data
//					?.also { (configuration, variables) ->
//						val context = variables // create context map from the data
//								.fields()
//								.asSequence()
//								.map { (k, v) -> URLEncoder.encode(k, "UTF-8") to v.getValue() }
//								.toMap()
//
//						val task = DeploymentTask(configuration, context,
//								{ states ->
//									states.takeIf { it.isNotEmpty() }
//											?.joinToString(";") { (name, status) -> "state,${name},${status}" }
//											?.also { message -> Platform.runLater { communicator.send(message) } }
//								},
//								{ isRunning.set(false) })
//						Thread(task).start()
//					}
//		} catch (e: IOException) {
//			LOGGER.error(e.message, e)
//		}
	}

	private fun JsonNode.getValue(): Any? = when {
		isBoolean -> asBoolean(false)
		isInt -> asInt(0)
		isLong -> asLong(0L)
		isDouble -> asDouble(0.0)
		isFloat -> asDouble(0.0)
		isShort -> asInt(0)
		isTextual -> URLEncoder.encode(asText(""), "UTF-8")
		isObject -> toString() // TODO JK
		else -> null
	}
}