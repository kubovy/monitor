package com.poterion.monitor.notifiers.tray.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.notifiers.NotifierConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class SystemTrayConfig(override var type: String = SystemTrayConfig::class.java.simpleName,
							override var name: String = "System Tray",
							override var enabled: Boolean = true,
							override var minPriority: Priority = Priority.LOW,
							var refresh: Boolean = false) : NotifierConfig