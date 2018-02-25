package com.poterion.monitor.notifiers.tray

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.data.Config
import com.poterion.monitor.notifiers.tray.control.SystemTrayNotifier
import com.poterion.monitor.notifiers.tray.data.SystemTrayConfig
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
object SystemTrayModule : NotifierModule<SystemTrayConfig, SystemTrayNotifier> {
	override val configClass: KClass<out SystemTrayConfig> = SystemTrayConfig::class
	override fun createControllers(controller: ControllerInterface, config: Config): Collection<SystemTrayNotifier> = config.notifiers
			.filter { it is SystemTrayConfig }
			.map { it as SystemTrayConfig }
			.map { SystemTrayNotifier(controller, it) }
}