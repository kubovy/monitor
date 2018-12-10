package com.poterion.monitor.notifiers.raspiw2812.control

import com.fasterxml.jackson.databind.ObjectMapper
import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.communication.BluetoothCommunicator
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.ui.CommonIcon
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.notifiers.raspiw2812.RaspiW2812Icon
import com.poterion.monitor.notifiers.raspiw2812.data.LightConfig
import com.poterion.monitor.notifiers.raspiw2812.data.RaspiW2812Config
import com.poterion.monitor.notifiers.raspiw2812.ui.ConfigWindowController
import dorkbox.systemTray.MenuItem
import javafx.scene.Parent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class RaspiW2812Notifier(override val controller: ControllerInterface, config: RaspiW2812Config) : Notifier<RaspiW2812Config>(config) {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(RaspiW2812Notifier::class.java)
	}

	var communicator: BluetoothCommunicator = BluetoothCommunicator("WS", config.deviceAddress, 5, 6)
	private var lastState = emptyList<LightConfig>()
	private val objectMapper = ObjectMapper()
	override val icon: Icon = RaspiW2812Icon.RASPBERRY
	override val navigationRoot: NavigationItem
		get() = super.navigationRoot.apply {
			sub?.add(NavigationItem(
					title = "Reconnect",
					icon = RaspiW2812Icon.DISCONNECTED,
					action = { communicator.connect() },
					update = { entry, _ -> (entry as? MenuItem)?.enabled = config.enabled }
			))
			sub?.add(NavigationItem(title = null))

			config.items.sortedBy { it.id }.forEach { itemConfig ->
				val title = itemConfig.id.takeIf { it.isNotEmpty() }
				sub?.add(NavigationItem(
						title = title ?: "Default",
						icon = title?.let { RaspiW2812Icon.NON_DEFAULT } ?: RaspiW2812Icon.DEFAULT,
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
			changeLights(lastState)
			controller.saveConfig()
		}
		NotifierAction.DISABLE -> {
			config.enabled = false
			controller.saveConfig()
		}
		NotifierAction.TOGGLE -> execute(if (config.enabled) NotifierAction.DISABLE else NotifierAction.ENABLE)
		NotifierAction.SHUTDOWN -> changeLights(listOf(LightConfig()))
	}

	internal fun reset() {
//		serialPortCommunicator = null
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