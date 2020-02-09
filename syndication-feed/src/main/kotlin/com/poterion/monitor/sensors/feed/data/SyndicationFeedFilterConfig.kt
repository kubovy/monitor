package com.poterion.monitor.sensors.feed.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
data class SyndicationFeedFilterConfig(var name: String = "",
									   var titleFilter: String = "",
									   var summaryFilter: String = "",
									   var priority: Priority = Priority.NONE,
									   var status: Status = Status.NONE)