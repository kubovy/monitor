package com.poterion.monitor.api.communication

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface BluetoothEmbeddedListener {
	/** On connection established callback */
	fun onConnect() {
	}

	/** On connection lost callback */
	fun onDisconnect() {
	}

	/**
	 * On message callback
	 *
	 * @param kind Message kind
	 * @param message Received message
	 */
	fun onMessage(kind: BluetoothMessageKind, message: ByteArray) {
	}

	fun onProgress(progress: Int, count: Int, disable: Boolean) {
	}

	fun onStateMachine(stateMachine: ByteArray) {
	}
}