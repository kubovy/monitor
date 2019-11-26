package com.poterion.monitor.sensors.alertmanager.ui

import com.poterion.monitor.api.ui.Icon
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class AlertManagerIcon(private val file: String) : Icon {
	ALERT_MANAGER("/icons/alert-manager.png");

	override val inputStream: InputStream
		get() = AlertManagerIcon::class.java.getResourceAsStream(file)
				.use { it.readBytes() }
				.let { ByteArrayInputStream(it) }
}