package com.poterion.monitor.notifiers.deploymentcase.data

import javafx.beans.property.ListProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.scene.control.TreeItem

object SharedUiData {
    val jobStatusColors: ObservableMap<String, Variable?> = FXCollections.observableHashMap()

    val jobColors: List<Variable>
        get() = jobStatusColors.values.filterNotNull()

    val pipelineStatusTargetStates: ObservableMap<String, State?> = FXCollections.observableHashMap()

    val pipelineTargetStates: List<State>
        get() = pipelineStatusTargetStates.values.filterNotNull()

    val devices: ObservableList<Device> = FXCollections.observableArrayList()
    val devicesProperty: ListProperty<Device> = SimpleListProperty(devices)
    val variables: ObservableList<Variable> = FXCollections.observableArrayList()
    val variablesProperty: ListProperty<Variable> = SimpleListProperty(variables)

    private val _stateMachine: ObservableList<TreeItem<StateMachineItem>> = FXCollections.observableArrayList()
    val stateMachineProperty: ListProperty<TreeItem<StateMachineItem>> = SimpleListProperty(_stateMachine)

    val stateMachine: List<State>
        get() = _stateMachine.mapNotNull { it.value as? State }

    fun findInStateMachine(filter: (StateMachineItem) -> Boolean): List<StateMachineItem> =
            findInStateMachine(filter, _stateMachine)

    private fun findInStateMachine(filter: (StateMachineItem) -> Boolean,
                                   children: List<TreeItem<StateMachineItem>>):
            List<StateMachineItem> = listOfNotNull(
            *(children.filter { filter(it.value) }.mapNotNull { it.value }
                    + children.map { it.children }.map { findInStateMachine(filter, it) }.flatten())
                    .toTypedArray())


    val maxColorComponent: Int
        get() = SharedUiData
                .variables
                .asSequence()
                .filter { it.type == VariableType.COLOR_PATTERN }
                .map { it.value.split(",").subList(1, 4) }
                .flatten()
                .map { it.toInt() }
                .max()
                ?: 255
}