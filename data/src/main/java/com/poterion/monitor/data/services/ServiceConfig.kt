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
package com.poterion.monitor.data.services

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.poterion.monitor.data.HttpConfig
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.Priority
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ObjectProperty
import javafx.collections.ObservableList

/**
 * Service configuration interface.
 *
 * @author Jan Kubovy [jan@kubovy.eu]
 */
interface ServiceConfig<SC> : ModuleConfig, HttpConfig {
	/**
	 * Order of the service in which it will be evaluated.
	 * @see orderProperty
	 */
	var order: Int
		@JsonInclude(value = JsonInclude.Include.ALWAYS) get

	/**
	 * Order property.
	 * @see order
	 */
	val orderProperty: IntegerProperty
		@JsonIgnore get

	/**
	 * Priority of the service used for [items][com.poterion.monitor.data.StatusItem] yield by it unless otherwise
	 * additionally configured.
	 * @see priorityProperty
	 */
	var priority: Priority
		@JsonInclude(value = JsonInclude.Include.ALWAYS) get

	/**
	 * Priority property.
	 * @see priority
	 */
	val priorityProperty: ObjectProperty<Priority>
		@JsonIgnore get

	/**
	 * Interval in which the service will be periodically checked for new [items][com.poterion.monitor.data.StatusItem].
	 * @see checkIntervalProperty
	 */
	var checkInterval: Long?
		@JsonInclude(value = JsonInclude.Include.ALWAYS) get

	/**
	 * Check interval property.
	 * @see checkInterval
	 */
	val checkIntervalProperty: ObjectProperty<Long?>
		@JsonIgnore get

	val subConfig: ObservableList<SC>
		@JsonIgnore get
}
