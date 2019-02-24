package com.poterion.monitor.api.communication

/**
 * Bluetooth listener interface.
 *
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface BluetoothEmbeddedListener {
	/** On connecting callback */
	fun onConnecting() {
	}

	/** On connection established callback */
	fun onConnect() {
	}

	/** On connection lost callback */
	fun onDisconnect() {
	}

	/**
	 * On message callback
	 *
	 * @param message Received message
	 */
	fun onMessage(message: ByteArray) {
	}
}