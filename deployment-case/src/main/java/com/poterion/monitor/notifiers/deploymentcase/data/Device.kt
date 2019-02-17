package com.poterion.monitor.notifiers.deploymentcase.data

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class Device(var name: String = "",
				  var kind: DeviceKind = DeviceKind.MCP23017,
				  var key: String = "") {

	val type: VariableType
		@JsonIgnore
		get() = when (kind) {
			DeviceKind.MCP23017 -> VariableType.BOOLEAN
			DeviceKind.WS281x -> VariableType.COLOR_PATTERN
			DeviceKind.BLUETOOTH -> VariableType.BOOLEAN
			DeviceKind.LCD -> when (key) {
				"message" -> VariableType.STRING
				else -> VariableType.BOOLEAN
			}
			DeviceKind.VIRTUAL -> when (key) {
				"goto" -> VariableType.STATE
				else -> VariableType.BOOLEAN
			}
		}
}

