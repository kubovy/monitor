package com.poterion.monitor.notifiers.raspiw2812.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.notifiers.NotifierConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class RaspiW2812Config(override var type: String = RaspiW2812Config::class.java.simpleName,
							override var name: String = "",
							override var enabled: Boolean = true,
							override var minPriority: Priority = Priority.LOW,
							var deviceName: String = "",
							var portName: String? = null,
							var combineMultipleServices: Boolean = true,
							var items: Collection<RaspiW2812ItemConfig> = emptyList()) : NotifierConfig