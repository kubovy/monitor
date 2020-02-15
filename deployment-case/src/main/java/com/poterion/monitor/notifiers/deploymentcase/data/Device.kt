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

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
data class Device(var name: String = "",
				  var kind: DeviceKind = DeviceKind.MCP23017,
				  var key: String = "") {

	val type: VariableType
		@JsonIgnore
		get() = when (kind) {
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
}

