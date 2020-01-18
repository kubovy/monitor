package com.poterion.monitor.notifications.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.notifiers.NotifierConfig
import java.util.*

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class NotificationsConfig(override var type: String = NotificationsConfig::class.java.simpleName,
							   override val uuid: String = UUID.randomUUID().toString(),
							   override var name: String = "Notifications",
							   override var enabled: Boolean = true,
							   override var minPriority: Priority = Priority.LOW,
							   var repeatAfter: Long? = null,
							   var minStatus: Status = Status.INFO,
							   var durations: MutableMap<String, Long?> = mutableMapOf()) : NotifierConfig