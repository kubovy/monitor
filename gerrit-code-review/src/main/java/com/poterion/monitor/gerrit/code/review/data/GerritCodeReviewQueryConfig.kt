package com.poterion.monitor.gerrit.code.review.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class GerritCodeReviewQueryConfig(var name: String = "",
									   var query: String = "",
									   var priority: Priority = Priority.NONE,
									   var status: Status = Status.NONE)