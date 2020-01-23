package com.poterion.monitor.sensors.jenkins

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.utils.javafx.Icon
import com.poterion.monitor.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.sensors.jenkins.control.JenkinsService
import com.poterion.monitor.sensors.jenkins.data.JenkinsConfig
import kotlin.reflect.KClass

object JenkinsModule : ServiceModule<JenkinsConfig, JenkinsService> {
	override val configClass: KClass<JenkinsConfig> = JenkinsConfig::class

	override val title: String
		get() = "Jenkins"

	override val icon: Icon = JenkinsIcon.JENKINS

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			JenkinsService = JenkinsConfig(uuid = applicationConfiguration.services.nextUUID(), name = title)
			.also { applicationConfiguration.services[it.uuid] = it }
			.let { JenkinsService(controller, it) }

	override fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			Collection<JenkinsService> = applicationConfiguration
			.services
			.values
			.filterIsInstance<JenkinsConfig>()
			.map { JenkinsService(controller, it) }
}