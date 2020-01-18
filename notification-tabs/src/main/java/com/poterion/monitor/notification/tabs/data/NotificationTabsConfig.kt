package com.poterion.monitor.notification.tabs.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.notifiers.NotifierConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class NotificationTabsConfig(override var type: String = NotificationTabsConfig::class.java.simpleName,
								  override var name: String = "Notification Tabs",
								  override var enabled: Boolean = true,
								  override var minPriority: Priority = Priority.LOW,
								  var minStatus: Status = Status.NONE,
								  var services: Set<String> = emptySet()) : NotifierConfig