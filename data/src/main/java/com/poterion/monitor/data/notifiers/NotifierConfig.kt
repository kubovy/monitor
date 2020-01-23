package com.poterion.monitor.data.notifiers

import com.fasterxml.jackson.annotation.JsonInclude
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
interface NotifierConfig : ModuleConfig {
	var minPriority: Priority
		@JsonInclude(value = JsonInclude.Include.ALWAYS)
		get
	var minStatus: Status
		@JsonInclude(value = JsonInclude.Include.ALWAYS)
		get
	val services: MutableSet<String>
}