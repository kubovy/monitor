package com.poterion.monitor.notifiers.deploymentcase

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.data.Config
import com.poterion.monitor.notifiers.deploymentcase.control.DeploymentCaseNotifier
import com.poterion.monitor.notifiers.deploymentcase.data.DeploymentCaseConfig
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
object DeploymentCaseModule : NotifierModule<DeploymentCaseConfig, DeploymentCaseNotifier> {
	override val configClass: KClass<out DeploymentCaseConfig> = DeploymentCaseConfig::class
	override fun createControllers(controller: ControllerInterface, config: Config): Collection<DeploymentCaseNotifier> = config.notifiers
			.filter { it is DeploymentCaseConfig }
			.map { it as DeploymentCaseConfig }
			.map { DeploymentCaseNotifier(controller, it) }
}