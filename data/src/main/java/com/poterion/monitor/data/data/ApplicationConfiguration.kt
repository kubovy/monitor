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

import com.fasterxml.jackson.annotation.JsonInclude
import com.poterion.monitor.data.HttpProxy
import com.poterion.monitor.data.notifiers.NotifierConfig
import com.poterion.monitor.data.services.ServiceConfig

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
@JsonInclude(value = JsonInclude.Include.NON_NULL, content = JsonInclude.Include.NON_NULL)
data class ApplicationConfiguration(
		var btDiscovery: Boolean = false,
		var showOnStartup: Boolean = true,
		var startMinimized: Boolean = false,
		var windowWidth: Double = 1200.0,
		var windowHeight: Double = 1000.0,
		var commonSplit: Double = 0.3,
		var selectedTab: String? = null,
		var previousTab: String? = null,
		var proxy: HttpProxy? = null,
		val services: MutableMap<String, ServiceConfig> = mutableMapOf(),
		val notifiers: MutableMap<String, NotifierConfig> = mutableMapOf(),
		val silenced: MutableMap<String, SilencedStatusItem> = mutableMapOf())
