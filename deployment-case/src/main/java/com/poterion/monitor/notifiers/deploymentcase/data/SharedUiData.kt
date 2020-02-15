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
import javafx.beans.Observable
import javafx.beans.property.*
import javafx.collections.*

object SharedUiData {

	val configurationProperty: ObjectProperty<Configuration> = SimpleObjectProperty()

	val nameProperty: StringProperty = SimpleStringProperty()
	val isActiveProperty: BooleanProperty = SimpleBooleanProperty()

	val devicesProperty: ObjectProperty<ObservableList<Device>> = SimpleObjectProperty(FXCollections.observableArrayList())
	val devices: ObservableList<Device>
		get() = devicesProperty.get()

	val variablesProperty: ObjectProperty<ObservableList<Variable>> = SimpleObjectProperty(FXCollections.observableArrayList())
	val variables: ObservableList<Variable>
		get() = variablesProperty.get()

	val jobStatusProperty: ObjectProperty<ObservableMap<String, String?>> = SimpleObjectProperty(FXCollections.observableHashMap())
	val jobStatus: ObservableMap<String, String?>
		get() = jobStatusProperty.get()

	val pipelineStatusProperty: ObjectProperty<ObservableMap<String, String?>> = SimpleObjectProperty(FXCollections.observableHashMap())
	val pipelineStatus: ObservableMap<String, String?>
		get() = pipelineStatusProperty.get()

	val stateMachineProperty: ObjectProperty<ObservableList<State>> = SimpleObjectProperty(FXCollections.observableArrayList())
	val stateMachine: ObservableList<State>
		get() = stateMachineProperty.get()

	var isUpdateInProgress = false

	init {

		val namePropertyListener = { _: Observable, _: String, name: String -> configurationProperty.get()?.name = name }
		val isActivePropertyListener = { _: Observable, _: Boolean, isActive: Boolean -> configurationProperty.get()?.isActive = isActive }
		val devicesChangeListener = ListChangeListener<Device> { configurationProperty.get()?.devices = it.list }
		val variableChangeListener = ListChangeListener<Variable> { configurationProperty.get()?.variables = it.list }
		val jobStatusChangeListener = MapChangeListener<String, String?> {
			configurationProperty.get()?.jobStatus = it.map.mapNotNull { (k, v) -> v?.let { k to it } }.toMap()
		}
		val pipelineStatusChangeListener = MapChangeListener<String, String?> {
			configurationProperty.get()?.pipelineStatus = it.map.mapNotNull { (k, v) -> v?.let { k to it } }.toMap()
		}
		val stateMachineChangeListener = ListChangeListener<State> { change ->
			configurationProperty.get()?.stateMachine = change.list
			variables.removeIf { it.type == VariableType.STATE }
			variables.addAll(change.list.map { Variable("state_${it.name}", VariableType.STATE, it.name) })
		}

		configurationProperty.addListener { _, _, configuration ->
			isUpdateInProgress = true

			nameProperty.removeListener(namePropertyListener)
			isActiveProperty.removeListener(isActivePropertyListener)
			devices.removeListener(devicesChangeListener)
			variables.removeListener(variableChangeListener)
			jobStatus.removeListener(jobStatusChangeListener)
			pipelineStatus.removeListener(pipelineStatusChangeListener)
			stateMachine.removeListener(stateMachineChangeListener)

			nameProperty.set(configuration?.name ?: "")
			nameProperty.addListener(namePropertyListener)

			isActiveProperty.set(configuration?.isActive ?: false)
			isActiveProperty.addListener(isActivePropertyListener)

			devicesProperty.set(FXCollections.observableList(configuration?.devices?.toMutableList()
					?: mutableListOf()))
			devices.addListener(devicesChangeListener)
			devices.setAll(((0..71) + (80..83) + (96..97) + (112..115)).map { it.toDevice(devices) })

			variablesProperty.set(FXCollections.observableList(configuration?.variables?.toMutableList()
					?: mutableListOf()))
			variables.addListener(variableChangeListener)
			if (variables.filtered { it.type == VariableType.BOOLEAN }.takeIf { it.size == 2 }?.map { it.value }
							?.takeIf { it.containsAll(listOf("true", "false")) } == null) {
				variables.removeIf { it.type == VariableType.BOOLEAN }
				variables.addAll(
						Variable(name = "true", type = VariableType.BOOLEAN, value = "true"),
						Variable(name = "false", type = VariableType.BOOLEAN, value = "false"))
			}

			jobStatusProperty.set(FXCollections.observableMap(configuration?.jobStatus?.toMutableMap<String, String?>()
					?: mutableMapOf()))
			jobStatus.addListener(jobStatusChangeListener)

			pipelineStatusProperty.set(FXCollections.observableMap(configuration?.pipelineStatus?.toMutableMap<String, String?>()
					?: mutableMapOf()))
			pipelineStatus.addListener(pipelineStatusChangeListener)

			stateMachineProperty.set(FXCollections.observableList(configuration?.stateMachine?.toMutableList()
					?: mutableListOf()))
			stateMachine.addListener(stateMachineChangeListener)
			variables.removeIf { it.type == VariableType.STATE }
			variables.addAll(stateMachine.map { Variable("state_${it.name}", VariableType.STATE, it.name) })

			isUpdateInProgress = false
		}
	}
}