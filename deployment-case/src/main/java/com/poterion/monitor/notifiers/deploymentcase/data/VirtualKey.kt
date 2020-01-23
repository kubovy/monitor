package com.poterion.monitor.notifiers.deploymentcase.data

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
enum class VirtualKey(val key: String, val condition: Boolean) {
	GOTO("goto", false),
	ENTER("enter", false),
	ENTERED("entered", true),
	ABORTED("aborted", true);

	companion object {
		fun get(key: String): VirtualKey? = values().find { it.key == key }
	}
}