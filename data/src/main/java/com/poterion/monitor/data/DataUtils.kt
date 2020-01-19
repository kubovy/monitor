package com.poterion.monitor.data

import com.poterion.monitor.data.services.ServiceConfig
import java.util.*

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */

fun StatusItem.service(services: Map<String, ServiceConfig>) = services[serviceId]

fun StatusItem.service(applicationConfiguration: ApplicationConfiguration) = service(applicationConfiguration.services)

fun StatusItem.serviceName(services: Map<String, ServiceConfig>): String = service(services)?.name ?: ""

fun StatusItem.serviceName(applicationConfiguration: ApplicationConfiguration) = serviceName(applicationConfiguration.services)

val StatusItem.key: String
	get() = "${serviceId}-${title}"

fun Map<String, ModuleConfig>.nextUUID(): String {
	var uuid: String
	do {
		uuid = UUID.randomUUID().toString()
	} while (keys.contains(uuid))
	return uuid
}