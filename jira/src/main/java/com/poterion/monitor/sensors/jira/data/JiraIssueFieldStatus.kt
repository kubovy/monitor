package com.poterion.monitor.sensors.jira.data

data class JiraIssueFieldStatus(var self: String? = null,
								var description: String? = null,
								var iconUrl: String? = null,
								var name: String? = null,
								var id: String? = null,
								var statusCategory: JiraIssueFieldStatusCategory? = null)