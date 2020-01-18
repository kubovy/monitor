package com.poterion.monitor.notifications

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.notifications.control.NotificationsNotifier
import com.poterion.monitor.notifications.data.NotificationsConfig
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
object NotificationsModule : NotifierModule<NotificationsConfig, NotificationsNotifier> {
	override val configClass: KClass<out NotificationsConfig> = NotificationsConfig::class

	override val singleton: Boolean
		get() = true

	override val title: String
		get() = "Notifications"

	override val icon: Icon = NotificationsIcon.NOTIFICATIONS

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			NotificationsNotifier = NotificationsConfig(uuid = applicationConfiguration.notifiers.nextUUID(), name = title)
			.also { applicationConfiguration.notifiers[it.uuid] = it }
			.let { NotificationsNotifier(controller, it) }

	override fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			Collection<NotificationsNotifier> = applicationConfiguration
			.notifiers
			.values
			.filterIsInstance<NotificationsConfig>()
			.map { NotificationsNotifier(controller, it) }
}