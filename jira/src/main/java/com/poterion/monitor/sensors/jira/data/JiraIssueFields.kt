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
package com.poterion.monitor.sensors.jira.data

data class JiraIssueFields(var project: JiraIssueFieldProject? = null,
						   var issuetype: JiraIssueFieldIssueType? = null,
						   var summary: String? = null,
						   var description: String? = null,
						   var status: JiraIssueFieldStatus? = null,
						   var priority: JiraIssueFieldPriority? = null,

						   var labels: Collection<String> = emptyList(),
						   var components: Collection<JiraIssueFieldTypeComponent> = emptyList(),
						   var issuelinks: Collection<JiraIssueFieldIssueLink> = emptyList(),

						   var workratio: Long? = null,
						   var progress: JiraIssueFieldProgress? = null,
						   var aggregateprogress: JiraIssueFieldProgress? = null,
						   var resolution: JiraIssueFieldResolution? = null,
						   var resolutiondate: String? = null, // Date yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ

						   var watches: JiraIssueFieldWatches? = null,
						   var votes: JiraIssueFieldVotes? = null,

		//var timespent: Any? = null,
		//var timeoriginalestimate: null,
		//var fixVersions: Collection<Any> = emptyList(),
		//var aggregatetimespent: null,
		//var aggregatetimeestimate: null,
		//var lastViewed: null,
		//var subtasks: Collection<Any> = emptyList(),
		//var environment: null,
		//var timeestimate: null,
		//var aggregatetimeoriginalestimate: null,
		//var versions: Collection<Any> = emptyList(),
		//var duedate: null,

						   var creator: JiraIssueFieldUser? = null,
						   var reporter: JiraIssueFieldUser? = null,
						   var assignee: JiraIssueFieldUser? = null,
						   var created: String? = null, // Date yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ
						   var updated: String? = null) // Date yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