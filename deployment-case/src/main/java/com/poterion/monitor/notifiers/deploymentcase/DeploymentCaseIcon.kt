package com.poterion.monitor.notifiers.deploymentcase

import com.poterion.monitor.api.ui.Icon
import javafx.scene.image.Image
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class DeploymentCaseIcon(private val file: String) : Icon {
	NUCLEAR_FOOTBALL("/icons/nuclear-football.png"),
	BLUETOOTH("/icons/bluetooth.png"),
	CONNECTED("/icons/connected.png"),
	DISCONNECTED("/icons/disconnected.png");

	override val inputStream: InputStream
		get() = DeploymentCaseIcon::class.java.getResourceAsStream(file)
				.use { it.readBytes() }
				.let { ByteArrayInputStream(it) }

	override fun image(width: Int, height: Int): Image = inputStream
			.use { Image(it, width.toDouble(), height.toDouble(), false, true) }
}