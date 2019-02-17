package com.poterion.monitor.api.communication

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface BluetoothRaspiListener {
	/** On inbound connection established callback */
	fun onInboundConnect() {
	}

	/** On inbound connection lost callback */
	fun onInboundDisconnect() {
	}

	/** On outbound connection established callback */
	fun onOutboundConnect() {
	}

	/** On outbound connection lost callback */
	fun onOutboundDisconnect() {
	}

	/**
	 * On message callback
	 *
	 * @param message Received message
	 */
	fun onMessage(message: String) {
	}
}