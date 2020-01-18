package com.poterion.monitor.notifiers.devops.light

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.notifiers.devops.light.control.DevOpsLightNotifier
import com.poterion.monitor.notifiers.devops.light.data.DevOpsLightConfig
import kotlin.reflect.KClass

object DevOpsLight : NotifierModule<DevOpsLightConfig, DevOpsLightNotifier> {
	override val configClass: KClass<out DevOpsLightConfig> = DevOpsLightConfig::class

	override val title: String
		get() = "Dev/Ops Light"

	override val icon: Icon = DevOpsLightIcon.LOGO

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			DevOpsLightNotifier = DevOpsLightConfig(uuid = applicationConfiguration.notifiers.nextUUID(), name = title)
			.also { applicationConfiguration.notifiers[it.uuid] = it }
			.let { DevOpsLightNotifier(controller, it) }

	override fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			Collection<DevOpsLightNotifier> = applicationConfiguration
			.notifiers
			.values
			.filterIsInstance<DevOpsLightConfig>()
			.map { DevOpsLightNotifier(controller, it) }
}