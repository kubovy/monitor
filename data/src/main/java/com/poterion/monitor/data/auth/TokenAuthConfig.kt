package com.poterion.monitor.data.auth

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class TokenAuthConfig(override var type: String = TokenAuthConfig::class.java.simpleName,
						   var token: String = "") : AuthConfig
