package com.poterion.monitor.sensors.alertmanager.data

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
data class AlertManagerStatusResponse(val state: String = "",
									  val silencedBy: List<String> = mutableListOf(),
									  val inhibitedBy: List<String> = mutableListOf())