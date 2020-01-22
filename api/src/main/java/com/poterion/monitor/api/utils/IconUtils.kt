package com.poterion.monitor.api.utils

import com.poterion.monitor.api.CommonIcon
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import javafx.scene.image.Image
import javafx.scene.image.ImageView

/**
 * Converts status item to icon.
 */
fun Status.toIcon() : Icon = when (this) {
	Status.UNKNOWN -> CommonIcon.STATUS_UNKNOWN
	Status.NONE -> CommonIcon.STATUS_NONE
	Status.OFF -> CommonIcon.STATUS_OFF

	Status.OK -> CommonIcon.STATUS_OK
	Status.INFO -> CommonIcon.STATUS_INFO
	Status.NOTIFICATION -> CommonIcon.STATUS_NOTIFICATION
	Status.WARNING -> CommonIcon.STATUS_WARNING
	Status.ERROR -> CommonIcon.STATUS_ERROR
	Status.FATAL -> CommonIcon.STATUS_FATAL

	Status.CONNECTION_ERROR -> CommonIcon.BROKEN_LINK
	Status.SERVICE_ERROR -> CommonIcon.UNAVAILABLE
}

fun Priority.toIcon(): Icon = when (this) {
	Priority.NONE -> CommonIcon.PRIORITY_NONE
	Priority.LOW -> CommonIcon.PRIORITY_LOW
	Priority.MEDIUM -> CommonIcon.PRIORITY_MEDIUM
	Priority.HIGH -> CommonIcon.PRIORITY_HIGH
	Priority.MAXIMUM -> CommonIcon.PRIORITY_MAXIMUM
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