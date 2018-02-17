package com.poterion.monitor.control.notifiers

import com.poterion.monitor.api.NotifierController
import com.poterion.monitor.data.Config
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.data.notifiers.NotifierConfig
import com.poterion.monitor.data.notifiers.RaspiW2812Config

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
object Notifiers {
    private var controllers: Map<String, NotifierController> = emptyMap()

    fun configure(config: Config) {
        controllers = config.notifiers.map { it.name to create(it) }.toMap()
    }

    private fun create(notifierConfig: NotifierConfig): NotifierController = when (notifierConfig) {
    //is TrayConfig -> TrayNotifierController(notifierConfig)
        is RaspiW2812Config -> RaspiW2812Controller(notifierConfig)
        else -> throw Exception("Unknown ${notifierConfig::class} service definition")
    }

    operator fun get(name: String): NotifierController? = controllers[name]

    fun shutDown() = controllers.values.forEach { it.execute(NotifierAction.SHUTDOWN) }
}