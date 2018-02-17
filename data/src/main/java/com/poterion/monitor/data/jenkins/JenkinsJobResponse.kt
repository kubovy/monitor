package com.poterion.monitor.data.jenkins

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class JenkinsJobResponse(val name: String = "",
                              val url: String? = null,
                              val color: String? = null)