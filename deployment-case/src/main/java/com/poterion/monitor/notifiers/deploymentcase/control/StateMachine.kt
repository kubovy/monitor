package com.poterion.monitor.notifiers.deploymentcase.control

import com.poterion.monitor.api.lib.toRGBColor
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon
import com.poterion.monitor.notifiers.deploymentcase.data.*
import com.poterion.monitor.notifiers.deploymentcase.toVariable
import com.poterion.monitor.notifiers.deploymentcase.ui.expandAll
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import kotlin.math.floor

fun ByteArray.toIntList() = map { it.toUByte() }.map { it.toInt() }

fun List<Int>.toByteArray() = map { it.toByte() }.toByteArray()

fun TreeView<StateMachineItem>.toStateMachine(devices: Collection<Device>, variables: Collection<Variable>): List<State> = root.children
		.map { it to (it.value as State) }
		.map { (stateTreeItem, state) ->
			state.apply {
				evaluations = stateTreeItem
						.children
						.mapNotNull { evaluation ->
							(evaluation.value as? Evaluation)?.apply {
								conditions = evaluation.children.filter { (it.value as? Placeholder)?.getTitle(devices, variables) == "Conditions" }
										.flatMap { placeholder -> placeholder.children.map { it.value as? Condition } }
										.filterNotNull()
								actions = evaluation.children.filter { (it.value as? Placeholder)?.getTitle(devices, variables) == "Actions" }
										.flatMap { placeholder -> placeholder.children.map { it.value as? Action } }
										.filterNotNull()
							}
						}
			}
		}

fun TreeView<StateMachineItem>.setStateMachine(states: List<State>) {
	root.children.clear()
	root.children.addAll(states.map { state ->
		TreeItem<StateMachineItem>(state).apply {
			children.addAll(state.evaluations.map { evaluation ->
				TreeItem<StateMachineItem>(Evaluation()).apply {
					children.addAll(
							TreeItem<StateMachineItem>(Placeholder("Conditions", DeploymentCaseIcon.CONDITIONS)).apply {
								children.addAll(evaluation.conditions.map { TreeItem<StateMachineItem>(it) })
							},
							TreeItem<StateMachineItem>(Placeholder("Actions", DeploymentCaseIcon.ACTIONS)).apply {
								children.addAll(evaluation.actions.map { TreeItem<StateMachineItem>(it) })
							})
				}
			})
		}
	})
	root.isExpanded = true
	root.expandAll()
}

fun Configuration.toData() = stateMachine.toData(devices, variables)

/**
 *
 */
fun List<State>.toData(devices: Collection<Device>, variables: Collection<Variable>): List<Int> {
	val statesLength = mutableListOf<Int>()
	val statesResult = mutableListOf<Int>()
	val actions = mutableListOf<Action>()

	forEach { state ->
		val stateResult = mutableListOf<Int>()
		stateResult.add(state.evaluations.size) // Evaluation count
		state.evaluations.forEach { evaluation ->
			var value = 0
			var mask = 0

			(0 until 40).forEach { bit ->
				if (bit % 8 > 0) {
					value = value shr 1
					mask = mask shr 1
				}

				evaluation.conditions
						.find { condition ->
							when (bit) {
								32 -> condition.device?.let { it.toDevice(devices).kind == DeviceKind.BLUETOOTH && it.toDevice(devices).key == BluetoothKey.CONNECTED.key } == true
								else -> condition.device?.let { it.toDevice(devices).kind == DeviceKind.MCP23017 && it.toDevice(devices).key == "${bit}" } == true
							}
						}
						?.let { it.value?.toVariable(variables)?.value?.toBoolean() == true }
						?.let { if (it) 1 else 0 }
						?.also {
							value = value or (it shl 7)
							mask = mask or (1 shl 7)
						}

				if (bit % 8 == 7) {
					stateResult.add(value)  // Value
					stateResult.add(mask)   // Mask
					value = 0
					mask = 0
				}
			}

			stateResult.add(evaluation.actions.size) // Action count (max 255 actions)
			evaluation.actions.forEach { action ->
				val index = actions.indexOfFirst {
					it.device?.toDevice(devices)?.kind == action.device?.toDevice(devices)?.kind
							&& it.device?.toDevice(devices)?.key == action.device?.toDevice(devices)?.key
							&& it.value?.toVariable(variables)?.value == action.value?.toVariable(variables)?.value
				}
				if (index == -1) {
					stateResult.addAll(actions.size.to2ByteInt()) // New action index
					actions.add(action)
				} else {
					stateResult.addAll(index.to2ByteInt())        // Existing action index
				}
			}
		}

		statesLength.add(stateResult.size)
		statesResult.addAll(stateResult)
	}

	val result = mutableListOf<Int>()
	result.add(0) // Reserved for internal state machine status
	result.add(statesLength.size) // State count (max 255 states)
	var position = 4
	statesLength.forEach { length ->
		result.addAll((position + statesLength.size * 2).to2ByteInt()) // Start of state x
		position += length
	}
	result.addAll((position + statesLength.size * 2).to2ByteInt()) // Actions start
	position++
	result.addAll(statesResult) // States

	val actionsLength = mutableListOf<Int>()
	val actionsResult = mutableListOf<Int>()

	actions.map { it.toData(this, devices, variables) }.takeIf { it.isNotEmpty() }?.forEach { actionResult ->
		actionsLength.add(actionResult.size)
		actionsResult.addAll(actionResult)
	}

	result.addAll(actionsLength.size.to2ByteInt()) // All action count
	position = result.size
	actionsLength.forEach { length ->
		result.addAll((position + actionsLength.size * 2).to2ByteInt()) // Start of action y
		position += length
	}
	result.addAll(actionsResult) // Actions
	return result
}

