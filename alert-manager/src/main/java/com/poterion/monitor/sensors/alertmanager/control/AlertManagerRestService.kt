package com.poterion.monitor.sensors.alertmanager.control

import com.poterion.monitor.sensors.alertmanager.data.AlertManagerResponse
import retrofit2.Call
import retrofit2.http.GET

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface AlertManagerRestService {
	/**
	 * The "api/" prefix is not included to support overwrites.
	 */
	@GET("v2/alerts?active=true&silenced=true&inhibited=true&unprocessed=true") // &receiver=devops-light
	fun check(): Call<List<AlertManagerResponse>>
}