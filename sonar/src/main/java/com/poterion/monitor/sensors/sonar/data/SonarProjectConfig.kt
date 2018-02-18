package com.poterion.monitor.sensors.sonar.data

import com.poterion.monitor.data.Priority

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class SonarProjectConfig(var id: Int? = null,
							  var name: String = "",
							  var priority: Priority = Priority.NONE)