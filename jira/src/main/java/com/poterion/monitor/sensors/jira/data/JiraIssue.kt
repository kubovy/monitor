package com.poterion.monitor.sensors.jira.data

data class JiraIssue(var id: String? = null,
					 var key: String? = null,
					 var self: String? = null,
					 var expand: String? = null,
					 var fields: JiraIssueFields? = null)