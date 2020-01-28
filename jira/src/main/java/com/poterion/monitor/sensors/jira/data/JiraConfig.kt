package com.poterion.monitor.sensors.jira.data

import com.poterion.monitor.data.HttpProxy
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.auth.AuthConfig
import com.poterion.monitor.data.services.ServiceConfig
import java.util.*

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class JiraConfig(override var type: String = JiraConfig::class.java.simpleName,
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
				 val priorityMapping: MutableMap<String, Priority> = mutableMapOf(
						 "Minor" to Priority.NONE,
						 "Trivial" to Priority.NONE,
						 "Lowest" to Priority.LOW,
						 "Low" to Priority.LOW,
						 "Medium" to Priority.MEDIUM,
						 "High" to Priority.HIGH,
						 "Highest" to Priority.MAXIMUM,
						 "Blocker" to Priority.MAXIMUM),
				 val statusMapping: MutableMap<String, Status> = mutableMapOf(
						 "Public Incident" to Status.FATAL,
						 "Incident" to Status.FATAL,
						 "Problem" to Status.ERROR,
						 "Bug" to Status.ERROR,
						 "Open" to Status.WARNING,
						 "Pending" to Status.WARNING,
						 "Waiting for Customer" to Status.NOTIFICATION,
						 "In Progress" to Status.INFO,
						 "Complete" to Status.OK,
						 "Canceled" to Status.OK,
						 "Closed" to Status.NONE),
				 val queries: MutableSet<JiraQueryConfig> = mutableSetOf()) : ServiceConfig