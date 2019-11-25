package com.poterion.monitor.sensors.jenkins

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.data.ApplicationConfiguration
import com.poterion.monitor.sensors.jenkins.control.JenkinsService
import com.poterion.monitor.sensors.jenkins.data.JenkinsConfig
import com.poterion.monitor.sensors.jenkins.ui.JenkinsIcon
import kotlin.reflect.KClass

object JenkinsModule : ServiceModule<JenkinsConfig, JenkinsService> {
	override val configClass: KClass<JenkinsConfig> = JenkinsConfig::class

	override val title: String
		get() = "Jenkins"

	override val icon: Icon = JenkinsIcon.JENKINS

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration): JenkinsService =
			JenkinsService(controller, JenkinsConfig(name = title).also { applicationConfiguration.services.add(it) })

	override fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration): Collection<JenkinsService> = applicationConfiguration
			.services
			.filterIsInstance<JenkinsConfig>()
			.map { JenkinsService(controller, it) }
}