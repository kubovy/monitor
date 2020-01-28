package com.poterion.monitor.notifiers.deploymentcase

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.notifiers.deploymentcase.control.DeploymentCaseNotifier
import com.poterion.monitor.notifiers.deploymentcase.data.DeploymentCaseConfig
import com.poterion.utils.javafx.Icon
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
object DeploymentCaseModule : NotifierModule<DeploymentCaseConfig, DeploymentCaseNotifier> {
	override val configClass: KClass<out DeploymentCaseConfig> = DeploymentCaseConfig::class

	override val title: String
		get() = "Deployment Case"

	override val icon: Icon = DeploymentCaseIcon.NUCLEAR_FOOTBALL

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			DeploymentCaseNotifier = DeploymentCaseConfig(uuid = applicationConfiguration.notifiers.nextUUID(), name = title)
			.also { applicationConfiguration.notifiers[it.uuid] = it }
			.let { DeploymentCaseNotifier(controller, it) }

	override fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			Collection<DeploymentCaseNotifier> = applicationConfiguration
			.notifiers
			.values
			.filterIsInstance<DeploymentCaseConfig>()
			.map { DeploymentCaseNotifier(controller, it) }
}