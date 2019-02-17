package com.poterion.monitor.notifiers.deploymentcase.data

enum class LcdKey(val key: String) {
	MESSAGE("message"),
	BACKLIGHT("backlight"),
	RESET("reset"),
	CLEAR("clear");

	companion object {
		fun get(key: String): LcdKey? = LcdKey.values().find { it.key == key }
	}
}