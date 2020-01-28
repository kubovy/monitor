package com.poterion.monitor.data.services

import com.fasterxml.jackson.annotation.JsonInclude
import com.poterion.monitor.data.HttpConfig
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.Priority

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
interface ServiceConfig : ModuleConfig, HttpConfig {
	var order: Int
		@JsonInclude(value = JsonInclude.Include.ALWAYS)
		get
	var priority: Priority
		@JsonInclude(value = JsonInclude.Include.ALWAYS)
		get
	var checkInterval: Long?
		@JsonInclude(value = JsonInclude.Include.ALWAYS)
		get
}
