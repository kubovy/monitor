package com.poterion.monitor.notifiers.tray

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.notifiers.tray.control.SystemTrayNotifier
import com.poterion.monitor.notifiers.tray.data.SystemTrayConfig
import com.poterion.utils.javafx.Icon
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
object SystemTrayModule : NotifierModule<SystemTrayConfig, SystemTrayNotifier> {
	override val configClass: KClass<out SystemTrayConfig> = SystemTrayConfig::class

	override val singleton: Boolean
		get() = true

	override val title: String
		get() = "System Tray"

	override val icon: Icon = SystemTrayIcon.TRAY

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			SystemTrayNotifier = SystemTrayNotifier(controller,
			SystemTrayConfig(uuid = applicationConfiguration.notifiers.nextUUID(), name = title))

	override fun loadController(controller: ControllerInterface, config: ModuleConfig): SystemTrayNotifier =
			SystemTrayNotifier(controller, config as SystemTrayConfig)
}