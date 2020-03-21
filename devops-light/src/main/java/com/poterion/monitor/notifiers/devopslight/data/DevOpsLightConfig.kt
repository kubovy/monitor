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
package com.poterion.monitor.notifiers.devopslight.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.poterion.communication.serial.payload.RgbColor
import com.poterion.communication.serial.toHex
import com.poterion.communication.serial.toRGBColor
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.notifiers.AbstractNotifierConfig
import com.poterion.monitor.data.notifiers.NotifierServiceReference
import com.poterion.utils.javafx.toObservableList
import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.ObservableList
import java.util.*

/**
 * Dev/Ops light notifier module configuration.
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
 * @param deviceAddress
 * @param usbPort
 * @param combineMultipleServices
 * @param split
 * @param items
 * @author Jan Kubovy [jan@kubovy.eu]
 */
@Suppress("MemberVisibilityCanBePrivate")
class DevOpsLightConfig(override var type: String = DevOpsLightConfig::class.java.simpleName,
						override var uuid: String = UUID.randomUUID().toString(),
						name: String = "",
						enabled: Boolean = false,
						minPriority: Priority = Priority.LOW,
						minStatus: Status = Status.NONE,
						services: List<NotifierServiceReference> = emptyList(),
						tableColumnWidths: Map<String, Int> = emptyMap(),
						deviceAddress: String = "",
						usbPort: String = "",
						onDemandConnection: Boolean = false,
						combineMultipleServices: Boolean = true,
						split: Double = 0.2,
						items: List<DevOpsLightItemConfig> = emptyList(),
						customColors: List<RgbColor> = emptyList()) :
		AbstractNotifierConfig(name, enabled, minPriority, minStatus, services, tableColumnWidths) {

	var deviceAddress: String
		get() = deviceAddressProperty.get()
		set(value) = deviceAddressProperty.set(value)

	val deviceAddressProperty: StringProperty = SimpleStringProperty(deviceAddress)
		@JsonIgnore get

	var usbPort: String
		get() = usbPortProperty.get()
		set(value) = usbPortProperty.set(value)

	val usbPortProperty: StringProperty = SimpleStringProperty(usbPort)
		@JsonIgnore get

	var onDemandConnection: Boolean
		get() = onDemandConnectionProperty.get()
		set(value) = onDemandConnectionProperty.set(value)

	val onDemandConnectionProperty: BooleanProperty = SimpleBooleanProperty(onDemandConnection)
		@JsonIgnore get

	var combineMultipleServices: Boolean
		get() = combineMultipleServicesProperty.get()
		set(value) = combineMultipleServicesProperty.set(value)

	val combineMultipleServicesProperty: BooleanProperty = SimpleBooleanProperty(combineMultipleServices)
		@JsonIgnore get

	var split: Double
		get() = splitProperty.get()
		set(value) = splitProperty.set(value)

	val splitProperty: DoubleProperty = SimpleDoubleProperty(split)
		@JsonIgnore get

	@Suppress("unused")
	private var _items: List<DevOpsLightItemConfig>
		@JsonProperty("items") get() = items
		set(value) {
			items.setAll(value)
		}

	val items: ObservableList<DevOpsLightItemConfig> = items.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _customColors: List<String>
		@JsonProperty("customColors") get() = customColors.map { it.toHex() }
		set(value) {
			customColors.setAll(value.map { it.toRGBColor() })
		}

	val customColors: ObservableList<RgbColor> = customColors.toObservableList()
		@JsonIgnore get
}