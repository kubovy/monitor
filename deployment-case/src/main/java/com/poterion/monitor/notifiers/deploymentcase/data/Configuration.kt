package com.poterion.monitor.notifiers.deploymentcase.data

import com.poterion.monitor.data.HttpConfig
import com.poterion.monitor.data.HttpProxy
import com.poterion.monitor.data.auth.AuthConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class Configuration(var name: String = "",
						 var isActive: Boolean = false,
						 var method: String = "GET",
						 override var url: String = "",
						 override var proxy: HttpProxy? = null,
						 override var trustCertificate: Boolean = false,
						 override var auth: AuthConfig? = null,
						 override var connectTimeout: Long? = null,
						 override var readTimeout: Long? = null,
						 override var writeTimeout: Long? = null,
						 var jobName: String = "",
						 var parameters: String = "",
						 var variables: List<Variable> = emptyList(),
						 var devices: List<Device> = emptyList(),
						 var jobStatus: Map<String, String> = emptyMap(),
						 var pipelineStatus: Map<String, String> = emptyMap(),
						 var stateMachine: List<State> = emptyList()) : HttpConfig