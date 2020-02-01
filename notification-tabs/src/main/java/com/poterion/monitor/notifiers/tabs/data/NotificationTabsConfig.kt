package com.poterion.monitor.notifiers.tabs.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.notifiers.NotifierConfig
import java.util.*

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
data class NotificationTabsConfig(override var type: String = NotificationTabsConfig::class.java.simpleName,
								  override var uuid: String = UUID.randomUUID().toString(),
								  override var name: String = "Notification Tabs",
								  override var enabled: Boolean = false,
								  override var minPriority: Priority = Priority.LOW,
								  override var minStatus: Status = Status.NONE,
								  override val services: MutableSet<String> = mutableSetOf(),
								  override var tableColumnWidths: MutableMap<String, Int> = mutableMapOf(),
								  var alertTitleWidth: Double = 200.0,
								  var alertServiceWidth: Double = 200.0,
								  var alertLabelsWidth: Double = 200.0,
								  var selectedPriority: Priority? = null,
								  var selectedStatus: Status? = null,
								  var selectedServiceId: String? = null,
								  var showWatched: Boolean = false,
								  var showSilenced: Boolean = false,
								  val watchedItems: MutableSet<String> = mutableSetOf()) : NotifierConfig