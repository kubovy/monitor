package com.poterion.monitor.notifiers.deploymentcase.control

import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon
import com.poterion.monitor.notifiers.deploymentcase.data.*
import com.poterion.monitor.notifiers.deploymentcase.ui.expandAll
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView

fun List<State>.toByteArray(): ByteArray {
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
								32 -> condition.device?.let { it.kind == DeviceKind.BLUETOOTH && it.key == BluetoothKey.CONNECTED.key } == true
								else -> condition.device?.let { it.kind == DeviceKind.MCP23017 && it.key == "${bit}" } == true
							}
						}
						?.let {
							it.value?.value?.toBoolean() == true
						}
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
					it.device?.kind == action.device?.kind
							&& it.device?.key == action.device?.key
							&& it.value?.value == action.value?.value
				}
				if (index == -1) {
					stateResult.addAll(actions.size.to2Byte()) // New action index
					actions.add(action)
				} else {
					stateResult.addAll(index.to2Byte())        // Existing action index
				}
			}
		}

		statesLength.add(stateResult.size)
		statesResult.addAll(stateResult)
	}

	val result = mutableListOf<Int>()
	result.addAll(statesLength.size.to2Byte()) // State count
	var position = 4
	statesLength.forEach { length ->
		result.addAll((position + statesLength.size * 2).to2Byte()) // Start of state x
		position += length
	}
	result.addAll((position + statesLength.size * 2).to2Byte()) // Actions start
	position++
	result.addAll(statesResult) // States

	val actionsLength = mutableListOf<Int>()
	val actionsResult = mutableListOf<Int>()

//	BufferedWriter(FileWriter("a1.list")).use { writer ->
//		actions.forEach { action ->
//			writer.write("${action}\n")
//		}
//	}

	actions.map { it.toIntList(this) }.takeIf { it.isNotEmpty() }?.forEach { actionResult ->
		actionsLength.add(actionResult.size)
		actionsResult.addAll(actionResult)
	}

	result.addAll(actionsLength.size.to2Byte()) // All action count
	position = result.size
	actionsLength.forEach { length ->
		result.addAll((position + actionsLength.size * 2).to2Byte()) // Start of action y
		position += length
	}
	result.addAll(actionsResult) // Actions
	return result.map { it.toByte() }.toByteArray()
}

fun ByteArray.toStateMachine(devices: List<Device> = emptyList(),
							 variables: List<Variable> = emptyList()): List<State> = this
		.map { it.toUByte() }
		.map { it.toInt() }
		.toStateMachine(devices, variables)

fun List<Int>.toStateMachine(devices: List<Device> = emptyList(),
							 variables: List<Variable> = emptyList()): List<State> {
	val stateCount = (this[0] * 256) + this[1] // 2 bytes
	val stateStarts = (0 until stateCount).map { it * 2 + 2 }.map { this[it] * 256 + this[it + 1] }
	val actionsStart = (stateCount * 2 + 2).let { this[it] * 256 + this[it + 1] }
	val actionCount = this[actionsStart] * 256 + this[actionsStart + 1]
	val actionStarts = (0 until actionCount).map { it * 2 + actionsStart + 2 }.map { this[it] * 256 + this[it + 1] }
	val actions = actionStarts.map { start -> this.toAction(start, devices, variables) }

//	BufferedWriter(FileWriter("a2.list")).use { writer ->
//		actions.forEach { action ->
//			writer.write("${action}\n")
//		}
//	}

	return stateStarts.mapIndexed { stateId, stateStart ->
		val evaluationCount = this[stateStart]
		var evaluationStart = stateStart + 1
		val evaluations = (0 until evaluationCount).map {
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
					.map { (device, variable) -> Condition(device = device, value = variable) }

			val evalActionCount = this[evaluationStart + 5 * 2]
			val evalActions = (0 until evalActionCount)
					.map { this[evaluationStart + 5 * 2 + it * 2 + 1] * 256 + this[evaluationStart + 5 * 2 + it * 2 + 2] }
					.map { actions[it] }
			evaluationStart += 5 * 2 + evalActionCount * 2 + 1
			Evaluation(conditions = evalConditions, actions = evalActions)
		}
		State("${stateId}", evaluations)
	}
}

