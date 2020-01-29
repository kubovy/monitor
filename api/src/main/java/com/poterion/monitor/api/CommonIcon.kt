package com.poterion.monitor.api

import com.poterion.utils.javafx.Icon

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
enum class CommonIcon : Icon {
	APPLICATION,
	DEVICE,
	LINK,
	REFRESH,
	DUPLICATE,
	TRASH,
	SETTINGS,
	UNDER_CONSTRUCTION,

	ACTIVE,
	INACTIVE,
	UNAVAILABLE,
	BROKEN_LINK,

	STATUS_NONE,
	STATUS_OFF,
	STATUS_OK,
	STATUS_UNKNOWN,
	STATUS_INFO,
	STATUS_NOTIFICATION,
	STATUS_WARNING,
	STATUS_ERROR,
	STATUS_FATAL,

	PRIORITY_NONE,
	PRIORITY_LOW,
	PRIORITY_MEDIUM,
	PRIORITY_HIGH,
	PRIORITY_MAXIMUM
}