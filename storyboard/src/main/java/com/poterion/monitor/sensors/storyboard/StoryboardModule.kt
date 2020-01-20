package com.poterion.monitor.sensors.storyboard

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.sensors.storyboard.control.StoryboardService
import com.poterion.monitor.sensors.storyboard.data.StoryboardConfig
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
object StoryboardModule : ServiceModule<StoryboardConfig, StoryboardService> {
	override val configClass: KClass<StoryboardConfig> = StoryboardConfig::class

	override val title: String
		get() = "Storyboard"

	override val icon: Icon = StoryboardIcon.STORYBOARD

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			StoryboardService = StoryboardConfig(uuid = applicationConfiguration.services.nextUUID(), name = title)
			.also { applicationConfiguration.services[it.uuid] = it }
			.let { StoryboardService(controller, it) }

	override fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			Collection<StoryboardService> = applicationConfiguration
			.services
			.values
			.filterIsInstance<StoryboardConfig>()
			.map { StoryboardService(controller, it) }
}