package com.poterion.monitor.notification.tabs

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.notification.tabs.control.NotificationTabsNotifier
import com.poterion.monitor.notification.tabs.data.NotificationTabsConfig
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
object NotificationTabsModule : NotifierModule<NotificationTabsConfig, NotificationTabsNotifier> {
	override val configClass: KClass<out NotificationTabsConfig> = NotificationTabsConfig::class

	override val title: String
		get() = "Notification Tabs"

	override val icon: Icon = NotificationTabsIcon.TABS

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			NotificationTabsNotifier = NotificationTabsConfig(uuid = applicationConfiguration.notifiers.nextUUID(), name = title)
			.also { applicationConfiguration.notifiers[it.uuid] = it }
			.let { NotificationTabsNotifier(controller, it) }

	override fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			Collection<NotificationTabsNotifier> = applicationConfiguration
			.notifiers
			.values
			.filterIsInstance<NotificationTabsConfig>()
			.map { NotificationTabsNotifier(controller, it) }
}