package com.poterion.monitor.sensors.alertmanager.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class AlertManagerLabelConfig(var name: String = "",
								   var value: String = "",
								   var priority: Priority = Priority.NONE,
								   var status: Status = Status.NONE)