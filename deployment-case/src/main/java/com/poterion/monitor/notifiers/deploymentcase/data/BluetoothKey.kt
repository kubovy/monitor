package com.poterion.monitor.notifiers.deploymentcase.data

enum class BluetoothKey(val key: String) {
	CONNECTED("connected"),
	TRIGGER("trigger");

	companion object {
		fun get(key: String): BluetoothKey? = BluetoothKey.values().find { it.key == key }
	}
}