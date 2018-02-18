package com.poterion.monitor.notifiers.raspiw2812.control

import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.NotifierController
import com.poterion.monitor.api.ui.Item
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.notifiers.raspiw2812.data.RaspiW2812Config
import com.poterion.monitor.notifiers.raspiw2812.ui.ConfigWindowController
import jssc.SerialPortException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class RaspiW2812NotifierController(override val controller: ControllerInterface, config: RaspiW2812Config) : NotifierController<RaspiW2812Config>(config) {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(RaspiW2812NotifierController::class.java)
	}

	private var serialPortCommunicator: SerialPortCommunicator? = null
	private var lastState = emptyList<String>()
	override val menuItem: Item
		get() = super.menuItem.apply {
			sub?.add(Item(title = "Configure", action = {
				ConfigWindowController.create(controller.stage, config, this@RaspiW2812NotifierController)
			}))
			sub?.add(Item()) // Separator
			config.items.sortedBy { it.id }.forEach { itemConfig ->
				sub?.add(Item(
						title = itemConfig.id.takeIf { it.isNotEmpty() } ?: "Default",
						sub = listOf("None" to itemConfig.statusNone,
								"Unknown" to itemConfig.statusUnknown,
								"OK" to itemConfig.statusOk,
								"Info" to itemConfig.statusInfo,
								"Notification" to itemConfig.statusNotification,
								"Connection Error" to itemConfig.statusConnectionError,
								"Service Error" to itemConfig.statusServiceError,
								"Warning" to itemConfig.statusWarning,
								"Error" to itemConfig.statusError,
								"Fatal" to itemConfig.statusFatal)
								.map { (label, lights) ->
									Item(title = label,
											action = {
												//enabledMenuItem.checked = false
												execute(NotifierAction.DISABLE)
												execute(NotifierAction.NOTIFY, *lights.toTypedArray())
											})
								}
								.toMutableList()))
			}
		}

	init {
		StatusCollector.subscribeToWorstUpdate { statusItem ->
			LOGGER.info("${if (config.enabled) "Changing" else "Skipping"}: ${statusItem}")
			statusItem.toLightString()?.also {
				lastState = it
				if (config.enabled) changeLights(it)
			}
		}
	}

	private fun changeLights(lightConfiguration: List<String>, rescue: Boolean = true) {
		if (serialPortCommunicator == null) serialPortCommunicator = SerialPortCommunicator.findCommunicator()
		try {
			serialPortCommunicator?.sendMessage(lightConfiguration)
		} catch (e: SerialPortException) {
			LOGGER.error(e.message, e)
			serialPortCommunicator = null
			if (rescue) changeLights(lightConfiguration, rescue = false)
		}
	}

	override fun execute(action: NotifierAction, vararg params: String): Unit = when (action) {
		NotifierAction.NOTIFY -> changeLights(params.toList())
		NotifierAction.ENABLE -> {
			config.enabled = true
			changeLights(lastState)
			controller.updateConfig()
		}
		NotifierAction.DISABLE -> {
			config.enabled = false
			controller.updateConfig()
		}
		NotifierAction.TOGGLE -> execute(if (config.enabled) NotifierAction.DISABLE else NotifierAction.ENABLE, *params)
		NotifierAction.SHUTDOWN -> changeLights(listOf("light 0,0,0 0,0,0 50 3 0 0 100"))
	}

	private fun StatusItem?.toLightString(): List<String>? {
		val lightConfig = config.items
				.map { it.id to it }
				.toMap()
				.let { it[this?.label ?: ""] ?: it[""] }

		return when (this?.status) {
			Status.NONE -> lightConfig?.statusNone
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