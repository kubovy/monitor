package com.poterion.monitor.notifiers.deploymentcase.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon

class Placeholder(private val label: String = "",
				  @JsonIgnore override val icon: DeploymentCaseIcon? = null) : StateMachineItem {

	@JsonIgnore
	override fun getTitle(devices: Collection<Device>, variables: Collection<Variable>): String = label

	override fun isBinarySame(other: StateMachineItem, devices: Collection<Device>, variables: Collection<Variable>): Boolean = true
}