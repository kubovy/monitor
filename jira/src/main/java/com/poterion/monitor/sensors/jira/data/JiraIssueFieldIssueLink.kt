package com.poterion.monitor.sensors.jira.data

data class JiraIssueFieldIssueLink(var id: String? = null,
								   var self: String? = null,
								   var type: JiraIssueFieldIssueLinkType? = null,
								   var outwardIssue: JiraIssue? = null)