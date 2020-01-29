package com.poterion.monitor.sensors.jenkins

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.sensors.jenkins.control.JenkinsService
import com.poterion.monitor.sensors.jenkins.data.JenkinsConfig
import com.poterion.utils.javafx.Icon
import kotlin.reflect.KClass

object JenkinsModule : ServiceModule<JenkinsConfig, JenkinsService> {
	override val configClass: KClass<JenkinsConfig> = JenkinsConfig::class

	override val title: String
		get() = "Jenkins"

	override val icon: Icon = JenkinsIcon.JENKINS

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			JenkinsService = JenkinsService(controller,
			JenkinsConfig(uuid = applicationConfiguration.services.nextUUID(), name = title))

	override fun loadController(controller: ControllerInterface, config: ModuleConfig): JenkinsService =
			JenkinsService(controller, config as JenkinsConfig)
}