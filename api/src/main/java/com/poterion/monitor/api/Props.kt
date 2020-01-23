package com.poterion.monitor.api

import java.util.*

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
object Props {
	private val values = Properties().apply {
		Props::class.java.getResource("/application.properties")
				?.openStream()
				?.use { Properties().apply { load(it) } }
				?.also { putAll(it) }
		Props::class.java.getResource("/version.properties")
				?.openStream()
				?.use { Properties().apply { load(it) } }
				?.also { putAll(it) }
	}

	val APP_NAME = Props["app.name"]
	val VERSION = Props["app.version"]

	operator fun get(key: String) = values[key]
}