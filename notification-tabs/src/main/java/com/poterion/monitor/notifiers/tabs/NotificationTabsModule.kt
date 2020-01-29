package com.poterion.monitor.notifiers.tabs

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.notifiers.tabs.control.NotificationTabsNotifier
import com.poterion.monitor.notifiers.tabs.data.NotificationTabsConfig
import com.poterion.utils.javafx.Icon
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
object NotificationTabsModule : NotifierModule<NotificationTabsConfig, NotificationTabsNotifier> {
	override val configClass: KClass<out NotificationTabsConfig> = NotificationTabsConfig::class

	override val title: String
		get() = "Notification Tabs"

	override val icon: Icon = NotificationTabsIcon.TABS

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			NotificationTabsNotifier = NotificationTabsNotifier(controller,
			NotificationTabsConfig(uuid = applicationConfiguration.notifiers.nextUUID(), name = title))

	override fun loadController(controller: ControllerInterface, config: ModuleConfig): NotificationTabsNotifier =
			NotificationTabsNotifier(controller, config as NotificationTabsConfig)
}