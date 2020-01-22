package com.poterion.monitor.sensors.jira.data

data class JiraIssueFieldIssueType(var self: String? = null,
								   var id: String? = null,
								   var description: String? = null,
								   var iconUrl: String? = null,
								   var name: String? = null,
								   var subtask: Boolean = false,
								   var avatarId: Int? = null)