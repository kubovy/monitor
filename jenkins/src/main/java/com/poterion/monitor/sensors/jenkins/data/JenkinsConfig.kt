package com.poterion.monitor.sensors.jenkins.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.auth.BasicAuthConfig
import com.poterion.monitor.data.services.ServiceConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class JenkinsConfig(override var type: String = JenkinsConfig::class.java.simpleName,
					override var name: String = "",
					override var enabled: Boolean = true,
					override var url: String = "",
					override var trustCertificate: Boolean = false,
					override var auth: BasicAuthConfig? = null,
					override var order: Int = Int.MAX_VALUE,
					override var priority: Priority = Priority.NONE,
					override var checkInterval: Long = 3600_000L, // 1 hour
					override var connectTimeout: Long? = null,
					override var readTimeout: Long? = null,
					override var writeTimeout: Long? = null,
					var jobs: MutableCollection<JenkinsJobConfig> = mutableListOf(),
					var filter: String? = null) : ServiceConfig