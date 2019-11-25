package com.poterion.monitor.notifiers.raspi.ws281x.control

import com.fasterxml.jackson.databind.ObjectMapper
import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.communication.BluetoothCommunicatorRaspi
import com.poterion.monitor.api.communication.BluetoothRaspiListener
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.CommonIcon
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.notifiers.raspi.ws281x.RaspiWS281xIcon
import com.poterion.monitor.notifiers.raspi.ws281x.RaspiWS281xModule
import com.poterion.monitor.notifiers.raspi.ws281x.data.LightConfig
import com.poterion.monitor.notifiers.raspi.ws281x.data.RaspiWS281xConfig
import com.poterion.monitor.notifiers.raspi.ws281x.ui.ConfigWindowController
import dorkbox.systemTray.MenuItem
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.control.TextField
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class RaspiWS281xNotifier(override val controller: ControllerInterface, config: RaspiWS281xConfig) :
		Notifier<RaspiWS281xConfig>(config), BluetoothRaspiListener {

	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(RaspiWS281xNotifier::class.java)
	}

	override val definition: Module<RaspiWS281xConfig, ModuleInstanceInterface<RaspiWS281xConfig>> = RaspiWS281xModule
	val communicator: BluetoothCommunicatorRaspi = BluetoothCommunicatorRaspi("WS", config.deviceAddress, 5, 6, config.enabled)
	private var lastState = emptyList<LightConfig>()
	private val objectMapper = ObjectMapper()
	private val connectedIcon: Icon
		get() = if (communicator.isInboundConnected && communicator.isOutboundConnected)
			RaspiWS281xIcon.CONNECTED else RaspiWS281xIcon.DISCONNECTED

	override val navigationRoot: NavigationItem
		get() = super.navigationRoot.apply {
			sub?.add(NavigationItem(
					title = "Reconnect",
					icon = connectedIcon,
					action = { communicator.connect() },
					update = { entry, _ ->
						//(entry as? MenuItem)?.enabled = config.enabled
						connectedIcon.inputStream.use { (entry as? MenuItem)?.setImage(it) }
					}
			))
			sub?.add(NavigationItem(title = null))

			config.items.sortedBy { it.id }.forEach { itemConfig ->
				val title = itemConfig.id.takeIf { it.isNotEmpty() }
				sub?.add(NavigationItem(
						title = title ?: "Default",
						icon = title?.let { RaspiWS281xIcon.NON_DEFAULT } ?: RaspiWS281xIcon.DEFAULT,
						sub = listOf(
								NavigationItem(title = "None", icon = CommonIcon.NONE) to itemConfig.statusNone,
								NavigationItem(title = "Unknown", icon = CommonIcon.UNKNOWN) to itemConfig.statusUnknown,
								NavigationItem(title = "OK", icon = CommonIcon.OK) to itemConfig.statusOk,
								NavigationItem(title = "Info", icon = CommonIcon.INFO) to itemConfig.statusInfo,
								NavigationItem(title = "Notification", icon = CommonIcon.NOTIFICATION) to itemConfig.statusNotification,
								NavigationItem(title = "Connection Error", icon = CommonIcon.BROKEN_LINK) to itemConfig.statusConnectionError,
								NavigationItem(title = "Service Error", icon = CommonIcon.UNAVAILABLE) to itemConfig.statusServiceError,
								NavigationItem(title = "Warning", icon = CommonIcon.WARNING) to itemConfig.statusWarning,
								NavigationItem(title = "Error", icon = CommonIcon.ERROR) to itemConfig.statusError,
								NavigationItem(title = "Fatal", icon = CommonIcon.FATAL) to itemConfig.statusFatal)
								.map { (item, lights) ->
									item.apply {
										action = {
											execute(NotifierAction.DISABLE)
											changeLights(lights)
											//val message = objectMapper.writeValueAsString(lights)
											//execute(NotifierAction.NOTIFY, message)
										}
									}
								}
								.toMutableList()))
			}
		}

	override val configurationRows: List<Pair<Node, Node>>?
		get() = listOf(Label("Bluetooth Address") to TextField(config.deviceAddress).apply {
			textProperty().addListener { _, _, address -> config.deviceAddress = address }
			focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
		})

	override val configurationTab: Parent?
		get() = ConfigWindowController.getRoot(config, this)

	override fun initialize() {
		StatusCollector.status.sample(10, TimeUnit.SECONDS).subscribe { statusItems ->
			val lights = if (config.combineMultipleServices) {
				val maxStatus = statusItems
						.filter { it.priority >= config.minPriority }
						.maxBy { it.status }
						?.status

				statusItems
						.filter { it.priority >= config.minPriority }
						.filter { it.status == maxStatus }
						.distinctBy { it.serviceName }
						.also { LOGGER.debug("${if (config.enabled) "Changing" else "Skipping"}: ${it}") }
						.mapNotNull { it.toLightConfig() }
						.flatten()
						.takeIf { it.isNotEmpty() }
						?: config.items.firstOrNull { it.id == "" }?.statusOk
			} else {
				statusItems
						.filter { it.priority >= config.minPriority }
						.maxBy { it.status }
						.also { LOGGER.debug("${if (config.enabled) "Changing" else "Skipping"}: ${it}") }
						?.toLightConfig()
						?: config.items.firstOrNull { it.id == "" }?.statusOk
			}

			lights?.also { lastState = it }
					?.takeIf { config.enabled }
					?.also { changeLights(it) }
		}
		communicator.register(this)
	}

	internal fun changeLights(lightConfiguration: List<LightConfig>?) {
		if (lightConfiguration != null) {
			lightConfiguration
					.takeIf { it.isNotEmpty() }
					?.let { objectMapper.writeValueAsString(it) }
					?.takeIf { it.isNotEmpty() }
					?.also { communicator.send(it) }
		}
	}

	override fun execute(action: NotifierAction): Unit = when (action) {
		NotifierAction.ENABLE -> {
			config.enabled = true
			communicator.shouldConnect = true
			communicator.connect()
			changeLights(lastState)
			controller.saveConfig()
		}
		NotifierAction.DISABLE -> {
			config.enabled = false
			communicator.shouldConnect = false
			communicator.disconnect()
			controller.saveConfig()
		}
		NotifierAction.TOGGLE -> execute(if (config.enabled) NotifierAction.DISABLE else NotifierAction.ENABLE)
		NotifierAction.SHUTDOWN -> changeLights(listOf(LightConfig()))
	}

	override fun onInboundConnect() {
		super.onInboundConnect()
		controller.check(true)
		controller.triggerUpdate()
	}

	override fun onInboundDisconnect() {
		super.onInboundDisconnect()
		controller.check(true)
		controller.triggerUpdate()
	}

	override fun onOutboundConnect() {
		super.onOutboundConnect()
		controller.check(true)
		controller.triggerUpdate()
	}

	override fun onOutboundDisconnect() {
		super.onOutboundDisconnect()
		controller.check(true)
		controller.triggerUpdate()
	}

	private fun StatusItem.key() = serviceName

	private fun StatusItem?.toLightConfig(): List<LightConfig>? {
		val lightConfig = config.items
				.map { it.id to it }
				.toMap()
				.let { it[this?.key() ?: ""] ?: it[""] }

		return when (this?.status) {
			Status.NONE, Status.OFF -> lightConfig?.statusNone
			Status.UNKNOWN -> lightConfig?.statusUnknown
			Status.OK -> lightConfig?.statusOk
			Status.INFO -> lightConfig?.statusInfo
			Status.NOTIFICATION -> lightConfig?.statusNotification
			Status.CONNECTION_ERROR -> lightConfig?.statusConnectionError
			Status.SERVICE_ERROR -> lightConfig?.statusServiceError
			Status.WARNING -> lightConfig?.statusWarning
			Status.ERROR -> lightConfig?.statusError
			Status.FATAL -> lightConfig?.statusFatal
			else -> lightConfig?.statusNone
		}
	}
}