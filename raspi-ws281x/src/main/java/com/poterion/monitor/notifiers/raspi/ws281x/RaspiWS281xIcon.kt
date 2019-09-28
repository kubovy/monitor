package com.poterion.monitor.notifiers.raspi.ws281x

import com.poterion.monitor.api.ui.Icon
import javafx.scene.image.Image
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class RaspiWS281xIcon(private val file: String) : Icon {
	CHIP("/icons/chip.png"),
	RASPBERRY("/icons/raspberry.png"),
	RASPBERRY_PI("/icons/raspberrypi.png"),
	DEFAULT("/icons/item-default.png"),
	NON_DEFAULT("/icons/item-non-default.png"),
	DETECT("/icons/detect.png"),
	USB("/icons/usb.png"),
	CONNECTED("/icons/connected.png"),
	DISCONNECTED("/icons/disconnected.png");


	override val inputStream: InputStream
		get() = RaspiWS281xIcon::class.java.getResourceAsStream(file)
				.use { it.readBytes() }
				.let { ByteArrayInputStream(it) }

	override fun image(width: Int, height: Int): Image = inputStream
			.use { Image(it, width.toDouble(), height.toDouble(), false, true) }
}