package com.poterion.monitor.data

import com.poterion.monitor.data.auth.AuthConfig
import com.poterion.monitor.data.auth.BasicAuthConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface HttpConfig {
	var url: String
	var trustCertificate: Boolean
	var auth: BasicAuthConfig?
	var connectTimeout: Long?
	var readTimeout: Long?
	var writeTimeout: Long?
}