package com.poterion.monitor.data

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
@JsonInclude(value = JsonInclude.Include.NON_NULL, content = JsonInclude.Include.NON_NULL)
interface ModuleConfig {
	var type: String
		@JsonInclude(value = JsonInclude.Include.ALWAYS)
		get
	val uuid: String
		@JsonInclude(value = JsonInclude.Include.ALWAYS)
		get
	var name: String
		@JsonInclude(value = JsonInclude.Include.ALWAYS)
		get
	var enabled: Boolean
		@JsonInclude(value = JsonInclude.Include.ALWAYS)
		get
	var tableColumnWidths: MutableMap<String, Int>
}