package com.poterion.monitor.data.auth

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class BasicAuthConfig(var username: String = "",
                           var password: String = "") : AuthConfig
