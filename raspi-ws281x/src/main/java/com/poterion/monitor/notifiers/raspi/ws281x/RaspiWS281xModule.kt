package com.poterion.monitor.notifiers.raspi.ws281x

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.data.Config
import com.poterion.monitor.notifiers.raspi.ws281x.control.RaspiWS281xNotifier
import com.poterion.monitor.notifiers.raspi.ws281x.data.RaspiWS281xConfig
import kotlin.reflect.KClass

object RaspiWS281xModule : NotifierModule<RaspiWS281xConfig, RaspiWS281xNotifier> {
	override val configClass: KClass<out RaspiWS281xConfig> = RaspiWS281xConfig::class
	override fun createControllers(controller: ControllerInterface, config: Config): Collection<RaspiWS281xNotifier> = config.notifiers
			.filter { it is RaspiWS281xConfig }
			.map { it as RaspiWS281xConfig }
			.map { RaspiWS281xNotifier(controller, it) }
}