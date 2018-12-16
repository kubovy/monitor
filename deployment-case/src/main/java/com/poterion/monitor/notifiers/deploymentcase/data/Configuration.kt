package com.poterion.monitor.notifiers.deploymentcase.data

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class Configuration(var name: String = "",
						 var isActive: Boolean = false,
						 var method: String = "GET",
						 var url: String = "",
						 var username: String = "",
						 var password: String = "",
						 var jobName: String = "",
						 var parameters: String = "",
						 var variables: Map<String, String> = mapOf(),
						 var devices: List<Device> = listOf(),
						 var stateMachine: String = "")