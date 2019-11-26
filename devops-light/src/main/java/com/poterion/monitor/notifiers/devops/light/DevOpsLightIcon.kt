package com.poterion.monitor.notifiers.devops.light

import com.poterion.monitor.api.ui.Icon
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class DevOpsLightIcon(private val file: String) : Icon {
	LOGO("/icons/logo.png"),
	CHIP("/icons/chip.png"),
	DEFAULT("/icons/item-default.png"),
	NON_DEFAULT("/icons/item-non-default.png"),
	DETECT("/icons/detect.png"),
	USB("/icons/usb.png"),
	CONNECTED("/icons/connected.png"),
	DISCONNECTED("/icons/disconnected.png"),
	BLUETOOTH_CONNECTED("/icons/bluetooth-connected.png"),
	BLUETOOTH_DISCONNECTED("/icons/bluetooth-disconnected.png"),
	USB_CONNECTED("/icons/usb-connected.png"),
	USB_DISCONNECTED("/icons/usb-disconnected.png");


	override val inputStream: InputStream
		get() = DevOpsLightIcon::class.java.getResourceAsStream(file)
				.use { it.readBytes() }
				.let { ByteArrayInputStream(it) }
}