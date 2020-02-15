package com.poterion.monitor.sensors.storyboard.data

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.auth.AuthConfig
import com.poterion.monitor.data.services.ServiceConfig
import java.util.*

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class StoryboardConfig(override var type: String = StoryboardConfig::class.java.simpleName,
					   override var uuid: String = UUID.randomUUID().toString(),
					   override var name: String = "",
					   override var enabled: Boolean = false,
					   override var url: String = "",
					   override var trustCertificate: Boolean = false,
					   override var auth: AuthConfig? = null,
					   override var order: Int = Int.MAX_VALUE,
					   override var priority: Priority = Priority.NONE,
					   override var checkInterval: Long? = null,
					   override var connectTimeout: Long? = null,
					   override var readTimeout: Long? = null,
					   override var writeTimeout: Long? = null,
					   override var tableColumnWidths: MutableMap<String, Int> = mutableMapOf(),
					   var projects: MutableSet<StoryboardProjectConfig> = mutableSetOf()): ServiceConfig