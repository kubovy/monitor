package com.poterion.monitor.notifiers.deploymentcase.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon
import com.poterion.monitor.notifiers.deploymentcase.control.toDevice
import com.poterion.monitor.notifiers.deploymentcase.getDisplayName
import com.poterion.monitor.notifiers.deploymentcase.toVariable

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class Condition(var device: Int? = null,
					 var value: String? = null) : StateMachineItem {

	@JsonIgnore
	override fun getTitle(devices: Collection<Device>, variables: Collection<Variable>): String =
			"${device?.toDevice(devices)?.getDisplayName()} == ${value?.toVariable(variables)?.getDisplayName()}"

	override val icon: DeploymentCaseIcon?
		@JsonIgnore
		get() = DeploymentCaseIcon.CONDITION

	override fun isBinarySame(other: StateMachineItem, devices: Collection<Device>, variables: Collection<Variable>):
			Boolean = (other is Condition
			&& device?.toDevice(devices)?.kind == other.device?.toDevice(devices)?.kind
			&& device?.toDevice(devices)?.key == other.device?.toDevice(devices)?.key
			&& value?.toVariable(variables)?.type == other.value?.toVariable(variables)?.type
			&& value?.toVariable(variables)?.value == other.value?.toVariable(variables)?.value)
			|| (other is Action
			&& device?.toDevice(devices)?.kind == other.device?.toDevice(devices)?.kind
			&& device?.toDevice(devices)?.key == other.device?.toDevice(devices)?.key
			&& value?.toVariable(variables)?.type == other.value?.toVariable(variables)?.type
			&& value?.toVariable(variables)?.value == other.value?.toVariable(variables)?.value)
}