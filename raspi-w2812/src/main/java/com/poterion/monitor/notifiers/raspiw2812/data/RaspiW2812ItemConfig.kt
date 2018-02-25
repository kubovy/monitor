package com.poterion.monitor.notifiers.raspiw2812.data

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class RaspiW2812ItemConfig(var id: String = "",
								var statusNone: List<LightConfig> = emptyList(),
								var statusUnknown: List<LightConfig> = emptyList(),
								var statusOk: List<LightConfig> = emptyList(),
								var statusInfo: List<LightConfig> = emptyList(),
								var statusNotification: List<LightConfig> = emptyList(),
								var statusConnectionError: List<LightConfig> = emptyList(),
								var statusServiceError: List<LightConfig> = emptyList(),
								var statusWarning: List<LightConfig> = emptyList(),
								var statusError: List<LightConfig> = emptyList(),
								var statusFatal: List<LightConfig> = emptyList())