package com.poterion.monitor.sensors.storyboard.data

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
data class Story(var id: Int? = null,
				 var title: String? = null,
				 var status: String? = null,
				 var description: String? = null,
				 var tags: Collection<String> = emptyList(),
				 @JsonProperty("private") var isPrivate: Boolean = false,
				 @JsonProperty("is_bug") var isBug: Boolean = false,
				 @JsonProperty("story_type_id") var storyTypeId: Int? = null,
				 @JsonProperty("task_statuses") var taskStatuses: Collection<TaskStatus> = emptyList(),
				 @JsonProperty("creator_id") var creatorId: Int? = null,
				 @JsonProperty("created_at") var createdAt: String? = null,
				 @JsonProperty("updated_at") var updatedAt: String? = null)