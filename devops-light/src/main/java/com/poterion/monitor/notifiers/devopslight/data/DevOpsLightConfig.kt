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

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.notifiers.NotifierConfig
import java.util.*

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
data class DevOpsLightConfig(override var type: String = DevOpsLightConfig::class.java.simpleName,
							 override var uuid: String = UUID.randomUUID().toString(),
							 override var name: String = "",
							 override var enabled: Boolean = false,
							 override var minPriority: Priority = Priority.LOW,
							 override var minStatus: Status = Status.NONE,
							 override val services: MutableSet<String> = mutableSetOf(),
							 override var tableColumnWidths: MutableMap<String, Int> = mutableMapOf(),
							 var deviceAddress: String = "",
							 var usbPort: String = "",
							 var grbColors: Boolean = false,
							 var combineMultipleServices: Boolean = true,
							 var split: Double = 0.2,
							 val items: MutableCollection<DevOpsLightItemConfig> = mutableListOf()) : NotifierConfig