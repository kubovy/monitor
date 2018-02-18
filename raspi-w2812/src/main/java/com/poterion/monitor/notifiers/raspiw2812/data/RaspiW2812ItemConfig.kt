package com.poterion.monitor.notifiers.raspiw2812.data

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class RaspiW2812ItemConfig(var id: String = "",
								var statusNone: List<String> = emptyList(),
								var statusUnknown: List<String> = emptyList(),
								var statusOk: List<String> = emptyList(),
								var statusInfo: List<String> = emptyList(),
								var statusNotification: List<String> = emptyList(),
								var statusConnectionError: List<String> = emptyList(),
								var statusServiceError: List<String> = emptyList(),
								var statusWarning: List<String> = emptyList(),
								var statusError: List<String> = emptyList(),
								var statusFatal: List<String> = emptyList())