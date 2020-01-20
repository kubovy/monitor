package com.poterion.monitor.sensors.storyboard.data

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class Task(
		var id: Int? = null,
		@JsonProperty("project_id")var projectId: Int? = null,
		@JsonProperty("story_id")var storyId: Int? = null,
		@JsonProperty("branch_id")var branchId: Int? = null,
		@JsonProperty("milestone_id")var milestoneId: Int? = null,
		var title: String? = null,
		var status: String? = null,
		var link: String? = null,
		@JsonProperty("due_dates")var dueDates: Collection<Int> = emptyList(),
		@JsonProperty("creator_id")var creatorId: Int? = null,
		@JsonProperty("assignee_id")var assigneeId: Int? = null,
		@JsonProperty("created_at") var createdAt: String? = null,
		@JsonProperty("updated_at") var updatedAt: String? = null)