fun TreeView<StateMachineItem>.getStateMachine() = root.children
		.map { it to (it.value as State) }
		.map { (stateTreeItem, state) ->
			state.apply {
				evaluations = stateTreeItem
						.children
						.mapNotNull { evaluation ->
							(evaluation.value as? Evaluation)?.apply {
								conditions = evaluation.children.filter { (it.value as? Placeholder)?.title == "Conditions" }
										.flatMap { placeholder -> placeholder.children.map { it.value as? Condition } }
										.filterNotNull()
								actions = evaluation.children.filter { (it.value as? Placeholder)?.title == "Actions" }
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

fun Action.toByteArray(states: List<State> = emptyList()) : ByteArray = toIntList(states)
		.map { it.toByte() }
		.toByteArray()

fun Action.toIntList(states: List<State> = emptyList()): List<Int> = device?.let { d -> value?.let { v -> d to v } }?.let { (device, value) ->
	val result = mutableListOf<Int>()

	// Action Device kind-key
	result.add(device.toInt())

	// Action value length
	result.add(when (value.type) {
		VariableType.BOOLEAN -> 1                 // 0x01
		VariableType.STRING -> value.value.length // 0x01 - 0xFF
		VariableType.COLOR_PATTERN -> 4           // 0x04
		VariableType.STATE -> 1                   // 0x01
	})

	// Action value
	result.addAll(when (value.type) {
		VariableType.BOOLEAN -> listOf(if (value.value.toBoolean()) 1 else 0)
		VariableType.STRING -> value.value.toCharArray().map { it.toInt() }
		VariableType.COLOR_PATTERN -> value.value.split(",").map { it.toInt() }
		VariableType.STATE -> listOf(states.map { it.name }.indexOf(value.value))
	})

	result
} ?: emptyList()

fun List<Int>.toAction(start: Int = 0, devices: List<Device>, variables: List<Variable>): Action = start
		.let { position -> (position + 1) to this[position].toDevice() }
		.let { (position, device) -> device to this.subList(position, this.size).toVariable(device) }
		.let { (device, variable) -> device.findName(devices) to variable.findName(variables) }
		.let { (device, variable) -> Action(device, variable) }

fun Int.toDevice() = when (this) {
	in (0 until 40) -> Device(kind = DeviceKind.MCP23017, key = "${this}")      // 0x00 - 0x27
	in (40 until 72) -> Device(kind = DeviceKind.WS281x, key = "${this - 40}")  // 0x28 - 0x47
	80 -> Device(kind = DeviceKind.LCD, key = LcdKey.MESSAGE.key)               // 0x50
	81 -> Device(kind = DeviceKind.LCD, key = LcdKey.BACKLIGHT.key)             // 0x51
	82 -> Device(kind = DeviceKind.LCD, key = LcdKey.RESET.key)                 // 0x52
	83 -> Device(kind = DeviceKind.LCD, key = LcdKey.CLEAR.key)                 // 0x53
	96 -> Device(kind = DeviceKind.BLUETOOTH, key = BluetoothKey.CONNECTED.key) // 0x60
	97 -> Device(kind = DeviceKind.BLUETOOTH, key = BluetoothKey.TRIGGER.key)   // 0x61
	192 -> Device(kind = DeviceKind.VIRTUAL, key = VirtualKey.GOTO.key)         // 0xC0
	else -> Device(kind = DeviceKind.VIRTUAL, key = "unknown")
}

fun Device.toInt() = when (kind) {
	DeviceKind.MCP23017 -> key.toInt()
	DeviceKind.WS281x -> key.toInt() + 40 // 0x28
	DeviceKind.LCD -> when (LcdKey.get(key)) {
		LcdKey.MESSAGE -> 80   // 0x50
		LcdKey.BACKLIGHT -> 81 // 0x51
		LcdKey.RESET -> 82     // 0x52
		LcdKey.CLEAR -> 83     // 0x53
		else -> 95             // 0x5F
	}
	DeviceKind.BLUETOOTH -> when (BluetoothKey.get(key)) {
		BluetoothKey.CONNECTED -> 96 // 0x60
		BluetoothKey.TRIGGER -> 97   // 0x61
		else -> 191                  // 0xBF
	}
	DeviceKind.VIRTUAL -> when (VirtualKey.get(key)) {
		VirtualKey.GOTO -> 192 // 0xC0
		else -> 255            // 0xFF
	}
}

fun ByteArray.toVariable(device: Device) = toVariable(device.kind, device.key)

fun ByteArray.toVariable(deviceKind: DeviceKind, deviceKey: String) = map { it.toUByte().toInt() }
		.toVariable(deviceKind, deviceKey)

fun List<Int>.toVariable(device: Device) = toVariable(device.kind, device.key)

fun List<Int>.toVariable(deviceKind: DeviceKind, deviceKey: String) = this[0]
		.let { len -> (1..len).map { this[it] } }
		.let { data ->
			when (deviceKind) {
				DeviceKind.MCP23017 -> Variable("", VariableType.BOOLEAN,
						if (data[0] > 0) "true" else "false")
				DeviceKind.WS281x -> Variable("", VariableType.COLOR_PATTERN,
						"${data[0]},${data[1]},${data[2]},${data[3]}")
				DeviceKind.LCD -> when (LcdKey.get(deviceKey)) {
					LcdKey.MESSAGE -> data
							.map { it.toChar() }
							.toCharArray()
							.let { String(it) }
							.let { Variable("", VariableType.STRING, it) }
					else -> Variable("", VariableType.BOOLEAN,
							if (data[0] > 0) "true" else "false")
				}
				DeviceKind.BLUETOOTH -> Variable("", VariableType.BOOLEAN,
						if (data[0] > 0) "true" else "false")
				DeviceKind.VIRTUAL -> when (VirtualKey.get(deviceKey)) {
					VirtualKey.GOTO -> Variable("", VariableType.STATE,
							"${data[0]}")
					else -> Variable("", VariableType.BOOLEAN,
							if (data[0] > 0) "true" else "false")
				}
			}
		}

fun Variable.toIntList(states: List<State>): List<Int> = listOf(when (type) { // Action value length
	VariableType.BOOLEAN -> 1                       // 0x01
	VariableType.STRING -> value.length // 0x01 - 0xFF
	VariableType.COLOR_PATTERN -> 4                 // 0x04
	VariableType.STATE -> 1                         // 0x01
}, *when (type) {
	VariableType.BOOLEAN -> arrayOf(if (value.toBoolean()) 1 else 0)
	VariableType.STRING -> value.toCharArray().map { it.toInt() }.toTypedArray()
	VariableType.COLOR_PATTERN -> value.split(",").map { it.toInt() }.toTypedArray()
	VariableType.STATE -> arrayOf(states.map { it.name }.indexOf(value))
})

fun Device.findName(devices: List<Device>) = apply {
	name = devices.find { it.kind == kind && it.key == key }?.name ?: ""
}

fun Variable.findName(variables: List<Variable>) = apply {
	name = variables.find { it.type == type && it.value == value }?.name ?: ""
}

private fun Int.to2Byte() = listOf(Math.floor(this / 256.0).toInt(), this % 256)
//.also { println("${this} -> ${it[0]}, ${it[1]} | ${"%02X".format(this)} -> ${"%02X".format(it[0])}, ${"%02X".format(it[1])}")  }