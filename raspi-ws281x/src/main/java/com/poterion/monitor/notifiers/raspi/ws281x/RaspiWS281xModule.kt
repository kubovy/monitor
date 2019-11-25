package com.poterion.monitor.notifiers.raspi.ws281x

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.data.ApplicationConfiguration
import com.poterion.monitor.notifiers.raspi.ws281x.control.RaspiWS281xNotifier
import com.poterion.monitor.notifiers.raspi.ws281x.data.RaspiWS281xConfig
import kotlin.reflect.KClass

object RaspiWS281xModule : NotifierModule<RaspiWS281xConfig, RaspiWS281xNotifier> {
	override val configClass: KClass<out RaspiWS281xConfig> = RaspiWS281xConfig::class

	override val title: String
		get() = "Raspi WS281x"

	override val icon: Icon = RaspiWS281xIcon.RASPBERRY

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration): RaspiWS281xNotifier =
			RaspiWS281xNotifier(controller, RaspiWS281xConfig(name = title).also { applicationConfiguration.notifiers.add(it) })

	override fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration): Collection<RaspiWS281xNotifier> = applicationConfiguration.notifiers
			.filterIsInstance<RaspiWS281xConfig>()
			.map { RaspiWS281xNotifier(controller, it) }
}