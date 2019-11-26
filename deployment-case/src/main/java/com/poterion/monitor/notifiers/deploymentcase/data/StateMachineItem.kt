package com.poterion.monitor.notifiers.deploymentcase.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.poterion.monitor.api.lib.toImageView
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon
import javafx.scene.image.ImageView

interface StateMachineItem {
	val title: String
		@JsonIgnore
		get

	val icon: DeploymentCaseIcon?
		@JsonIgnore
		get

	fun isBinarySame(other: StateMachineItem): Boolean
}