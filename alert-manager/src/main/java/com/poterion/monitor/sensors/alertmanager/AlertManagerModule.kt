package com.poterion.monitor.sensors.alertmanager

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.data.ModuleConfig
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
			AlertManagerService = AlertManagerService(controller,
			AlertManagerConfig(uuid = applicationConfiguration.services.nextUUID(), name = title))

	override fun loadController(controller: ControllerInterface, config: ModuleConfig): AlertManagerService =
			AlertManagerService(controller, config as AlertManagerConfig)
}