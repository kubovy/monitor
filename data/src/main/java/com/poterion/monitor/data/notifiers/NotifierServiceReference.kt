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
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/**
 * Notifier service reference.
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class NotifierServiceReference(uuid: String? = null,
							   minStatus: Status = Status.NONE,
							   minPriority: Priority = Priority.NONE) {

	var uuid: String?
		get() = uuidProperty.get()
		set(value) = uuidProperty.set(value)

	val uuidProperty: StringProperty = SimpleStringProperty(uuid)
		@JsonIgnore get

	var minStatus: Status
		get() = minStatusProperty.get()
		set(value) = minStatusProperty.set(value)

	val minStatusProperty: ObjectProperty<Status> = SimpleObjectProperty(minStatus)
		@JsonIgnore get

	var minPriority: Priority
		get() = minPriorityProperty.get()
		set(value) = minPriorityProperty.set(value)

	val minPriorityProperty: ObjectProperty<Priority> = SimpleObjectProperty(minPriority)
		@JsonIgnore get
}