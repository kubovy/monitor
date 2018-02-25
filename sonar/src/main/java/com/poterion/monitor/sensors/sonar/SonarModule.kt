package com.poterion.monitor.sensors.sonar

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.data.Config
import com.poterion.monitor.sensors.sonar.control.SonarService
import com.poterion.monitor.sensors.sonar.data.SonarConfig
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
object SonarModule : ServiceModule<SonarConfig, SonarService> {
	override val configClass: KClass<SonarConfig> = SonarConfig::class
	override fun createControllers(controller: ControllerInterface, config: Config): Collection<SonarService> = config.services
			.filter { it is SonarConfig }
			.map { it as SonarConfig }
			.map { SonarService(controller, it) }
}