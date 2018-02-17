package com.poterion.monitor.control.services

import com.poterion.monitor.data.jenkins.JenkinsResponse
import retrofit2.Call
import retrofit2.http.GET

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface JenkinsService {
    @GET("api/json?pretty=1")
    fun check(): Call<JenkinsResponse>
}