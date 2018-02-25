package com.poterion.monitor.api.ui

import javafx.scene.image.Image
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class CommonIcon(private val file: String) : Icon {
	APPLICATION("/icons/application.png"),
	DEVICE("/icons/device.png"),
	SETTINGS("/icons/settings.png"),
	UNDER_CONSTRUCTION("/icons/under-construction.png"),

	ACTIVE("/icons/active.png"),
	INACTIVE("/icons/inactive.png"),
	UNAVAILABLE("/icons/unavailable.png"),
	BROKEN_LINK("/icons/broken-link.png"),

	NONE("/icons/none.png"),
	DEFAULT("/icons/default.png"),
	OFF("/icons/off.png"),
	OK("/icons/ok.png"),
	UNKNOWN("/icons/unknown.png"),
	INFO("/icons/info.png"),
	NOTIFICATION("/icons/notification.png"),
	WARNING("/icons/warning.png"),
	ERROR("/icons/error.png"),
	FATAL("/icons/fatal.png");

	override val inputStream: InputStream
		get() = CommonIcon::class.java.getResourceAsStream(file)
				.use { it.readBytes() }
				.let { ByteArrayInputStream(it) }

	override fun image(width: Int, height: Int): Image = inputStream
			.use { Image(it, width.toDouble(), height.toDouble(), false, true) }
}