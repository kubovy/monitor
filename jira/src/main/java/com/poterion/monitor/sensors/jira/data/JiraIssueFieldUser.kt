package com.poterion.monitor.sensors.jira.data

data class JiraIssueFieldUser(var self: String? = null,
							  var name: String? = null,
							  var key: String? = null,
							  var emailAddress: String? = null,
							  var avatarUrls: Map<String, String> = emptyMap(),
							  var displayName: String? = null,
							  var active: Boolean = false,
							  var timeZone: String? = null)