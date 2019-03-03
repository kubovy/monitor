package com.poterion.monitor.notifiers.deploymentcase.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon
import com.poterion.monitor.notifiers.deploymentcase.getDisplayName

data class Action(var device: Device? = null,
				  var value: Variable? = null) : StateMachineItem {
	override val title: String
		@JsonIgnore
		get() = "${device.getDisplayName()} = ${value?.getDisplayName()}"

	override val icon: DeploymentCaseIcon?
		@JsonIgnore
		get() = DeploymentCaseIcon.ACTION

	override fun isBinarySame(other: StateMachineItem): Boolean = other is Action
			&& device?.kind == other.device?.kind
			&& device?.key == other.device?.key
			&& value?.type == other.value?.type
			&& value?.value == other.value?.value
}