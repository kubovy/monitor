package com.poterion.monitor.sensors.sonar.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.auth.BasicAuthConfig
import com.poterion.monitor.data.services.ServiceConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class SonarConfig(override var type: String = SonarConfig::class.java.simpleName,
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
				  var projects: Collection<SonarProjectConfig> = emptyList(),
				  var filter: String? = null) : ServiceConfig
