package com.poterion.monitor.sensors.jenkins.data

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class JenkinsResponse(
        val name: String = "",
        val description: String = "",
        val property: List<Any> = emptyList(),
        val url: String = "",
        val jobs: List<JenkinsJobResponse> = emptyList())

