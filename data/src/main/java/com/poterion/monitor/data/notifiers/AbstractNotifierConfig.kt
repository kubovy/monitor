/******************************************************************************
 * Copyright (c) 2020 Jan Kubovy <jan@kubovy.eu>                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify it    *
 * under the terms of the GNU General Public License as published by the Free *
 * Software Foundation, version 3.                                            *
 *                                                                            *
 * This program is distributed in the hope that it will be useful, but        *
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY *
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License    *
 * for more details.                                                          *
 *                                                                            *
 * You should have received a copy of the GNU General Public License along    *
 * with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ******************************************************************************/
package com.poterion.monitor.data.notifiers

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.poterion.monitor.data.AbstractModuleConfig
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.utils.javafx.toObservableList
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList

/**
 * Abstract service configuration with common implementation of [ServiceConfig].
 *
 * @param name Module name
 * @param enabled Whether module is enabled (`true`) or not (`false`)
 * @param minPriority Minimum priority a [StatusItem][com.poterion.monitor.data.StatusItem] must have in order to be
 *        considered by this notifier.
 * @param minStatus Minimum status a [StatusItem][com.poterion.monitor.data.StatusItem] must have in order to be
 *        considered by this notifier.
 * @param services List of service [UUIDs][java.util.UUID] of [services][ServiceConfig]
 *        contributing their [status items][com.poterion.monitor.data.StatusItem] to this notifier. (An empty list means
 *        that all [services][com.poterion.monitor.api.controllers.Service] are contributing with their
 *        [status items][com.poterion.monitor.data.StatusItem] to this notifier)
 * @param tableColumnWidths Saved UI table column widths (column name -> width)
 * @author Jan Kubovy [jan@kubovy.eu]
 */
abstract class AbstractNotifierConfig(name: String = "",
									  enabled: Boolean = false,
									  minPriority: Priority = Priority.LOW,
									  minStatus: Status = Status.NONE,
									  services: List<String> = emptyList(),
									  tableColumnWidths: Map<String, Int> = emptyMap()) :

		AbstractModuleConfig(name, enabled, tableColumnWidths), NotifierConfig {

	final override var minPriority: Priority
		get() = minPriorityProperty.get()
		set(value) = minPriorityProperty.set(value)

	final override val minPriorityProperty: ObjectProperty<Priority> = SimpleObjectProperty(minPriority)
		@JsonIgnore get

	final override var minStatus: Status
		get() = minStatusProperty.get()
		set(value) = minStatusProperty.set(value)

	final override val minStatusProperty: ObjectProperty<Status> = SimpleObjectProperty(minStatus)
		@JsonIgnore get

	@Suppress("unused")
	private var _services: List<String>
		@JsonProperty("services") get() = services
		set(value) {
			services.setAll(value)
		}

	final override val services: ObservableList<String> = services.toObservableList()
		@JsonIgnore get
}