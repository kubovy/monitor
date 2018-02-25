package com.poterion.monitor.notifiers.raspiw2812

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.data.Config
import com.poterion.monitor.notifiers.raspiw2812.control.RaspiW2812Notifier
import com.poterion.monitor.notifiers.raspiw2812.data.RaspiW2812Config
import kotlin.reflect.KClass

object RaspiW2812Module : NotifierModule<RaspiW2812Config, RaspiW2812Notifier> {
	override val configClass: KClass<out RaspiW2812Config> = RaspiW2812Config::class
	override fun createControllers(controller: ControllerInterface, config: Config): Collection<RaspiW2812Notifier> = config.notifiers
			.filter { it is RaspiW2812Config }
			.map { it as RaspiW2812Config }
			.map { RaspiW2812Notifier(controller, it) }
}