package com.poterion.monitor.notifiers.notifications

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.notifiers.notifications.control.NotificationsNotifier
import com.poterion.monitor.notifiers.notifications.data.NotificationsConfig
import com.poterion.utils.javafx.Icon
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
object NotificationsModule : NotifierModule<NotificationsConfig, NotificationsNotifier> {
	override val configClass: KClass<out NotificationsConfig> = NotificationsConfig::class

	override val title: String
		get() = "Notifications"

	override val icon: Icon = NotificationsIcon.NOTIFICATIONS

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			NotificationsNotifier = NotificationsNotifier(controller,
			NotificationsConfig(uuid = applicationConfiguration.notifiers.nextUUID(), name = title))

	override fun loadController(controller: ControllerInterface, config: ModuleConfig): NotificationsNotifier =
			NotificationsNotifier(controller, config as NotificationsConfig)
}