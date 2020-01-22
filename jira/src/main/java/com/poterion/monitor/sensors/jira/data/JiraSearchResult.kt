package com.poterion.monitor.sensors.jira.data

data class JiraSearchResult(var expand: String? = null,
							var startAt: Int = 0,
							var maxResults: Int = 0,
							var total: Int = 0,
							var issues: Collection<JiraIssue> = emptyList())