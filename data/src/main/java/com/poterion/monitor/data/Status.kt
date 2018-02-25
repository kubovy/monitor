package com.poterion.monitor.data

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class Status {
	NONE,
	OFF,
	UNKNOWN,
	OK,
	INFO,
	NOTIFICATION,
	CONNECTION_ERROR,
	SERVICE_ERROR,
	WARNING,
	ERROR,
	FATAL
}