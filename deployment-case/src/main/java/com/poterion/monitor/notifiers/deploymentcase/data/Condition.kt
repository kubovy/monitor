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
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon
import com.poterion.monitor.notifiers.deploymentcase.control.toDevice
import com.poterion.monitor.notifiers.deploymentcase.getDisplayName
import com.poterion.monitor.notifiers.deploymentcase.toVariable

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
data class Condition(var device: Int? = null,
					 var value: String? = null) : StateMachineItem {

	@JsonIgnore
	override fun getTitle(devices: Collection<Device>?, variables: Collection<Variable>?): String =
			"${device?.toDevice(devices)?.getDisplayName()} == ${value?.toVariable(variables)?.getDisplayName()}"

	override val icon: DeploymentCaseIcon?
		@JsonIgnore
		get() = DeploymentCaseIcon.CONDITION

	override fun isBinarySame(other: StateMachineItem, devices: Collection<Device>?, variables: Collection<Variable>?):
			Boolean = (other is Condition
			&& device?.toDevice(devices)?.kind == other.device?.toDevice(devices)?.kind
			&& device?.toDevice(devices)?.key == other.device?.toDevice(devices)?.key
			&& value?.toVariable(variables)?.type == other.value?.toVariable(variables)?.type
			&& value?.toVariable(variables)?.value == other.value?.toVariable(variables)?.value)
			|| (other is Action
			&& device?.toDevice(devices)?.kind == other.device?.toDevice(devices)?.kind
			&& device?.toDevice(devices)?.key == other.device?.toDevice(devices)?.key
			&& value?.toVariable(variables)?.type == other.value?.toVariable(variables)?.type
			&& value?.toVariable(variables)?.value == other.value?.toVariable(variables)?.value)
}