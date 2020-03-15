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
import javafx.beans.binding.Bindings
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import java.util.concurrent.Callable

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class Device(name: String = "",
			 kind: DeviceKind = DeviceKind.MCP23017,
			 key: String = "") {

	var name: String
		get() = nameProperty.get()
		set(value) = nameProperty.set(value)

	val nameProperty: StringProperty = SimpleStringProperty(name)
		@JsonIgnore get

	var kind: DeviceKind
		get() = kindProperty.get()
		set(value) = kindProperty.set(value)

	val kindProperty: ObjectProperty<DeviceKind> = SimpleObjectProperty(kind)
		@JsonIgnore get

	var key: String
		get() = keyProperty.get()
		set(value) = keyProperty.set(value)

	val keyProperty: StringProperty = SimpleStringProperty(key)
		@JsonIgnore get

	val type: VariableType
		@JsonIgnore get() = when (kind) {
			DeviceKind.MCP23017 -> VariableType.BOOLEAN
			DeviceKind.WS281x -> VariableType.COLOR_PATTERN
			DeviceKind.BLUETOOTH -> VariableType.BOOLEAN
			DeviceKind.LCD -> when (key) {
				"${LcdKey.MESSAGE.key}" -> VariableType.STRING
				else -> VariableType.BOOLEAN
			}
			DeviceKind.VIRTUAL -> when (key) {
				VirtualKey.GOTO.key -> VariableType.STATE
				VirtualKey.ENTER.key -> VariableType.ENTER
				else -> VariableType.BOOLEAN
			}
		}

	val typeProperty: ObjectBinding<VariableType>
		@JsonIgnore get() = Bindings.createObjectBinding<VariableType>(Callable { type }, kindProperty)
}

