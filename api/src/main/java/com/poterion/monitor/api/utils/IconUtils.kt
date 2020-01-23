package com.poterion.monitor.api.utils

import com.poterion.monitor.api.CommonIcon
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.utils.javafx.Icon

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

