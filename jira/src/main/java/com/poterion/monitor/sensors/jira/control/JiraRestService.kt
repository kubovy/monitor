package com.poterion.monitor.sensors.jira.control

import com.poterion.monitor.sensors.jira.data.JiraSearchRequestBody
import com.poterion.monitor.sensors.jira.data.JiraSearchResult
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface JiraRestService {
	@POST("/rest/api/2/search")
	fun search(@Body body: JiraSearchRequestBody): Call<JiraSearchResult>
}