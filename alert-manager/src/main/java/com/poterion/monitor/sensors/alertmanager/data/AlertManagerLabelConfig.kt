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
package com.poterion.monitor.sensors.alertmanager.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.services.ServiceSubConfig
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import java.util.*

/**
 * Alert manager label or annotation and value to [Priority]/[Status] mapping item.
 *
 * @param name Label or annotation name
 * @param value Label or annotation value
 * @param priority [Priority]
 * @param status [Status]
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class AlertManagerLabelConfig(override val uuid: String = UUID.randomUUID().toString(),
							  name: String = "",
							  value: String = "",
							  priority: Priority = Priority.NONE,
							  status: Status = Status.NONE) : ServiceSubConfig {

	/**
	 * Label or annotation name.
	 * @see nameProperty
	 */
	var name: String
		get() = nameProperty.get()
		set(value) = nameProperty.set(value)

	/**
	 * Label or annotation name property.
	 * @see name
	 */
	val nameProperty: StringProperty = SimpleStringProperty(name)
		@JsonIgnore get

	/**
	 * Label or annotation value.
	 * @see valueProperty
	 */
	var value: String
		get() = valueProperty.get()
		set(value) = valueProperty.set(value)

	/**
	 * Label or annotation value property.
	 * @see value
	 */
	val valueProperty: StringProperty = SimpleStringProperty(value)
		@JsonIgnore get

	/**
	 * [Priority].
	 * @see priorityProperty
	 */
	var priority: Priority
		get() = priorityProperty.get()
		set(value) = priorityProperty.set(value)

	/**
	 * [Priority] property.
	 * @see priority
	 */
	val priorityProperty: ObjectProperty<Priority> = SimpleObjectProperty(priority)
		@JsonIgnore get

	/**
	 * [Status].
	 * @see statusProperty
	 */
	var status: Status
		get() = statusProperty.get()
		set(value) = statusProperty.set(value)

	/**
	 * [Status] property.
	 * @see status
	 */
	val statusProperty: ObjectProperty<Status> = SimpleObjectProperty(status)
		@JsonIgnore get

	override val configTitle: String
		get() = "${name}: ${value}"
}