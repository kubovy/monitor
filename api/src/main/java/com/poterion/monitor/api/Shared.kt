package com.poterion.monitor.api

import java.io.File

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
object Shared {
	/** Configuration directory. */
	var configDirectory: File = File(System.getProperty("user.home"))
			.resolve(".config")
			.resolve("poterion-monitor")

	/** Configuration file. */
	var configFile: File = File("config.yaml")
		get() = if (field.isAbsolute) field else configDirectory.resolve(field).absoluteFile

	/** Cache file. */
	var cacheFile: File = File("cache.yaml")
		get() = if (field.isAbsolute) field else configDirectory.resolve(field).absoluteFile
}