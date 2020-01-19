package com.poterion.monitor.gerrit.code.review.data

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class GerritCodeReviewQueryResponse(var id: String? = null,
										 var project: String? = null,
										 var branch: String? = null,
										 var topic: String? = null,
										 var hashtags: Set<String>? = null,
										 @JsonProperty("change_id") var changeId: String? = null,
										 var subject: String? = null,
										 var status: String? = null,
										 var created: String? = null,
										 var updated: String? = null,
										 @JsonProperty("submit_type") var submitType: String? = null,
										 var mergeable: Boolean? = null,
										 var submittable: Boolean? = null,
										 var insertions: Int? = null,
										 var deletions: Int? = null,
										 var _number: Int? = null,
										 var owner: GerritChangeOwner? = null,
										 var labels: Map<String, Map<String, GerritChangeOwner>> = emptyMap())