package com.poterion.monitor.notifiers.devopslight.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.notifiers.NotifierConfig
import java.util.*

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class DevOpsLightConfig(override var type: String = DevOpsLightConfig::class.java.simpleName,
							 override val uuid: String = UUID.randomUUID().toString(),
							 override var name: String = "",
							 override var enabled: Boolean = true,
							 override var minPriority: Priority = Priority.LOW,
							 override var minStatus: Status = Status.NONE,
							 override val services: MutableSet<String> = mutableSetOf(),
							 override var tableColumnWidths: MutableMap<String, Int> = mutableMapOf(),
							 var deviceAddress: String = "",
							 var usbPort: String = "",
							 var grbColors: Boolean = false,
							 var combineMultipleServices: Boolean = true,
							 var split: Double = 0.2,
							 val items: MutableCollection<DevOpsLightItemConfig> = mutableListOf()) : NotifierConfig