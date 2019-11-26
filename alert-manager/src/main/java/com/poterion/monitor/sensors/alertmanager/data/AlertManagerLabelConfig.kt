package com.poterion.monitor.sensors.alertmanager.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class AlertManagerLabelConfig(var label: String = "",
								   var priority: Priority = Priority.NONE)//,
//								   var silencedStatus: Status = Status.OFF,
//								   var notFiringStatus: Status = Status.OK,
//								   var firingStatus: Status = Status.ERROR)