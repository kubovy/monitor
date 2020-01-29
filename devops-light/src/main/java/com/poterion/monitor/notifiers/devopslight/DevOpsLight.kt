package com.poterion.monitor.notifiers.devopslight

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.notifiers.devopslight.control.DevOpsLightNotifier
import com.poterion.monitor.notifiers.devopslight.data.DevOpsLightConfig
import com.poterion.utils.javafx.Icon
import kotlin.reflect.KClass

object DevOpsLight : NotifierModule<DevOpsLightConfig, DevOpsLightNotifier> {
	override val configClass: KClass<out DevOpsLightConfig> = DevOpsLightConfig::class

	override val title: String
		get() = "Dev/Ops Light"

	override val icon: Icon = DevOpsLightIcon.LOGO

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			DevOpsLightNotifier = DevOpsLightNotifier(controller,
			DevOpsLightConfig(uuid = applicationConfiguration.notifiers.nextUUID(), name = title))

	override fun loadController(controller: ControllerInterface, config: ModuleConfig): DevOpsLightNotifier =
			DevOpsLightNotifier(controller, config as DevOpsLightConfig)
}