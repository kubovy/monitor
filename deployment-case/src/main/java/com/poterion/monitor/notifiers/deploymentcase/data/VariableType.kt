package com.poterion.monitor.notifiers.deploymentcase.data

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class VariableType(val description: String) {
	BOOLEAN("Boolean"),
	//UINT8("Integer"),
	//UINT8_ARRAY("Array"),
	STRING("String"),
	//COLOR("Color"),
	COLOR_PATTERN("Color & Pattern"),
	STATE("State"),
	ENTER("Enter")
}