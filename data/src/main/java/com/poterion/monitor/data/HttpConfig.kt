package com.poterion.monitor.data

import com.poterion.monitor.data.auth.AuthConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface HttpConfig {
	var url: String
	var proxy: HttpProxy?
	var trustCertificate: Boolean
	var auth: AuthConfig?
	var connectTimeout: Long?
	var readTimeout: Long?
	var writeTimeout: Long?
}