package com.poterion.monitor.notifiers.deploymentcase.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.notifiers.NotifierConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class DeploymentCaseConfig(override var type: String = DeploymentCaseConfig::class.java.simpleName,
								override var name: String = "",
								override var enabled: Boolean = true,
								override var minPriority: Priority = Priority.LOW,
								var deviceAddress: String = "",
								var testNameHistory: List<String> = emptyList(),
								var testValueHistory: List<String> = emptyList(),
								var configurations: List<Configuration> = emptyList()) : NotifierConfig