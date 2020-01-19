package com.poterion.monitor.notifiers.deploymentcase

import com.poterion.monitor.notifiers.deploymentcase.data.*

fun Device.getDisplayName(): String = this.name.takeUnless { it.isEmpty() } ?: this.let {
	when (it.kind) {
		DeviceKind.VIRTUAL -> when (it.key) {
			VirtualKey.GOTO.key -> "Goto State"
			VirtualKey.ENTER.key -> "Enter Input"
			VirtualKey.ENTERED.key -> "Input Entered"
			VirtualKey.ABORTED.key -> "Input Aborted"
			else -> "${it.kind} [${it.key}]"
		}
		else -> "${it.kind} [${it.key}]"
	}
}


fun String.toDevice(devices: List<Device>) = devices.find { it.name == this || "${it.kind} [${it.key}]" == this }

fun getDisplayString(value: String?, type: VariableType) = when (type) {
	VariableType.COLOR_PATTERN -> value
			?.split(",")
			?.takeIf { it.size == 5 }
			?.mapIndexed { i, v ->
				when (i) {
					0 -> v.toIntOrNull()?.let { LightPattern.values()[it].description }
					in (1..2) -> v // color hex, delay
					in (3..4) -> "${v.split("x").getOrNull(1)?.toInt(16)}" // min, max
					else -> null
				}
			}
			?.filterNotNull()
			?.takeIf { it.size == 5 }
			?.let {
				if (it[0] == LightPattern.LIGHT.description) listOf(it[0], it[1], it[2], it[4])
				else listOf(it[0], it[1], it[2], "${it[3]}-${it[4]}")
			}
			?.let { "${it[0]} (${it[1]}, delay: ${it[2]}, limits: ${it[3]})" }
			?: ""
	VariableType.ENTER -> value
			?.split("|")
			?.let { "${it.getOrNull(1) ?: "?"}: ${it.getOrNull(2) ?: "?"} (${it.getOrNull(0) ?: "?"})" }
	VariableType.BOOLEAN -> if (value?.toBoolean() == true) "ON" else "OFF"
	else -> value
}

fun Variable.getDisplayString() = getDisplayString(value, type)

fun Variable.getDisplayName() = when (type) {
	VariableType.STATE -> "${getDisplayString()}"
	else -> name
}

fun Variable.getDisplayNameValue() = when (type) {
	VariableType.STATE -> "${getDisplayString()}"
	else -> "${name}: ${getDisplayString()}"
}

fun String.toVariable(variables: Collection<Variable>) = variables.find { it.name == this }

fun String?.toVariableFromValue(variables: List<Variable>) = variables.find { it.value == this }
