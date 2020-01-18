package com.poterion.monitor.notification.tabs

import com.poterion.monitor.api.ui.Icon
import javafx.scene.image.Image
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class NotificationTabsIcon(private val file: String) : Icon {
	TABS("/icons/tabs.png");

	override val inputStream: InputStream
		get() = NotificationTabsIcon::class.java.getResourceAsStream(file)
				.use { it.readBytes() }
				.let { ByteArrayInputStream(it) }
}