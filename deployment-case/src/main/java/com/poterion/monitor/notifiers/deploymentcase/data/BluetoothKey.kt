package com.poterion.monitor.notifiers.deploymentcase.data

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class BluetoothKey(val key: String) {
	CONNECTED("connected"),
	TRIGGER("trigger");

	companion object {
		fun get(key: String): BluetoothKey? = values().find { it.key == key }
	}
}