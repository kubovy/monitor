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
package com.poterion.monitor.notifiers.deploymentcase.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.notifiers.AbstractNotifierConfig
import com.poterion.utils.javafx.toObservableList
import javafx.beans.property.*
import javafx.collections.ObservableList
import java.util.*

/**
 * Deployment case notifier module configuration.
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
 * @param debug
 * @param deviceAddress
 * @param testNameHistory
 * @param testValueHistory
 * @param customColors
 * @param split
 * @param configurations
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class DeploymentCaseConfig(override var type: String = DeploymentCaseConfig::class.java.simpleName,
						   override var uuid: String = UUID.randomUUID().toString(),
						   name: String = "",
						   enabled: Boolean = false,
						   minPriority: Priority = Priority.LOW,
						   minStatus: Status = Status.NONE,
						   services: List<String> = emptyList(),
						   tableColumnWidths: Map<String, Int> = emptyMap(),
						   debug: Boolean = false,
						   deviceAddress: String = "",
						   testNameHistory: List<String> = emptyList(),
						   testValueHistory: List<String> = emptyList(),
						   customColors: List<String> = emptyList(),
						   split: Double = 0.2,
						   configurations: List<Configuration> = emptyList()) :
		AbstractNotifierConfig(name, enabled, minPriority, minStatus, services, tableColumnWidths) {

	var debug: Boolean
		get() = debugProperty.get()
		set(value) = debugProperty.set(value)

	val debugProperty: BooleanProperty = SimpleBooleanProperty(debug)
		@JsonIgnore get

	var deviceAddress: String
		get() = deviceAddressProperty.get()
		set(value) = deviceAddressProperty.set(value)

	val deviceAddressProperty: StringProperty = SimpleStringProperty(deviceAddress)
		@JsonIgnore get

	@Suppress("unused")
	private var _testNameHistory: List<String>
		@JsonProperty("testNameHistory") get() = testNameHistory
		set(value) {
			testNameHistory.setAll(value)
		}

	val testNameHistory: ObservableList<String> = testNameHistory.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _testValueHistory: List<String>
		@JsonProperty("testValueHistory") get() = testValueHistory
		set(value) {
			testValueHistory.setAll(value)
		}

	val testValueHistory: ObservableList<String> = testValueHistory.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _customColors: List<String>
		@JsonProperty("customColors") get() = customColors
		set(value) {
			customColors.setAll(value)
		}

	val customColors: ObservableList<String> = customColors.toObservableList()
		@JsonIgnore get

	var split: Double
		get() = splitProperty.get()
		set(value) = splitProperty.set(value)

	val splitProperty: DoubleProperty = SimpleDoubleProperty(split)
		@JsonIgnore get

	@Suppress("unused")
	private var _configurations: List<Configuration>
		@JsonProperty("configurations") get() = configurations
		set(value) {
			configurations.setAll(value)
		}

	val configurations: ObservableList<Configuration> = configurations.toObservableList()
		@JsonIgnore get
}