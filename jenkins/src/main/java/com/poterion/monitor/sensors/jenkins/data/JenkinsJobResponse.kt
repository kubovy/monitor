package com.poterion.monitor.sensors.jenkins.data

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class JenkinsJobResponse(val name: String = "",
                              val url: String? = null,
                              val color: String? = null)