package com.poterion.monitor.data

import com.poterion.monitor.data.auth.AuthConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class HttpProxy(var address: String? = null, var port: Int? = null, var auth: AuthConfig? = null)