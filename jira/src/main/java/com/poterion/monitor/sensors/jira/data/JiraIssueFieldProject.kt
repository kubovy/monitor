package com.poterion.monitor.sensors.jira.data

data class JiraIssueFieldProject(var self: String? = null,
								 var id: String? = null,
								 var key: String? = null,
								 var name: String? = null,
								 var avatarUrls: Map<String, String> = emptyMap())