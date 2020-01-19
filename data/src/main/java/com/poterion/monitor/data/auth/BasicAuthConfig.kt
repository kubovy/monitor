package com.poterion.monitor.data.auth

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class BasicAuthConfig(override var type: String = BasicAuthConfig::class.java.simpleName,
						   var username: String = "",
						   var password: String = "") : AuthConfig
