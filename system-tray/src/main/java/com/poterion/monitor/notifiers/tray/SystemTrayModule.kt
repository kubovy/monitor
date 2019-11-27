package com.poterion.monitor.notifiers.tray

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.data.ApplicationConfiguration
import com.poterion.monitor.notifiers.tray.control.SystemTrayNotifier
import com.poterion.monitor.notifiers.tray.data.SystemTrayConfig
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
object SystemTrayModule : NotifierModule<SystemTrayConfig, SystemTrayNotifier> {
	override val configClass: KClass<out SystemTrayConfig> = SystemTrayConfig::class

	override val singleton: Boolean
		get() = true

	override val title: String
		get() = "System Tray"

	override val icon: Icon = SystemTrayIcon.TRAY

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration): SystemTrayNotifier =
			SystemTrayNotifier(controller, SystemTrayConfig(name = title).also { applicationConfiguration.notifiers.add(it) })
					.also { it.initialize() }

	override fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration): Collection<SystemTrayNotifier> = applicationConfiguration
			.notifiers
			.filterIsInstance<SystemTrayConfig>()
			.map { SystemTrayNotifier(controller, it) }
}