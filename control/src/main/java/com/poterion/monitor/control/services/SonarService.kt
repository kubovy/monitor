package com.poterion.monitor.control.services

import com.poterion.monitor.data.sonar.SonarProjectResponse
import retrofit2.Call
import retrofit2.http.GET

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface SonarService {
    @GET("api/resources?metrics=alert_status&depth=-1&scopes=PRJ&limit=2000&format=json")
    fun check(): Call<Collection<SonarProjectResponse>>
}