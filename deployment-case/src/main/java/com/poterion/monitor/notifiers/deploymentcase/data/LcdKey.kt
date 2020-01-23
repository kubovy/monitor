package com.poterion.monitor.notifiers.deploymentcase.data

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
enum class LcdKey(val key: Int) {
	MESSAGE(0x50),
	BACKLIGHT(0x51),
	RESET(0x52),
	CLEAR(0x53);

	companion object {
		fun get(key: Int): LcdKey? = values().find { it.key == key }
	}
}