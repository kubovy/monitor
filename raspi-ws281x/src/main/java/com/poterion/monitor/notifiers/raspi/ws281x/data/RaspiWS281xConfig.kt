package com.poterion.monitor.notifiers.raspi.ws281x.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.notifiers.NotifierConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class RaspiWS281xConfig(override var type: String = RaspiWS281xConfig::class.java.simpleName,
							 override var name: String = "",
							 override var enabled: Boolean = true,
							 override var minPriority: Priority = Priority.LOW,
							 var deviceAddress: String = "",
							 var combineMultipleServices: Boolean = true,
							 var items: Collection<RaspiWS281xItemConfig> = emptyList()) : NotifierConfig