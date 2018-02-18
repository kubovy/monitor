package com.poterion.monitor.notifiers.raspiw2812.data

import com.poterion.monitor.data.notifiers.NotifierConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class RaspiW2812Config(override var type: String = RaspiW2812Config::class.java.simpleName,
							override var name: String = "",
							override var enabled: Boolean = true,
							var deviceName: String = "",
							var items: Collection<RaspiW2812ItemConfig> = emptyList()) : NotifierConfig