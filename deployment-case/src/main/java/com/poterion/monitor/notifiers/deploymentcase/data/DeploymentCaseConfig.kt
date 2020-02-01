package com.poterion.monitor.notifiers.deploymentcase.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.notifiers.NotifierConfig
import java.util.*

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
data class DeploymentCaseConfig(override var type: String = DeploymentCaseConfig::class.java.simpleName,
								override var uuid: String = UUID.randomUUID().toString(),
								override var name: String = "",
								override var enabled: Boolean = false,
								override var minPriority: Priority = Priority.LOW,
								override var minStatus: Status = Status.NONE,
								override val services: MutableSet<String> = mutableSetOf(),
								override var tableColumnWidths: MutableMap<String, Int> = mutableMapOf(),
								var debug: Boolean = false,
								var deviceAddress: String = "",
								val testNameHistory: MutableList<String> = mutableListOf(),
								val testValueHistory: MutableList<String> = mutableListOf(),
								var customColors: List<String>? = null,
								var split: Double = 0.2,
								val configurations: MutableList<Configuration> = mutableListOf()) : NotifierConfig