package com.poterion.monitor.notifiers.notifications.data

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
							   override var minStatus: Status = Status.NONE,
							   override val services: MutableSet<String> = mutableSetOf(),
							   var repeatAfter: Long? = null,
							   var durations: MutableMap<String, Long?> = mutableMapOf(),
							   var lastUpdated: MutableMap<String, MutableMap<String, LastUpdatedConfig>> = mutableMapOf()) : NotifierConfig