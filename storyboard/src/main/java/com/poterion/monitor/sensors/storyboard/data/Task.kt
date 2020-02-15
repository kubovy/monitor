/******************************************************************************
 * Copyright (C) 2020 Jan Kubovy (jan@kubovy.eu)                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.      *
 ******************************************************************************/
package com.poterion.monitor.sensors.storyboard.data

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author Jan Kubovy [jan@kubovy.eu]
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