package com.poterion.monitor.api.utils

import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File
import java.net.URI

private val LOGGER = LoggerFactory.getLogger("Utils")
private val desktop = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null

fun open(uri: URI) = desktop?.takeIf { it.isSupported(Desktop.Action.BROWSE) }
		?.also { it.browse(uri) } != null

fun open(file: File) = desktop?.takeIf { it.isSupported(Desktop.Action.OPEN) }
		?.also { it.open(file) } != null

fun open(path: String): Boolean = try {
	val uri = URI(path)
	if (uri.scheme.startsWith("file")) {
		open(File(uri))
	} else {
		open(uri)
	}
	true
} catch (e: Exception) {
	LOGGER.warn("Can't open ${path}!", e)
	false
}

fun noop() {
	// This is a no-op helper
}
