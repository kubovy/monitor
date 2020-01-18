package com.poterion.monitor.notifiers.deploymentcase.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.notifiers.NotifierConfig
import java.util.*

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class DeploymentCaseConfig(override var type: String = DeploymentCaseConfig::class.java.simpleName,
								override val uuid: String = UUID.randomUUID().toString(),
								override var name: String = "",
								override var enabled: Boolean = true,
								override var minPriority: Priority = Priority.LOW,
								var deviceAddress: String = "",
								var testNameHistory: List<String> = emptyList(),
								var testValueHistory: List<String> = emptyList(),
								var split: Double = 0.2,
								var configurations: List<Configuration> = emptyList()) : NotifierConfig