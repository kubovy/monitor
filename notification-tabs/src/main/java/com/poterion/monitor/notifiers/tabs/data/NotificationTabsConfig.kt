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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.notifiers.AbstractNotifierConfig
import com.poterion.monitor.data.notifiers.NotifierServiceReference
import com.poterion.utils.javafx.toObservableSet
import com.poterion.utils.kotlin.setAll
import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.ObservableSet
import java.util.*

/**
 * Notification tabs notifier module configuration.
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
 * @param alertLabelsWidth
 * @param alertServiceWidth
 * @param alertTitleWidth
 * @param selectedPriority
 * @param selectedStatus
 * @param selectedServiceId
 * @param showWatched
 * @param showSilenced
 * @param watchedItems
 * @author Jan Kubovy [jan@kubovy.eu]
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class NotificationTabsConfig(override var type: String = NotificationTabsConfig::class.java.simpleName,
							 override var uuid: String = UUID.randomUUID().toString(),
							 name: String = "Notification Tabs",
							 enabled: Boolean = false,
							 minPriority: Priority = Priority.LOW,
							 minStatus: Status = Status.NONE,
							 services: List<NotifierServiceReference> = emptyList(),
							 tableColumnWidths: Map<String, Int> = emptyMap(),
							 alertTitleWidth: Double = 200.0,
							 alertServiceWidth: Double = 200.0,
							 alertConfigWidth: Double = 200.0,
							 alertLabelsWidth: Double = 200.0,
							 selectedPriority: Priority? = null,
							 selectedStatus: Status? = null,
							 selectedServiceId: String? = null,
							 selectedConfiguration: String? = null,
							 showWatched: Boolean = false,
							 showSilenced: Boolean = false,
							 watchedItems: Set<String> = emptySet()) :
		AbstractNotifierConfig(name, enabled, minPriority, minStatus, services, tableColumnWidths) {

	var alertTitleWidth: Double
		get() = alertTitleWidthProperty.get()
		set(value) = alertTitleWidthProperty.set(value)

	val alertTitleWidthProperty: DoubleProperty = SimpleDoubleProperty(alertTitleWidth)
		@JsonIgnore get

	var alertServiceWidth: Double
		get() = alertServiceWidthProperty.get()
		set(value) = alertServiceWidthProperty.set(value)

	val alertServiceWidthProperty: DoubleProperty = SimpleDoubleProperty(alertServiceWidth)
		@JsonIgnore get

	var alertConfigWidth: Double
		get() = alertConfigWidthProperty.get()
		set(value) = alertConfigWidthProperty.set(value)

	val alertConfigWidthProperty: DoubleProperty = SimpleDoubleProperty(alertConfigWidth)
		@JsonIgnore get

	var alertLabelsWidth: Double
		get() = alertLabelsWidthProperty.get()
		set(value) = alertLabelsWidthProperty.set(value)

	val alertLabelsWidthProperty: DoubleProperty = SimpleDoubleProperty(alertLabelsWidth)
		@JsonIgnore get

	var selectedPriority: Priority?
		get() = selectedPriorityProperty.get()
		set(value) = selectedPriorityProperty.set(value)

	val selectedPriorityProperty: ObjectProperty<Priority?> = SimpleObjectProperty(selectedPriority)
		@JsonIgnore get

	var selectedStatus: Status?
		get() = selectedStatusProperty.get()
		set(value) = selectedStatusProperty.set(value)

	val selectedStatusProperty: ObjectProperty<Status?> = SimpleObjectProperty(selectedStatus)
		@JsonIgnore get

	var selectedServiceId: String?
		get() = selectedServiceIdProperty.get()
		set(value) = selectedServiceIdProperty.set(value)

	val selectedServiceIdProperty: StringProperty = SimpleStringProperty(selectedServiceId)
		@JsonIgnore get

	var selectedConfiguration: String?
		get() = selectedConfigurationProperty.get()
		set(value) = selectedConfigurationProperty.set(value)

	val selectedConfigurationProperty: StringProperty = SimpleStringProperty(selectedConfiguration)
		@JsonIgnore get

	var showWatched: Boolean
		get() = showWatchedProperty.get()
		set(value) = showWatchedProperty.set(value)

	val showWatchedProperty: BooleanProperty = SimpleBooleanProperty(showWatched)
		@JsonIgnore get

	var showSilenced: Boolean
		get() = showSilencedProperty.get()
		set(value) = showSilencedProperty.set(value)

	val showSilencedProperty: BooleanProperty = SimpleBooleanProperty(showSilenced)
		@JsonIgnore get

	@Suppress("unused")
	private var _watchedItems: Set<String>
		@JsonProperty("watchedItems") get() = watchedItems
		set(value) {
			watchedItems.setAll(value)
		}

	val watchedItems: ObservableSet<String> = watchedItems.toObservableSet()
		@JsonIgnore get
}