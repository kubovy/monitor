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
package com.poterion.monitor.data

import com.poterion.monitor.data.auth.AuthConfig
import com.poterion.monitor.data.auth.BasicAuthConfig
import com.poterion.monitor.data.auth.TokenAuthConfig
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.services.ServiceConfig
import java.util.*

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */

fun StatusItem.service(services: Map<String, ServiceConfig>) = services[serviceId]

fun StatusItem.service(applicationConfiguration: ApplicationConfiguration) = service(applicationConfiguration.services)

fun StatusItem.serviceName(services: Map<String, ServiceConfig>): String = service(services)?.name ?: ""

fun StatusItem.serviceName(applicationConfiguration: ApplicationConfiguration) = serviceName(applicationConfiguration.services)

val StatusItem.key: String
	get() = "${serviceId}-${title}"

fun Map<String, ModuleConfig>.nextUUID(): String {
	var uuid: String
	do {
		uuid = UUID.randomUUID().toString()
	} while (keys.contains(uuid))
	return uuid
}

fun AuthConfig.toHeaderString() = when (this) {
	is BasicAuthConfig -> "Basic " + Base64.getEncoder().encodeToString("${username}:${password}".toByteArray())
	is TokenAuthConfig -> "Bearer ${token}"
	else -> null
}