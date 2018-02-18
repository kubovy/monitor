package com.poterion.monitor.sensors.sonar

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.data.Config
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.monitor.sensors.sonar.control.SonarServiceController
import com.poterion.monitor.sensors.sonar.data.SonarConfig
import kotlin.reflect.KClass

object SonarModule : ServiceModule<SonarConfig, SonarServiceController> {
	override val configClass: KClass<SonarConfig> = SonarConfig::class
	override fun createControllers(controller: ControllerInterface, config: Config): Collection<SonarServiceController> = config.services
			.filter { it is SonarConfig }
			.map { it as SonarConfig }
			.map { SonarServiceController(controller, it) }
}