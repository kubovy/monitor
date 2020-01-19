package com.poterion.monitor.notifiers.deploymentcase.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon

data class State(var name: String = "0", var evaluations: List<Evaluation> = listOf()) : StateMachineItem {

	@JsonIgnore
	override fun getTitle(devices: Collection<Device>, variables: Collection<Variable>): String = name

	override val icon: DeploymentCaseIcon?
		@JsonIgnore
		get() = DeploymentCaseIcon.STATE

	override fun isBinarySame(other: StateMachineItem, devices: Collection<Device>, variables: Collection<Variable>) =
			other is State && name == other.name
}