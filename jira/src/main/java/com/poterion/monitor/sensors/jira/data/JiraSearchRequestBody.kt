package com.poterion.monitor.sensors.jira.data

data class JiraSearchRequestBody(var jql: String = "",
								 var startAt: Int = 0,
								 var maxResults: Int = 10,
								 var fields: Set<String> = JiraIssueFieldName.values().map { it.toString() }.toSet())