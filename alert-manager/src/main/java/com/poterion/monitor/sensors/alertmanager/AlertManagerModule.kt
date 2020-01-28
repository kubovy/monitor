package com.poterion.monitor.sensors.alertmanager

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.sensors.alertmanager.control.AlertManagerService
import com.poterion.monitor.sensors.alertmanager.data.AlertManagerConfig
import com.poterion.utils.javafx.Icon
import kotlin.reflect.KClass

object AlertManagerModule : ServiceModule<AlertManagerConfig, AlertManagerService> {
	override val configClass: KClass<AlertManagerConfig> = AlertManagerConfig::class

	override val title: String
		get() = "Alert Manager"

	override val icon: Icon = AlertManagerIcon.ALERT_MANAGER

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			AlertManagerService = AlertManagerConfig(uuid = applicationConfiguration.services.nextUUID(), name = title)
			.also { applicationConfiguration.services[it.uuid] = it }
			.let { AlertManagerService(controller, it) }

	override fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			Collection<AlertManagerService> = applicationConfiguration
			.services
			.values
			.filterIsInstance<AlertManagerConfig>()
			.map { AlertManagerService(controller, it) }
}