fun List<Int>.toStateMachine(states: List<State>, devices: List<Device>, variables: List<Variable>): List<State> {
	// this[0] // Reserved for internal state machine status
	val stateCount = this[1] // 1 bytes (max 255 states)
	val stateStarts = (0 until stateCount).map { it * 2 + 2 }.map { this[it] * 256 + this[it + 1] }
	val actionsStart = (stateCount * 2 + 2).let { this[it] * 256 + this[it + 1] }
	val actionCount = this[actionsStart] * 256 + this[actionsStart + 1]
	val actionStarts = (0 until actionCount).map { it * 2 + actionsStart + 2 }.map { this[it] * 256 + this[it + 1] }
	val actions = actionStarts.map { start -> this.toAction(start, states, devices, variables) }

	return stateStarts.mapIndexed { stateId, stateStart ->
		val evaluationCount = this[stateStart]
		var evaluationStart = stateStart + 1
		val evaluations = (0 until evaluationCount).map { _ ->
			val evalConditions = (0 until 5)
					.map { it to (evaluationStart + it * 2) }
					.flatMap { (conditionId, position) ->
						val cond = this[position]
						val mask = this[position + 1]
						(0 until 8)
								.filter { (mask and (1 shl it)) > 0 }
								.map { it to ((cond and (1 shl it)) > 0) }
								.map { (bit, value) ->
									when (conditionId) {
										in (0 until 4) -> Device("", DeviceKind.MCP23017, "${conditionId * 8 + bit}")
										4 -> when (bit) {
											0 -> Device("", DeviceKind.BLUETOOTH, BluetoothKey.CONNECTED.key)
											else -> Device("", DeviceKind.VIRTUAL, "unknown")
										}
										else -> Device("", DeviceKind.VIRTUAL, "unknown")
									} to Variable("", VariableType.BOOLEAN, if (value) "true" else "false")
								}
					}
					.map { (device, variable) -> device.findName(devices) to variable.findName(variables) }
					.map { (device, variable) -> Condition(device = device.toData(), value = variable.name) }

			val evalActionCount = this[evaluationStart + 5 * 2]
			val evalActions = (0 until evalActionCount)
					.map { this[evaluationStart + 5 * 2 + it * 2 + 1] * 256 + this[evaluationStart + 5 * 2 + it * 2 + 2] }
					.mapNotNull { actions.getOrNull(it) }
			evaluationStart += 5 * 2 + evalActionCount * 2 + 1
			Evaluation(conditions = evalConditions, actions = evalActions)
		}
		State(states.getOrNull(stateId)?.name ?: "${stateId}", evaluations)
	}
}

fun Action.toData(states: List<State>, devices: Collection<Device>, variables: Collection<Variable>): List<Int> = device
		?.let { d -> value?.let { v -> d to v } }
		?.let { (deviceId, value) ->
			val result = mutableListOf<Int>()
			val device = deviceId.toDevice(devices)

			// Action Device kind-key
			val variable: Variable? = value.toVariable(variables)
			if (variable != null) {
				result.add(device.toData() or (if (includingEnteringState) 0x80 else 0x00))
				result.addAll(variable.toData(states)) // FIXME
			} else {
				throw RuntimeException("Cannot determine variable")
			}
			result
		}
		?: emptyList()

fun List<Int>.toAction(start: Int = 0, states: List<State>, devices: List<Device>, variables: List<Variable>): Action = start
		.let { position -> (position + 1) to this[position] }
		.let { (position, deviceByte) -> Triple(position, deviceByte and 0x80 == 0x80, (deviceByte and 0x7F).toDevice(devices)) }
		.let { (position, includingEntering, device) -> Triple(device, includingEntering, this.subList(position, this.size).toVariable(states, device, variables)) }
		.let { (device, includingEntering, variable) -> Action(device.toData(), variable?.name, includingEntering) }

