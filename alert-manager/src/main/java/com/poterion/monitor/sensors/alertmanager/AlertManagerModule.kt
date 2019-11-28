package com.poterion.monitor.sensors.alertmanager

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.data.ApplicationConfiguration
import com.poterion.monitor.sensors.alertmanager.control.AlertManagerService
import com.poterion.monitor.sensors.alertmanager.data.AlertManagerConfig
import com.poterion.monitor.sensors.alertmanager.ui.AlertManagerIcon
import kotlin.reflect.KClass

object AlertManagerModule : ServiceModule<AlertManagerConfig, AlertManagerService> {
	override val configClass: KClass<AlertManagerConfig> = AlertManagerConfig::class

	override val title: String
		get() = "Alert Manager"

	override val icon: Icon = AlertManagerIcon.ALERT_MANAGER

	override val staticNotificationSet: Boolean
		get() = false

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration): AlertManagerService =
			AlertManagerService(controller, AlertManagerConfig(name = title).also { applicationConfiguration.services.add(it) })

	override fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration): Collection<AlertManagerService> = applicationConfiguration
			.services
			.filterIsInstance<AlertManagerConfig>()
			.map { AlertManagerService(controller, it) }
}