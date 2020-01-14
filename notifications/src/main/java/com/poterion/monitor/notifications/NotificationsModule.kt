package com.poterion.monitor.notifications

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.data.ApplicationConfiguration
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

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration): NotificationsNotifier =
			NotificationsNotifier(controller, NotificationsConfig(name = title).also { applicationConfiguration.notifiers.add(it) })
					.also { it.initialize() }

	override fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration): Collection<NotificationsNotifier> = applicationConfiguration
			.notifiers
			.filterIsInstance<NotificationsConfig>()
			.map { NotificationsNotifier(controller, it) }
}