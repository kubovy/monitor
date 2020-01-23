package com.poterion.monitor.sensors.storyboard.data

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
data class Project(
		var id: Int? = null,
		var name: String? = null,
		var description: String? = null,
		@JsonProperty("is_active") var isActive: Boolean = false,
		@JsonProperty("repo_url") var repoUrl: String? = null,
		@JsonProperty("created_at") var createdAt: String? = null)