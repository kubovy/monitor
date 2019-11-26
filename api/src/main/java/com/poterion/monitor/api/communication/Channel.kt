package com.poterion.monitor.api.communication

/**
 * Available channels.
 *
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class Channel(val maxPacketSize: Int) {
	USB(256),
	BLUETOOTH(32)
}