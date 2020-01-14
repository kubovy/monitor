package com.poterion.monitor.api.lib

import com.poterion.monitor.api.ui.CommonIcon
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import javafx.scene.image.Image
import javafx.scene.image.ImageView

/**
 * Converts status item to icon.
 */
fun Status.toIcon() : Icon = when (this) {
	Status.UNKNOWN -> CommonIcon.UNKNOWN
	Status.NONE -> CommonIcon.DEFAULT
	Status.OFF -> CommonIcon.OFF

	Status.OK -> CommonIcon.OK
	Status.INFO -> CommonIcon.INFO
	Status.NOTIFICATION -> CommonIcon.NOTIFICATION
	Status.WARNING -> CommonIcon.WARNING
	Status.ERROR -> CommonIcon.ERROR
	Status.FATAL -> CommonIcon.FATAL

	Status.CONNECTION_ERROR -> CommonIcon.BROKEN_LINK
	Status.SERVICE_ERROR -> CommonIcon.UNAVAILABLE
}

fun Priority.toIcon(): Icon = when (this) {
	Priority.NONE -> CommonIcon.NONE
	Priority.LOW -> CommonIcon.LOW
	Priority.MEDIUM -> CommonIcon.MEDIUM
	Priority.HIGH -> CommonIcon.HIGH
	Priority.MAXIMUM -> CommonIcon.MAXIMUM
}

/**
 * Converts icon to an image.
 *
 * @param width Requested width (default 0)
 * @param height Requested height (default 0)
 */
fun Icon.toImage(width: Int = 0, height: Int = 0): Image = inputStream
		.use { Image(it, width.toDouble(), height.toDouble(), true, true) }

/**
 * Converts icon to an image view.
 *
 * @param width Requested width (default 16)
 * @param height Requested height (default 16)
 */
fun Icon.toImageView(width: Int = 16, height: Int = 16): ImageView = inputStream
		.use { ImageView(Image(it, width.toDouble(), height.toDouble(), true, true)) }