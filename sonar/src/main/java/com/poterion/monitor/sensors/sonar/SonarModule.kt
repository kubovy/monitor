package com.poterion.monitor.sensors.sonar

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.sensors.sonar.control.SonarService
import com.poterion.monitor.sensors.sonar.data.SonarConfig
import com.poterion.utils.javafx.Icon
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
object SonarModule : ServiceModule<SonarConfig, SonarService> {
	override val configClass: KClass<SonarConfig> = SonarConfig::class

	override val title: String
		get() = "Sonar"

	override val icon: Icon = SonarIcon.SONAR

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			SonarService = SonarService(controller,
			SonarConfig(uuid = applicationConfiguration.services.nextUUID(), name = title))

	override fun loadController(controller: ControllerInterface, config: ModuleConfig): SonarService =
			SonarService(controller, config as SonarConfig)
}