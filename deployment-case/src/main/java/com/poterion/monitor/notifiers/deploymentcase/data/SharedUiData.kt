/******************************************************************************
 * Copyright (C) 2020 Jan Kubovy (jan@kubovy.eu)                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.      *
 ******************************************************************************/
package com.poterion.monitor.notifiers.deploymentcase.data

import com.poterion.monitor.notifiers.deploymentcase.control.toDevice
import com.poterion.utils.kotlin.noop
import javafx.beans.property.*
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableMap

object SharedUiData {

	val configurationProperty: ObjectProperty<Configuration> = SimpleObjectProperty()

	val nameProperty: StringProperty = SimpleStringProperty()
	val isActiveProperty: BooleanProperty = SimpleBooleanProperty()

	val devices: ObservableList<Device>?
		get() = configurationProperty.get()?.devices

	val variables: ObservableList<Variable>?
		get() = configurationProperty.get()?.variables

	val jobStatus: ObservableMap<String, String>?
		get() = configurationProperty.get()?.jobStatus

	val pipelineStatus: ObservableMap<String, String>?
		get() = configurationProperty.get()?.pipelineStatus

	val stateMachine: ObservableList<State>?
		get() = configurationProperty.get()?.stateMachine

	var isUpdateInProgress = false

	init {
		val stateMachineChangeListener = ListChangeListener<State> { change ->
			while (change.next()) when {
				change.wasPermutated() -> noop()
				change.wasUpdated() -> noop()
				else -> configurationProperty.get()?.also { configuration ->
					configuration.variables.also { variables ->
						variables.removeIf { it.type == VariableType.STATE }
						variables.addAll(change.list.map { Variable("state_${it.name}", VariableType.STATE, it.name) })
					}
				}
			}
		}

		configurationProperty.addListener { _, old, new ->
			isUpdateInProgress = true

			new?.devices?.setAll(((0..71) + (80..83) + (96..97) + (112..115)).map { it.toDevice(new.devices) })

			if (new?.variables
							?.filter { it.type == VariableType.BOOLEAN }
							?.takeIf { it.size == 2 }
							?.map { it.value }
							?.takeIf { it.containsAll(listOf("true", "false")) } == null) {
				new?.variables?.removeIf { it.type == VariableType.BOOLEAN }
				new?.variables?.addAll(
						Variable(name = "true", type = VariableType.BOOLEAN, value = "true"),
						Variable(name = "false", type = VariableType.BOOLEAN, value = "false"))
			}
			new?.variables?.removeIf { it.type == VariableType.STATE }
			new?.variables?.addAll(new.stateMachine.map { Variable("state_${it.name}", VariableType.STATE, it.name) })

			nameProperty.set(new?.name ?: "")
			isActiveProperty.set(new?.isActive ?: false)

			old?.stateMachine?.removeListener(stateMachineChangeListener)
			new?.stateMachine?.addListener(stateMachineChangeListener)

			isUpdateInProgress = false
		}
	}
}