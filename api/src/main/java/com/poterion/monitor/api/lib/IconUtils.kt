package com.poterion.monitor.api.lib

import com.poterion.monitor.api.ui.CommonIcon
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem

fun StatusItem.toIcon() : Icon = when (status) {
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