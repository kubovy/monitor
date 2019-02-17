package com.poterion.monitor.notifiers.deploymentcase.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon
import com.poterion.monitor.notifiers.deploymentcase.getDisplayName

data class Condition(var device: Device? = null, var value: Variable? = null) : StateMachineItem {
	override val title: String
		@JsonIgnore
		get() = "${device.getDisplayName()} == ${value?.name}"

	override val icon: DeploymentCaseIcon?
		@JsonIgnore
		get() = DeploymentCaseIcon.CONDITION
}