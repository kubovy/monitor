package com.poterion.monitor.notifications

import com.poterion.monitor.api.ui.Icon
import javafx.scene.image.Image
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class NotificationsIcon(private val file: String) : Icon {
	NOTIFICATIONS("/icons/notifications.png");

	override val inputStream: InputStream
		get() = NotificationsIcon::class.java.getResourceAsStream(file)
				.use { it.readBytes() }
				.let { ByteArrayInputStream(it) }
}