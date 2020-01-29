package com.poterion.monitor.sensors.storyboard

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.sensors.storyboard.control.StoryboardService
import com.poterion.monitor.sensors.storyboard.data.StoryboardConfig
import com.poterion.utils.javafx.Icon
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
object StoryboardModule : ServiceModule<StoryboardConfig, StoryboardService> {
	override val configClass: KClass<StoryboardConfig> = StoryboardConfig::class

	override val title: String
		get() = "Storyboard"

	override val icon: Icon = StoryboardIcon.STORYBOARD

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			StoryboardService = StoryboardService(controller,
			StoryboardConfig(uuid = applicationConfiguration.services.nextUUID(), name = title))

	override fun loadController(controller: ControllerInterface, config: ModuleConfig): StoryboardService =
			StoryboardService(controller, config as StoryboardConfig)
}