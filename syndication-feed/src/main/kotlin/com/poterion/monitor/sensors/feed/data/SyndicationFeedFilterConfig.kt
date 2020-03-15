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
package com.poterion.monitor.sensors.feed.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/**
 * Syndication feed title/summary - [Priority]/[Status] mapping configuration item.
 *
 * @param name Item name
 * @param titleFilter Title filter
 * @param summaryFilter Summary filter
 * @param priority [Priority]
 * @param status [Status]
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class SyndicationFeedFilterConfig(name: String = "",
								  titleFilter: String = "",
								  summaryFilter: String = "",
								  priority: Priority = Priority.NONE,
								  status: Status = Status.NONE) {
	/**
	 * Item name.
	 * @see nameProperty
	 */
	var name: String
		get() = nameProperty.get()
		set(value) = nameProperty.set(value)

	/**
	 * Item name property.
	 * @see name
	 */
	val nameProperty: StringProperty = SimpleStringProperty(name)
		@JsonIgnore get

	/**
	 * Title filter.
	 * @see titleFilterProperty
	 */
	var titleFilter: String
		get() = titleFilterProperty.get()
		set(value) = titleFilterProperty.set(value)

	/**
	 * Title filter property.
	 * @see titleFilter
	 */
	val titleFilterProperty: StringProperty = SimpleStringProperty(titleFilter)
		@JsonIgnore get

	/**
	 * Summary filter.
	 * @see summaryFilterProperty
	 */
	var summaryFilter: String
		get() = summaryFilterProperty.get()
		set(value) = summaryFilterProperty.set(value)

	/**
	 * Summary filter property.
	 * @see summaryFilter
	 */
	val summaryFilterProperty: StringProperty = SimpleStringProperty(summaryFilter)
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