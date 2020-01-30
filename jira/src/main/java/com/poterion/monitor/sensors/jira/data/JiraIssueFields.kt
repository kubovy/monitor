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