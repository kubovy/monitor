package com.poterion.monitor.ui

import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem

fun StatusItem.toIcon() = when (status) {
	Status.OK -> Icon.OK
	Status.INFO -> Icon.INFO
	Status.NOTIFICATION -> Icon.NOTIFICATION
	Status.WARNING -> Icon.WARNING
	Status.ERROR -> Icon.ERROR
	Status.FATAL -> Icon.FATAL

	Status.NONE,
	Status.CONNECTION_ERROR,
	Status.SERVICE_ERROR,
	Status.UNKNOWN -> Icon.UNKNOWN
}