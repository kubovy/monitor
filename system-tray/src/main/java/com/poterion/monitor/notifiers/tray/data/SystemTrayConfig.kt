package com.poterion.monitor.notifiers.tray.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.notifiers.NotifierConfig
import java.util.*

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class SystemTrayConfig(override var type: String = SystemTrayConfig::class.java.simpleName,
							override val uuid: String = UUID.randomUUID().toString(),
							override var name: String = "System Tray",
							override var enabled: Boolean = true,
							override var minPriority: Priority = Priority.LOW,
							override var minStatus: Status = Status.NONE,
							override val services: MutableSet<String> = mutableSetOf(),
							var refresh: Boolean = false) : NotifierConfig