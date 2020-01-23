package com.poterion.monitor.sensors.jenkins.data

import com.poterion.monitor.data.Priority

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
data class JenkinsJobConfig(var name: String = "",
							var priority: Priority = Priority.NONE)