package com.poterion.monitor.sensors.alertmanager.data

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
data class AlertManagerResponse(
		val fingerprint: String = "",
		val annotations: Map<String, String> = mutableMapOf(),
		val labels: Map<String, String> = mutableMapOf(),
		val receivers: List<AlertManagerReceiverResponse> = mutableListOf(),
		val startsAt: String? = null,
		val endsAt: String? = null,
		val updatedAt: String? = null,
		val status: AlertManagerStatusResponse? = null,
		val generatorURL: String = "")