fun List<Int>.toActions(states: List<State>, devices: List<Device>, variables: List<Variable>): List<Action> {
	var position = 0
	val size = this[position++]
	val actions = mutableListOf<Action>()

	(0 until size).forEach { _ ->
		val device = this[position++].toDevice(devices)
		val length = this[position] // Length will be also part of the data
		val data = this.subList(position, position + length + 1) // Length is part of the data
		val variable = data.toVariable(states, device, variables)
		actions.add(Action(device = device.toData(), value = variable?.name))
		position += length + 1 // Data (length) + the length byte itself
	}
	return actions
}

fun Device.toData() : Int = when (kind) {
	DeviceKind.MCP23017 -> key.toInt()    // 0x00 - 0x27
	DeviceKind.WS281x -> key.toInt() + 40 // 0x28 - 0x47
	DeviceKind.LCD -> when (LcdKey.get(key.toInt())) {
		LcdKey.MESSAGE -> 80              // 0x50
		LcdKey.BACKLIGHT -> 81            // 0x51
		LcdKey.RESET -> 82                // 0x52
		LcdKey.CLEAR -> 83                // 0x53
		else -> 95                        // 0x5F
	}
	DeviceKind.BLUETOOTH -> when (BluetoothKey.get(key)) {
		BluetoothKey.CONNECTED -> 96      // 0x60
		BluetoothKey.TRIGGER -> 97        // 0x61
		else -> 111                       // 0x6F
	}
	DeviceKind.VIRTUAL -> when (VirtualKey.get(key)) {
		VirtualKey.GOTO -> 112            // 0x70
		VirtualKey.ENTER -> 113           // 0x71
		else -> 127                       // 0x7F
	}
}

fun Int.toDevice(devices: Collection<Device>) = when (this) {
	in (0 until 40) -> Device(kind = DeviceKind.MCP23017, key = "${this}")      // 0x00 - 0x27
	in (40 until 72) -> Device(kind = DeviceKind.WS281x, key = "${this - 40}")  // 0x28 - 0x47
	80 -> Device(kind = DeviceKind.LCD, key = "${LcdKey.MESSAGE.key}")          // 0x50
	81 -> Device(kind = DeviceKind.LCD, key = "${LcdKey.BACKLIGHT.key}")        // 0x51
	82 -> Device(kind = DeviceKind.LCD, key = "${LcdKey.RESET.key}")            // 0x52
	83 -> Device(kind = DeviceKind.LCD, key = "${LcdKey.CLEAR.key}")            // 0x53
	96 -> Device(kind = DeviceKind.BLUETOOTH, key = BluetoothKey.CONNECTED.key) // 0x60
	97 -> Device(kind = DeviceKind.BLUETOOTH, key = BluetoothKey.TRIGGER.key)   // 0x61
	112 -> Device(kind = DeviceKind.VIRTUAL, key = VirtualKey.GOTO.key)         // 0x70
	113 -> Device(kind = DeviceKind.VIRTUAL, key = VirtualKey.ENTER.key)        // 0x71
	else -> Device(kind = DeviceKind.VIRTUAL, key = "unknown")
}.findName(devices)

fun Variable.toData(states: List<State>): List<Int> = listOf(when (type) { // Variable value length
	VariableType.BOOLEAN -> 1           // 0x01
	VariableType.STRING -> value.replace("\\n", "\n").length // 0x01 - 0xFF
	VariableType.COLOR_PATTERN -> 8     // 0x08
	VariableType.STATE -> 1             // 0x01
	VariableType.ENTER -> value.split("|", limit = 3)
			.mapIndexed { i, s -> if (i == 0) 1 else s.length }
			.reduce { acc, i -> acc + i } + 2
}, *when (type) { // Variable value
	VariableType.BOOLEAN -> arrayOf(if (value.toBoolean()) 1 else 0)
	VariableType.STRING -> value.replace("\\n", "\n").toCharArray().map { it.toInt() }.toTypedArray()
	VariableType.COLOR_PATTERN -> value
			.split(",")
			.takeIf { it.size >= 5 }
			?.mapIndexed { i, v ->
				when (i) {
					0 -> listOf(v.toIntOrNull() ?: 0)
					1 -> v.toRGBColor()?.let { listOf(it.red, it.green, it.blue) } ?: listOf(0, 0, 0)
					2 -> v.toIntOrNull()?.let { listOf(it / 256, it % 256) } ?: listOf(0, 0)
					in (3..4) -> listOf(v.split("x").getOrNull(1)?.toIntOrNull(16) ?: 0)
					else -> null
				}
			}
			?.filterNotNull()
			?.flatten()
			?.takeIf { it.size == 8 }
			?.toTypedArray()
			?: arrayOf(0, 0, 0, 0, 0, 10, 0, 32)
	VariableType.STATE -> arrayOf(states.map { it.name }.indexOf(value))
	VariableType.ENTER -> value.split("|", limit = 3)
			.mapIndexed { i, s -> if (i == 0) listOf(s.toInt()) else (listOf(0x1E) + s.toCharArray().map { it.toInt() }) }
			.flatten()
			.toTypedArray()
})

