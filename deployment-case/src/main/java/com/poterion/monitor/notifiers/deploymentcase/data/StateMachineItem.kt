package com.poterion.monitor.notifiers.deploymentcase.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon
import javafx.scene.image.ImageView

interface StateMachineItem {
	val title: String
		@JsonIgnore
		get

	val icon: DeploymentCaseIcon?
		@JsonIgnore
		get

	val getImageView: ImageView?
		@JsonIgnore
		get() = icon?.let { ImageView(it.image(16, 16)) }
}