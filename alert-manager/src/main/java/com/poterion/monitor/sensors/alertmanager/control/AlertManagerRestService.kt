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
package com.poterion.monitor.sensors.alertmanager.control

import com.poterion.monitor.sensors.alertmanager.data.AlertManagerResponse
import retrofit2.Call
import retrofit2.http.GET

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
interface AlertManagerRestService {
	/**
	 * The "api/" prefix is not included to support overwrites.
	 */
	@GET("v2/alerts?active=true&silenced=false&inhibited=false&unprocessed=true") // &receiver=devops-light
	fun check(): Call<List<AlertManagerResponse>>
}