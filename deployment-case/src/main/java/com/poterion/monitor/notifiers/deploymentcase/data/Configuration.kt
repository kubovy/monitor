package com.poterion.monitor.notifiers.deploymentcase.data

import com.poterion.monitor.data.auth.BasicAuthConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class Configuration(var name: String = "",
						 var isActive: Boolean = false,
						 var method: String = "GET",
						 var url: String = "",
						 var auth: BasicAuthConfig? = null,
						 var jobName: String = "",
						 var parameters: String = "",
						 var variables: List<Variable> = emptyList(),
						 var devices: List<Device> = emptyList(),
						 var jobStatus: Map<String, String> = emptyMap(),
						 var pipelineStatus: Map<String, String> = emptyMap(),
						 var stateMachine: List<State> = emptyList())