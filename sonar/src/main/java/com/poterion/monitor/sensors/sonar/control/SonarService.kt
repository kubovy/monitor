package com.poterion.monitor.sensors.sonar.control

import com.poterion.monitor.sensors.sonar.data.SonarProjectResponse
import retrofit2.Call
import retrofit2.http.GET

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface SonarService {
    @GET("api/resources?metrics=alert_status&depth=-1&scopes=PRJ&limit=2000&format=json")
    fun check(): Call<Collection<SonarProjectResponse>>
}