package com.poterion.monitor.notifiers.deploymentcase.control

import com.fasterxml.jackson.databind.JsonNode
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Url

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface JenkinsRestService {
	@POST
	fun fire(@Url url: String): Call<Void>

	@GET
	fun get(@Url url: String): Call<JsonNode>

	@GET("job/{name}/wfapi/runs")
	fun buildStatus(@Path("name") jobName: String): Call<JsonNode>
}