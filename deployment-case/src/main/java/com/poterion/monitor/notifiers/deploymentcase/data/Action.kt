package com.poterion.monitor.notifiers.deploymentcase.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon
import com.poterion.monitor.notifiers.deploymentcase.control.toDevice
import com.poterion.monitor.notifiers.deploymentcase.getDisplayName
import com.poterion.monitor.notifiers.deploymentcase.toVariable

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class Action(var device: Int? = null,
				  var value: String? = null,
				  var includingEnteringState: Boolean = false) : StateMachineItem {

	@JsonIgnore
	override fun getTitle(devices: Collection<Device>, variables: Collection<Variable>): String =
			"${device?.toDevice(devices)?.getDisplayName()} = ${value?.toVariable(variables)?.getDisplayName()}" +
					(if (includingEnteringState) " (including entering state)" else "")

	override val icon: DeploymentCaseIcon?
		@JsonIgnore
		get() = DeploymentCaseIcon.ACTION

	override fun isBinarySame(other: StateMachineItem, devices: Collection<Device>, variables: Collection<Variable>):
			Boolean = other is Action
			&& device?.toDevice(devices)?.kind == other.device?.toDevice(devices)?.kind
			&& device?.toDevice(devices)?.key == other.device?.toDevice(devices)?.key
			&& value?.toVariable(variables)?.type == other.value?.toVariable(variables)?.type
			&& value?.toVariable(variables)?.value == other.value?.toVariable(variables)?.value
			&& includingEnteringState == other.includingEnteringState
}