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
package com.poterion.monitor.sensors.jira.control

import com.poterion.monitor.sensors.jira.data.JiraSearchRequestBody
import com.poterion.monitor.sensors.jira.data.JiraSearchResult
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
interface JiraRestService {
	@POST("/rest/api/2/search")
	fun search(@Body body: JiraSearchRequestBody): Call<JiraSearchResult>
}