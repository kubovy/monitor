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
package com.poterion.monitor.notifiers.tray.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.notifiers.AbstractNotifierConfig
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import java.util.*

/**
 * System tray notifier module configuration.
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
 * @param refresh
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class SystemTrayConfig(override var type: String = SystemTrayConfig::class.java.simpleName,
					   override var uuid: String = UUID.randomUUID().toString(),
					   name: String = "Notifications",
					   enabled: Boolean = false,
					   minPriority: Priority = Priority.LOW,
					   minStatus: Status = Status.NONE,
					   services: List<String> = emptyList(),
					   tableColumnWidths: Map<String, Int> = emptyMap(),
					   refresh: Boolean = false) :
		AbstractNotifierConfig(name, enabled, minPriority, minStatus, services, tableColumnWidths) {

	var refresh: Boolean
		get() = refreshProperty.get()
		set(value) = refreshProperty.set(value)

	val refreshProperty: BooleanProperty = SimpleBooleanProperty(refresh)
		@JsonIgnore get
}