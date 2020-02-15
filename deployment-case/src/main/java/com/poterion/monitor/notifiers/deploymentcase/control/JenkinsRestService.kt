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
package com.poterion.monitor.notifiers.deploymentcase.control

import com.fasterxml.jackson.databind.JsonNode
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Url

/**
 * Jenkins REST service for deployment case.
 *
 * @author Jan Kubovy [jan@kubovy.eu]
 */
interface JenkinsRestService {

	/**
	 * Generic POST call.
	 * @param url Full URL to call.
	 */
	@POST
	fun post(@Url url: String): Call<Void>

	/**
	 * Generic GET call.
	 * @param url Full URL to call.
	 */
	@GET
	fun get(@Url url: String): Call<JsonNode>

	/**
	 * Build status call.
	 * @param jobName Name of the job to get the status of.
	 */
	@GET("job/{name}/wfapi/runs")
	fun buildStatus(@Path("name") jobName: String): Call<JsonNode>
}