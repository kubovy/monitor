package com.poterion.monitor.api.ui

import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface Icon {
	val inputStream: InputStream
		get() = this::class.java.getResourceAsStream("icons/${toString().toLowerCase()}.png")
				.use { it.readBytes() }
				.let { ByteArrayInputStream(it) }
}