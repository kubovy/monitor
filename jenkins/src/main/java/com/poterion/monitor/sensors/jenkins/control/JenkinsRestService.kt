package com.poterion.monitor.sensors.jenkins.control

import com.poterion.monitor.sensors.jenkins.data.JenkinsResponse
import retrofit2.Call
import retrofit2.http.GET

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
interface JenkinsRestService {
	@GET("api/json?pretty=1")
	fun check(): Call<JenkinsResponse>
}