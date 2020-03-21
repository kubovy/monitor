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
package com.poterion.monitor.data.notifiers

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import javafx.beans.property.ObjectProperty
import javafx.collections.ObservableList


/**
 * Notifier config interface.
 *
 * @author Jan Kubovy [jan@kubovy.eu]
 */
interface NotifierConfig : ModuleConfig {
	/**
	 * Minimum priority a [StatusItem][com.poterion.monitor.data.StatusItem] must have in order to be considered by
	 * this notifier.
	 * @see minPriorityProperty
	 */
	var minPriority: Priority
		@JsonInclude(value = JsonInclude.Include.ALWAYS)
		get() = minPriorityProperty.get()
		set(value) = minPriorityProperty.set(value)

	/**
	 * Minimum priority property.
	 * @see minPriority
	 */
	val minPriorityProperty: ObjectProperty<Priority>
		@JsonIgnore get

	/**
	 * Minimum status a [StatusItem][com.poterion.monitor.data.StatusItem] must have in order to be considered by
	 * this notifier.
	 * @see minStatusProperty
	 */
	var minStatus: Status
		@JsonInclude(value = JsonInclude.Include.ALWAYS)
		get() = minStatusProperty.get()
		set(value) = minStatusProperty.set(value)

	/**
	 * Minimum status property.
	 * @see minStatus
	 */
	val minStatusProperty: ObjectProperty<Status>
		@JsonIgnore get

	/**
	 * List of service [UUIDs][java.util.UUID] of [services][com.poterion.monitor.api.controllers.Service] contributing
	 * their [status items][com.poterion.monitor.data.StatusItem] to this notifier.
	 *
	 * An empty list means that all [services][com.poterion.monitor.api.controllers.Service] are contributing with their
	 * [status items][com.poterion.monitor.data.StatusItem] to this notifier.
	 */
	val services: ObservableList<NotifierServiceReference>
}