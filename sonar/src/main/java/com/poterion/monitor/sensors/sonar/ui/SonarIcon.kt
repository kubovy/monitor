package com.poterion.monitor.sensors.sonar.ui

import com.poterion.monitor.api.ui.Icon
import javafx.scene.image.Image
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class SonarIcon(private val file: String) : Icon {
	SONAR("/icons/sonar.png");

	override val inputStream: InputStream
		get() = SonarIcon::class.java.getResourceAsStream(file)
				.use { it.readBytes() }
				.let { ByteArrayInputStream(it) }
}