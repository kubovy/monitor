package com.poterion.monitor.ui

import com.poterion.monitor.api.ui.Icon
import javafx.scene.image.Image
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class UiIcon(private val file: String) : Icon {
	SERVICES("/icons/services.png"),
	NOTIFIERS("/icons/notifiers.png");

	override val inputStream: InputStream
		get() = UiIcon::class.java.getResourceAsStream(file)
				.use { it.readBytes() }
				.let { ByteArrayInputStream(it) }
}