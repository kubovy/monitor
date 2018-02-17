package com.poterion.monitor.data.services

import com.poterion.monitor.data.Priority

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class JenkinsJobConfig(var name: String = "",
                            var priority: Priority = Priority.NONE)