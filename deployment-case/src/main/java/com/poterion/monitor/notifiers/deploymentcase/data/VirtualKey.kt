package com.poterion.monitor.notifiers.deploymentcase.data

enum class VirtualKey(val key: String) {
	GOTO("goto");

	companion object {
		fun get(key: String): VirtualKey? = VirtualKey.values().find { it.key == key }
	}
}