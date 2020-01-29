package com.poterion.monitor.api.modules

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.notifiers.NotifierConfig

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
interface NotifierModule<out Conf : NotifierConfig, out Ctrl : Notifier<Conf>> : Module<Conf, Ctrl> {
	override fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			Collection<Ctrl> = applicationConfiguration
			.notifiers
			.values
			.filterIsInstance(configClass.java)
			.map { loadController(controller, it) }
}