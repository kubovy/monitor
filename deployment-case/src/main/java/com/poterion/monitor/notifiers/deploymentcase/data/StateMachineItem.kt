package com.poterion.monitor.notifiers.deploymentcase.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon

interface StateMachineItem {
	@JsonIgnore
	fun getTitle(devices: Collection<Device>, variables: Collection<Variable>): String

	val icon: DeploymentCaseIcon?
		@JsonIgnore
		get

	fun isBinarySame(other: StateMachineItem, devices: Collection<Device>, variables: Collection<Variable>): Boolean
}