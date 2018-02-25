package com.poterion.monitor.data.notifiers

import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.Priority

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface NotifierConfig : ModuleConfig {
	var minPriority: Priority
}