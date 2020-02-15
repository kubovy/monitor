/******************************************************************************
 * Copyright (C) 2020 Jan Kubovy (jan@kubovy.eu)                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.      *
 ******************************************************************************/
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