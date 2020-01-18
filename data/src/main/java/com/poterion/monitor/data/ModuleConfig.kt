package com.poterion.monitor.data

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface ModuleConfig {
	var type: String
	val uuid: String
	var name: String
	var enabled: Boolean
}