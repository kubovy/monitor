package com.poterion.monitor.sensors.jira

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.utils.javafx.Icon
import com.poterion.monitor.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.sensors.jira.control.JiraService
import com.poterion.monitor.sensors.jira.data.JiraConfig
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
			JiraService = JiraConfig(uuid = applicationConfiguration.services.nextUUID(), name = title)
			.also { applicationConfiguration.services[it.uuid] = it }
			.let { JiraService(controller, it) }

	override fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			Collection<JiraService> = applicationConfiguration
			.services
			.values
			.filterIsInstance<JiraConfig>()
			.map { JiraService(controller, it) }
}