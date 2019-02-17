package com.poterion.monitor.notifiers.deploymentcase

import com.poterion.monitor.notifiers.deploymentcase.data.Device
import com.poterion.monitor.notifiers.deploymentcase.data.LightPattern
import com.poterion.monitor.notifiers.deploymentcase.data.Variable
import com.poterion.monitor.notifiers.deploymentcase.data.VariableType

fun Device?.getDisplayName() = this?.name?.takeUnless { it.isEmpty() } ?: this?.let { "${it.kind} [${it.key}]" } ?: ""

fun String?.toDevice(devices: List<Device>) = devices.find { it.name == this || "${it.kind} [${it.key}]" == this }

fun getDisplayString(value: String?, type: VariableType) = when (type) {
	VariableType.COLOR_PATTERN -> value
			?.split(",")
			?.mapNotNull { it.toIntOrNull() }
			?.takeIf { it.size == 4 }
			?.mapIndexed { i, v -> if (i == 0) LightPattern.values()[v].description else "%02x".format(v) }
			?.let { "${it[0]} (#${it[1]}${it[2]}${it[3]})" }
	VariableType.BOOLEAN -> if (value?.toBoolean() == true) "ON" else "OFF"
	else -> value
}

fun Variable.getDisplayString() = getDisplayString(value, type)

fun Variable.getDisplayName() = when(type) {
	VariableType.STATE -> "${getDisplayString()}"
	else -> name
}

fun Variable.getDisplayNameValue() = when(type) {
	VariableType.STATE -> "${getDisplayString()}"
	else -> "${name}: ${getDisplayString()}"
}


fun String?.toVariable(variables: List<Variable>) = variables.find { it.value == this }