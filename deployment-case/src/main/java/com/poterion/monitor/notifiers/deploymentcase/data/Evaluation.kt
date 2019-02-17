package com.poterion.monitor.notifiers.deploymentcase.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon

data class Evaluation(var conditions: List<Condition> = listOf(),
					  var actions: List<Action> = listOf()) : StateMachineItem {
	override val title: String
		@JsonIgnore
		get() = "Evaluation"

	override val icon: DeploymentCaseIcon?
		@JsonIgnore
		get() = DeploymentCaseIcon.EVALUATION
}