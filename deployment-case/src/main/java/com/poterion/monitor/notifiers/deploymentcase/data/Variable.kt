package com.poterion.monitor.notifiers.deploymentcase.data

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
data class Variable(var name: String = "",
					var type: VariableType = VariableType.BOOLEAN,
					var value: String = "false")