package com.poterion.monitor.control.notifiers

import com.poterion.monitor.api.NotifierController
import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.control.SerialPortCommunicator
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.data.notifiers.RaspiW2812Config
import jssc.SerialPortException
import org.slf4j.LoggerFactory

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class RaspiW2812Controller(private val config: RaspiW2812Config) : NotifierController {
    companion object {
        val LOGGER = LoggerFactory.getLogger(RaspiW2812Controller::class.java)
    }

    private var serialPortCommunicator: SerialPortCommunicator? = null
    private var lastState = emptyList<String>()

    init {
        StatusCollector.subscribeToWorstUpdate { statusItem ->
            LOGGER.info("${if (config.enabled) "Changing" else "Skipping"}: ${statusItem}")
            if (config.enabled) {
                statusItem.toLightString()?.also {
                    changeLights(it)
                }
            }
        }
    }

    private fun changeLights(lightConfiguration: List<String>, storeState: Boolean = true, rescue: Boolean = true) {
        if (storeState) lastState = lightConfiguration
        if (serialPortCommunicator == null) serialPortCommunicator = SerialPortCommunicator.findCommunicator()
        try {
            serialPortCommunicator?.sendMessage(lightConfiguration)
        } catch (e: SerialPortException) {
            LOGGER.error(e.message, e)
            serialPortCommunicator = null
            if (rescue) changeLights(lightConfiguration, storeState = storeState, rescue = false)
        }
    }

    override fun execute(action: NotifierAction, vararg params: String) = when (action) {
        NotifierAction.NOTIFY -> changeLights(params.toList())
        NotifierAction.ENABLE -> {
            config.enabled = true
            changeLights(lastState, storeState = false)
        }
        NotifierAction.DISABLE -> config.enabled = false
        NotifierAction.SHUTDOWN -> changeLights(listOf("light 0,0,0 0,0,0 50 3 0 0 100"), storeState = false)
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