fun List<Int>.toVariable(states: List<State>, device: Device, variables: List<Variable>) = toVariable(states, device.kind, device.key, variables)

fun List<Int>.toVariable(states: List<State>, deviceKind: DeviceKind, deviceKey: String, variables: List<Variable>) = getOrNull(0)
		?.let { len -> (1..len).map { this[it] } }
		?.let { data ->
			when (deviceKind) {
				DeviceKind.MCP23017 -> Variable(
						type = VariableType.BOOLEAN,
						value = if ((data.getOrNull(0) ?: 0) > 0) "true" else "false")
				DeviceKind.WS281x -> Variable(
						type = VariableType.COLOR_PATTERN,
						value = data.takeIf { it.size >= 8 }
								?.let { "%d,#%02X%02X%02X,%d,0x%02X,0x%02X".format(it[0], it[1], it[2], it[3], it[4] * 256 + it[5], it[6], it[7]) }
								?: "0,#000000,10,0x00,0x20")
				DeviceKind.LCD -> when (LcdKey.get(deviceKey.toInt())) {
					LcdKey.MESSAGE -> Variable(
							type = VariableType.STRING,
							value = data
									.map { it.toChar() }
									.toCharArray()
									.let { String(it) }
									.replace("\n", "\\n"))
					else -> Variable(
							type = VariableType.BOOLEAN,
							value = if ((data.getOrNull(0) ?: 0) > 0) "true" else "false")
				}
				DeviceKind.BLUETOOTH -> Variable(
						type = VariableType.BOOLEAN,
						value = if ((data.getOrNull(0) ?: 0) > 0) "true" else "false")
				DeviceKind.VIRTUAL -> when (VirtualKey.get(deviceKey)) {
					VirtualKey.GOTO -> Variable(
							type = VariableType.STATE,
							value = data.getOrNull(0)?.let { states.getOrNull(it)?.name ?: "${it}" } ?: "0")
					VirtualKey.ENTER -> Variable(
							type = VariableType.ENTER,
							value = data.toEnterString()
					)
					else -> Variable(
							type = VariableType.BOOLEAN,
							value = if ((data.getOrNull(0) ?: 0) > 0) "true" else "false")
				}
			}
		}
		?.findName(variables)

private fun List<Int>.toEnterString(): String {
	var title = ""
	var pattern = ""

	assert(this[1] == 0x1E) // RS (Record Separator)
	var i = 2
	while (this[i] != 0x1E) title += this[i++].toChar()
	assert(this[i++] == 0x1E)
	while (i < this.size) pattern += this[i++].toChar()
	return "${this[0]}|${title}|${pattern}"
}


fun Device.findName(devices: Collection<Device>) = apply {
	name = when {
		kind == DeviceKind.VIRTUAL && key == VirtualKey.GOTO.key -> "GOTO"
		else -> devices.find { it.kind == kind && it.key == key }?.name ?: ""
	}
}

fun Variable.findName(variables: Collection<Variable>) = apply {
	name = variables.find { it.type == type && it.value == value }?.name ?: ""
}

fun findInStateMachine(filter: (StateMachineItem) -> Boolean): List<StateMachineItem> =
		findInStateMachine(filter, SharedUiData.stateMachine)

private fun findInStateMachine(filter: (StateMachineItem) -> Boolean, states: List<State>):
		List<StateMachineItem> = states.flatMap { it.children() }.filter { filter(it) }

private fun StateMachineItem.children(): List<StateMachineItem> = listOf(this) + when (this) {
	is State -> evaluations.flatMap { it.children() }
	is Evaluation -> conditions.flatMap { it.children() } + actions.flatMap { it.children() }
	is Condition -> listOf(device?.toDevice(SharedUiData.devices), value?.toVariable(SharedUiData.variables)).filterIsInstance<StateMachineItem>()
	is Action -> listOf(device?.toDevice(SharedUiData.devices), value?.toVariable(SharedUiData.variables)).filterIsInstance<StateMachineItem>()
	else -> emptyList()
}


fun Int.to2ByteInt() = listOf(floor(this / 256.0).toInt(), this % 256)
//.also { println("${this} -> ${it[0]}, ${it[1]} | ${"%02X".format(this)} -> ${"%02X".format(it[0])}, ${"%02X".format(it[1])}")  }

fun Int.to2Byte() = to2ByteInt().map { it.toByte() }

fun Int.to2ByteArray() = to2Byte().toByteArray()