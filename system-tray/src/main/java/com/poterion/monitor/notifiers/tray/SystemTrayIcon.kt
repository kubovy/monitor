package com.poterion.monitor.notifiers.tray

import com.poterion.monitor.api.ui.Icon
import javafx.scene.image.Image
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class SystemTrayIcon(private val file: String) : Icon {
	TRAY("/icons/tray.png"),
	ABOUT("/icons/about.png"),
	REFRESH("/icons/refresh.png"),
	QUIT("/icons/quit.png");

	override val inputStream: InputStream
		get() = SystemTrayIcon::class.java.getResourceAsStream(file)
				.use { it.readBytes() }
				.let { ByteArrayInputStream(it) }

	override fun image(width: Int, height: Int): Image = inputStream
			.use { Image(it, width.toDouble(), height.toDouble(), false, true) }
}