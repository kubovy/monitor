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
package com.poterion.monitor.notifiers.notifications.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.notifiers.AbstractNotifierConfig
import com.poterion.utils.javafx.toObservableMap
import com.poterion.utils.kotlin.setAll
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableMap
import java.util.*

/**
 * Notifications notifier module configuration.
 *
 * @param name Module name
 * @param enabled Whether module is enabled (`true`) or not (`false`)
 * @param minPriority Minimum priority a [StatusItem][com.poterion.monitor.data.StatusItem] must have in order to be
 *        considered by this notifier.
 * @param minStatus Minimum status a [StatusItem][com.poterion.monitor.data.StatusItem] must have in order to be
 *        considered by this notifier.
 * @param services List of service [UUIDs][java.util.UUID] of [services][com.poterion.monitor.api.controllers.Service]
 *        contributing their [status items][com.poterion.monitor.data.StatusItem] to this notifier. (An empty list means
 *        that all [services][com.poterion.monitor.api.controllers.Service] are contributing with their
 *        [status items][com.poterion.monitor.data.StatusItem] to this notifier)
 * @param tableColumnWidths Saved UI table column widths (column name -> width)
 * @param repeatAfter
 * @param durations
 * @param lastUpdated
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class NotificationsConfig(override var type: String = NotificationsConfig::class.java.simpleName,
						  override var uuid: String = UUID.randomUUID().toString(),
						  name: String = "Notifications",
						  enabled: Boolean = false,
						  minPriority: Priority = Priority.LOW,
						  minStatus: Status = Status.NONE,
						  services: List<String> = emptyList(),
						  tableColumnWidths: Map<String, Int> = emptyMap(),
						  repeatAfter: Long? = null,
						  durations: Map<String, Long?> = emptyMap(),
						  lastUpdated: Map<String, MutableMap<String, LastUpdatedConfig>> = emptyMap()) :
		AbstractNotifierConfig(name, enabled, minPriority, minStatus, services, tableColumnWidths) {

	var repeatAfter: Long?
		get() = repeatAfterProperty.get()
		set(value) = repeatAfterProperty.set(value)

	val repeatAfterProperty: ObjectProperty<Long?> = SimpleObjectProperty(repeatAfter)
		@JsonIgnore get

	@Suppress("unused")
	private var _durations: Map<String, Long?>
		@JsonProperty("durations") get() = durations
		set(value) = durations.setAll(value)

	val durations: ObservableMap<String, Long?> = durations.toObservableMap()
		@JsonIgnore get

	@Suppress("unused")
	private var _lastUpdated: Map<String, MutableMap<String, LastUpdatedConfig>>
		@JsonProperty("lastUpdated") get() = lastUpdated
		set(value) = lastUpdated.setAll(value)

	val lastUpdated: ObservableMap<String, MutableMap<String, LastUpdatedConfig>> = lastUpdated.toObservableMap()
		@JsonIgnore get
}