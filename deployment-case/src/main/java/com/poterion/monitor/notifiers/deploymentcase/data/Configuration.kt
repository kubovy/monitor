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

import com.poterion.monitor.data.HttpConfig
import com.poterion.monitor.data.auth.AuthConfig

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
data class Configuration(var name: String = "",
						 var isActive: Boolean = false,
						 var method: String = "GET",
						 override var url: String = "",
						 override var trustCertificate: Boolean = false,
						 override var auth: AuthConfig? = null,
						 override var connectTimeout: Long? = null,
						 override var readTimeout: Long? = null,
						 override var writeTimeout: Long? = null,
						 var jobName: String = "",
						 var parameters: String = "",
						 var variables: List<Variable> = emptyList(),
						 var devices: List<Device> = emptyList(),
						 var jobStatus: Map<String, String> = emptyMap(),
						 var pipelineStatus: Map<String, String> = emptyMap(),
						 var stateMachine: List<State> = emptyList()): HttpConfig