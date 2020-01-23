package com.poterion.monitor.data

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
enum class Status {
	NONE,
	OFF,
	UNKNOWN,
	OK,
	INFO,
	NOTIFICATION,
	WARNING,
	ERROR,
	CONNECTION_ERROR,
	SERVICE_ERROR,
	FATAL
}