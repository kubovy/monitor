package com.poterion.monitor.notifiers.devops.light.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.notifiers.NotifierConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class DevOpsLightConfig(override var type: String = DevOpsLightConfig::class.java.simpleName,
							 override var name: String = "",
							 override var enabled: Boolean = true,
							 override var minPriority: Priority = Priority.LOW,
							 var deviceAddress: String = "",
							 var usbPort: String = "",
							 var grbColors: Boolean = false,
							 var combineMultipleServices: Boolean = true,
							 var split: Double = 0.2,
							 var items: Collection<DevOpsLightItemConfig> = emptyList()) : NotifierConfig