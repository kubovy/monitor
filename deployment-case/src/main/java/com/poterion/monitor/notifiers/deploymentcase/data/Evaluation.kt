package com.poterion.monitor.notifiers.deploymentcase.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class Evaluation(var conditions: List<Condition> = listOf(),
					  var actions: List<Action> = listOf()) : StateMachineItem {

	@JsonIgnore
	override fun getTitle(devices: Collection<Device>, variables: Collection<Variable>): String = "Evaluation"

	override val icon: DeploymentCaseIcon?
		@JsonIgnore
		get() = DeploymentCaseIcon.EVALUATION

	override fun isBinarySame(other: StateMachineItem, devices: Collection<Device>, variables: Collection<Variable>): Boolean = true
}