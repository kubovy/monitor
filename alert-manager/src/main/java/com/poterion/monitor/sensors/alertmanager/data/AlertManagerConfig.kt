package com.poterion.monitor.sensors.alertmanager.data

import com.poterion.monitor.data.HttpProxy
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.auth.AuthConfig
import com.poterion.monitor.data.services.ServiceConfig
import java.util.*

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class AlertManagerConfig(override var type: String = AlertManagerConfig::class.java.simpleName,
						 override val uuid: String = UUID.randomUUID().toString(),
						 override var name: String = "",
						 override var enabled: Boolean = true,
						 override var url: String = "",
						 override var proxy: HttpProxy? = null,
						 override var trustCertificate: Boolean = false,
						 override var auth: AuthConfig? = null,
						 override var order: Int = Int.MAX_VALUE,
						 override var priority: Priority = Priority.NONE,
						 override var checkInterval: Long? = null,
						 override var connectTimeout: Long? = null,
						 override var readTimeout: Long? = null,
						 override var writeTimeout: Long? = null,
						 override var tableColumnWidths: MutableMap<String, Int> = mutableMapOf(),
						 var nameRefs: Set<String> = emptySet(),
						 var descriptionRefs: Set<String> = emptySet(),
						 var receivers: Set<String> = emptySet(),
						 var labelFilter: Set<String> = emptySet(),
						 var labels: MutableCollection<AlertManagerLabelConfig> = mutableListOf()) : ServiceConfig