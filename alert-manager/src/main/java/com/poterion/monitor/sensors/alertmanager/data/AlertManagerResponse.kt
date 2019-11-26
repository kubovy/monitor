package com.poterion.monitor.sensors.alertmanager.data

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class AlertManagerResponse(
		val annotations: List<Map<String, String>> = mutableListOf(),
		val receivers: List<Map<String, String>> = mutableListOf(),
		val fingerprint: String = "",
		val startsAt: String = "",
		val updatedAt: String = "",
		val endsAt: String = "",
		val status: AlertManagerStatusResponse? = null)

