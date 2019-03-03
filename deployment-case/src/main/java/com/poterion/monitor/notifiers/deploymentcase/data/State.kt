package com.poterion.monitor.notifiers.deploymentcase.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon

data class State(var name: String = "0", var evaluations: List<Evaluation> = listOf()) : StateMachineItem {
	override val title: String
		@JsonIgnore
		get() = name

	override val icon: DeploymentCaseIcon?
		@JsonIgnore
		get() = DeploymentCaseIcon.STATE

	override fun isBinarySame(other: StateMachineItem) = other is State && name == other.name
}