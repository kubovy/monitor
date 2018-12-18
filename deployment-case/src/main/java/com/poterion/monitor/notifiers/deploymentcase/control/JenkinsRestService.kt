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
 * @author Jan Kubovy <jan@kubovy.eu>
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