package com.poterion.monitor.data.services

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.poterion.monitor.data.HttpConfig
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.Priority

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface ServiceConfig : ModuleConfig, HttpConfig {
	var order: Int
	var priority: Priority
	var checkInterval: Long
}
