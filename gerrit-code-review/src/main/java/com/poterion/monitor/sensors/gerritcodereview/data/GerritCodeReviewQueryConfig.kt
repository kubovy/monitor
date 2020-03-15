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
package com.poterion.monitor.sensors.gerritcodereview.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/**
 * Gerrit code review query to [Priority],[Status] mapping configuration item.
 *
 * @param name Name of the configuration item
 * @param query Gerrit code review query
 * @param priority [Priority]
 * @param status [Status]
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class GerritCodeReviewQueryConfig(name: String = "",
								  query: String = "",
								  priority: Priority = Priority.NONE,
								  status: Status = Status.NONE) {

	/**
	 * Name of the configuration item.
	 * @see nameProperty
	 */
	var name: String
		get() = nameProperty.get()
		set(value) = nameProperty.set(value)

	/**
	 * Name of the configuration item property
	 * @see name
	 */
	val nameProperty: StringProperty = SimpleStringProperty(name)
		@JsonIgnore get

	/**
	 * Gerrit code review query.
	 * @see queryProperty
	 */
	var query: String
		get() = queryProperty.get()
		set(value) = queryProperty.set(value)

	/**
	 * Gerrit code review query property.
	 * @see query
	 */
	val queryProperty: StringProperty = SimpleStringProperty(query)
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
}