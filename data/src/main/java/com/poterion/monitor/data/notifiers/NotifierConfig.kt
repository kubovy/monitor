package com.poterion.monitor.data.notifiers

import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface NotifierConfig : ModuleConfig {
	var minPriority: Priority
	var minStatus: Status
	val services: MutableSet<String>
}