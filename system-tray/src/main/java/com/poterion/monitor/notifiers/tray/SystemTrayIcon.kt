package com.poterion.monitor.notifiers.tray

import com.poterion.monitor.api.ui.Icon
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class SystemTrayIcon : Icon {
	TRAY,
	ABOUT,
	REFRESH,
	QUIT
}