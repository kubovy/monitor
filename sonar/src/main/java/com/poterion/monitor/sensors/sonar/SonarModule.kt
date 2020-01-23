package com.poterion.monitor.sensors.sonar

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.utils.javafx.Icon
import com.poterion.monitor.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.sensors.sonar.control.SonarService
import com.poterion.monitor.sensors.sonar.data.SonarConfig
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
			SonarService = SonarConfig(uuid = applicationConfiguration.services.nextUUID(), name = title)
			.also { applicationConfiguration.services[it.uuid] = it }
			.let { SonarService(controller, it) }

	override fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			Collection<SonarService> = applicationConfiguration
			.services
			.values
			.filterIsInstance<SonarConfig>()
			.map { SonarService(controller, it) }
}