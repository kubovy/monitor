package com.poterion.monitor.notifiers.deploymentcase.data

data class Variable(var name: String = "",
					var type: VariableType = VariableType.BOOLEAN,
					var value: String = "false")