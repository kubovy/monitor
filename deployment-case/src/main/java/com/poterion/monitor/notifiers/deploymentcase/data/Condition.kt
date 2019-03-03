package com.poterion.monitor.notifiers.deploymentcase.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon
import com.poterion.monitor.notifiers.deploymentcase.getDisplayName

data class Condition(var device: Device? = null,
					 var value: Variable? = null) : StateMachineItem {
	override val title: String
		@JsonIgnore
		get() = "${device.getDisplayName()} == ${value?.name}"

	override val icon: DeploymentCaseIcon?
		@JsonIgnore
		get() = DeploymentCaseIcon.CONDITION

	override fun isBinarySame(other: StateMachineItem): Boolean = (other is Condition
			&& device?.kind == other.device?.kind
			&& device?.key == other.device?.key
			&& value?.type == other.value?.type
			&& value?.value == other.value?.value)
			|| (other is Action
			&& device?.kind == other.device?.kind
			&& device?.key == other.device?.key
			&& value?.type == other.value?.type
			&& value?.value == other.value?.value)
}