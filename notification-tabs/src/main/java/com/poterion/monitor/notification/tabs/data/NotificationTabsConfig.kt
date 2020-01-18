package com.poterion.monitor.notification.tabs.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.notifiers.NotifierConfig
import java.util.*

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class NotificationTabsConfig(override var type: String = NotificationTabsConfig::class.java.simpleName,
								  override val uuid: String = UUID.randomUUID().toString(),
								  override var name: String = "Notification Tabs",
								  override var enabled: Boolean = true,
								  override var minPriority: Priority = Priority.LOW,
								  var minStatus: Status = Status.NONE,
								  var services: MutableSet<String> = mutableSetOf(),
								  var alertTitleWidth: Double = 200.0,
								  var alertServiceWidth: Double = 200.0,
								  var alertLabelsWidth: Double = 200.0) : NotifierConfig