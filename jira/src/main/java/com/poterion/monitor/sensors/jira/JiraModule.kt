package com.poterion.monitor.sensors.jira

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.sensors.jira.control.JiraService
import com.poterion.monitor.sensors.jira.data.JiraConfig
import com.poterion.utils.javafx.Icon
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
object JiraModule : ServiceModule<JiraConfig, JiraService> {
	override val configClass: KClass<JiraConfig> = JiraConfig::class

	override val title: String
		get() = "Jira"

	override val icon: Icon = JiraIcon.JIRA

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			JiraService = JiraService(controller,
			JiraConfig(uuid = applicationConfiguration.services.nextUUID(), name = title))

	override fun loadController(controller: ControllerInterface, config: ModuleConfig): JiraService =
			JiraService(controller, config as JiraConfig)
}