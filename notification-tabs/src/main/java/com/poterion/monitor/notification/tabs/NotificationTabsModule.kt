package com.poterion.monitor.notification.tabs

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.data.ApplicationConfiguration
import com.poterion.monitor.notification.tabs.control.NotificationTabsNotifier
import com.poterion.monitor.notification.tabs.data.NotificationTabsConfig
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
object NotificationTabsModule : NotifierModule<NotificationTabsConfig, NotificationTabsNotifier> {
	override val configClass: KClass<out NotificationTabsConfig> = NotificationTabsConfig::class

	override val singleton: Boolean
		get() = true

	override val title: String
		get() = "Notification Tabs"

	override val icon: Icon = NotificationTabsIcon.TABS

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration): NotificationTabsNotifier =
			NotificationTabsNotifier(controller, NotificationTabsConfig(name = title).also { applicationConfiguration.notifiers.add(it) })
					.also { it.initialize() }

	override fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration): Collection<NotificationTabsNotifier> = applicationConfiguration
			.notifiers
			.filterIsInstance<NotificationTabsConfig>()
			.map { NotificationTabsNotifier(controller, it) }
}