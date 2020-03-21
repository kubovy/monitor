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
package com.poterion.monitor.data.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.poterion.monitor.data.HttpProxy
import com.poterion.monitor.data.notifiers.NotifierConfig
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.utils.javafx.ReadOnlyObservableList
import com.poterion.utils.javafx.toObservableMap
import com.poterion.utils.kotlin.setAll
import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableMap

/**
 * Application configuration.
 *
 * @author Jan Kubovy [jan@kubovy.eu]
 */
@JsonInclude(value = JsonInclude.Include.NON_NULL, content = JsonInclude.Include.NON_NULL)
class ApplicationConfiguration(btDiscovery: Boolean = false,
							   showOnStartup: Boolean = true,
							   startMinimized: Boolean = false,
							   windowWidth: Double = 1200.0,
							   windowHeight: Double = 1000.0,
							   commonSplit: Double = 0.3,
							   selectedTab: String? = null,
							   previousTab: String? = null,
							   proxy: HttpProxy? = null,
							   services: Map<String, ServiceConfig> = emptyMap(),
							   notifiers: Map<String, NotifierConfig> = emptyMap(),
							   silenced: Map<String, SilencedStatusItem> = emptyMap()) {

	var btDiscovery: Boolean
		get() = btDiscoveryProperty.get()
		set(value) = btDiscoveryProperty.set(value)

	val btDiscoveryProperty: BooleanProperty = SimpleBooleanProperty(btDiscovery)
		@JsonIgnore get

	var showOnStartup: Boolean
		get() = showOnStartupProperty.get()
		set(value) = showOnStartupProperty.set(value)

	val showOnStartupProperty: BooleanProperty = SimpleBooleanProperty(showOnStartup)
		@JsonIgnore get

	var startMinimized: Boolean
		get() = startMinimizedProperty.get()
		set(value) = startMinimizedProperty.set(value)

	val startMinimizedProperty: BooleanProperty = SimpleBooleanProperty(startMinimized)
		@JsonIgnore get

	var windowWidth: Double
		get() = windowWidthProperty.get()
		set(value) = windowWidthProperty.set(value)

	val windowWidthProperty: DoubleProperty = SimpleDoubleProperty(windowWidth)
		@JsonIgnore get

	var windowHeight: Double
		get() = windowHeightProperty.get()
		set(value) = windowHeightProperty.set(value)

	val windowHeightProperty: DoubleProperty = SimpleDoubleProperty(windowHeight)
		@JsonIgnore get

	var commonSplit: Double
		get() = commonSplitProperty.get()
		set(value) = commonSplitProperty.set(value)

	val commonSplitProperty: DoubleProperty = SimpleDoubleProperty(commonSplit)
		@JsonIgnore get

	var selectedTab: String?
		get() = selectedTabProperty.get()
		set(value) = selectedTabProperty.set(value)

	val selectedTabProperty: StringProperty = SimpleStringProperty(selectedTab)
		@JsonIgnore get

	var previousTab: String?
		get() = previousTabProperty.get()
		set(value) = previousTabProperty.set(value)

	val previousTabProperty: StringProperty = SimpleStringProperty(previousTab)
		@JsonIgnore get

	var proxy: HttpProxy?
		get() = proxyProperty.get()
		set(value) = proxyProperty.set(value)

	val proxyProperty: ObjectProperty<HttpProxy?> = SimpleObjectProperty(proxy)
		@JsonIgnore get

	@Suppress("unused")
	private var _services: Map<String, ServiceConfig>
		@JsonProperty("services") get() = serviceMap
		set(value) = serviceMap.setAll(value)

	val serviceMap: ObservableMap<String, ServiceConfig> = services.toObservableMap()
		@JsonIgnore get

	private val _serviceList: ObservableList<ServiceConfig> = FXCollections.observableArrayList(services.values)

	val services: ReadOnlyObservableList<ServiceConfig> = ReadOnlyObservableList(_serviceList)
		@JsonIgnore get

	@Suppress("unused")
	private var _notifiers: Map<String, NotifierConfig>
		@JsonProperty("notifiers") get() = notifierMap
		set(value) = notifierMap.setAll(value)

	val notifierMap: ObservableMap<String, NotifierConfig> = notifiers.toObservableMap()
		@JsonIgnore get

	private val _notifierList: ObservableList<NotifierConfig> = FXCollections.observableArrayList(notifiers.values)

	val notifiers: ReadOnlyObservableList<NotifierConfig> = ReadOnlyObservableList(_notifierList)
		@JsonIgnore get

	@Suppress("unused")
	private var _silenced: Map<String, SilencedStatusItem>
		@JsonProperty("silenced") get() = silencedMap
		set(value) = silencedMap.setAll(value)

	val silencedMap: ObservableMap<String, SilencedStatusItem> = silenced.toObservableMap()
		@JsonIgnore get

	@field:JsonIgnore private val _silencedList: ObservableList<SilencedStatusItem> = FXCollections
			.observableArrayList(silenced.values)

	val silenced: ReadOnlyObservableList<SilencedStatusItem> = ReadOnlyObservableList(_silencedList)
		@JsonIgnore get

	init {
		serviceMap.addListener(MapChangeListener { change ->
			if (change.wasRemoved()) {
				_serviceList.removeIf { it.uuid == change.key }
			}
			if (change.wasAdded()) _serviceList.add(change.valueAdded)
		})
		notifierMap.addListener(MapChangeListener { change ->
			if (change.wasRemoved()) _notifierList.removeIf { it.uuid == change.key }
			if (change.wasAdded()) _notifierList.add(change.valueAdded)
		})
		silencedMap.addListener(MapChangeListener { change ->
			if (change.wasRemoved()) _silencedList.removeIf { it.item.id == change.key }
			if (change.wasAdded()) _silencedList.add(change.valueAdded)
		})
	}
}
