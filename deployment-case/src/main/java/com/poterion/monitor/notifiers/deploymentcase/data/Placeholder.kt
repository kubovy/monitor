package com.poterion.monitor.notifiers.deploymentcase.data

import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon

data class Placeholder(override var title: String = "", override var icon: DeploymentCaseIcon? = null) : StateMachineItem {
	override fun isBinarySame(other: StateMachineItem): Boolean = true
}