package com.poterion.monitor.notifiers.deploymentcase

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.data.ApplicationConfiguration
import com.poterion.monitor.notifiers.deploymentcase.control.DeploymentCaseNotifier
import com.poterion.monitor.notifiers.deploymentcase.data.DeploymentCaseConfig
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
object DeploymentCaseModule : NotifierModule<DeploymentCaseConfig, DeploymentCaseNotifier> {
	override val configClass: KClass<out DeploymentCaseConfig> = DeploymentCaseConfig::class

	override val title: String
		get() = "Deployment Case"

	override val icon: Icon = DeploymentCaseIcon.NUCLEAR_FOOTBALL

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration): DeploymentCaseNotifier =
			DeploymentCaseNotifier(controller, DeploymentCaseConfig(name = title).also { applicationConfiguration.notifiers.add(it) })

	override fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration): Collection<DeploymentCaseNotifier> = applicationConfiguration.notifiers
			.filterIsInstance<DeploymentCaseConfig>()
			.map { DeploymentCaseNotifier(controller, it) }
}