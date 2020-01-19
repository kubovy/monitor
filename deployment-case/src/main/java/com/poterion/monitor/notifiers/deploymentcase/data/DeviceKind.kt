package com.poterion.monitor.notifiers.deploymentcase.data

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class DeviceKind(val description: String) {
	MCP23017("MCP23017"),
	WS281x("WS281x"),
	LCD("LCD"),
	BLUETOOTH("Bluetooth"),
	VIRTUAL("Virtual");
}