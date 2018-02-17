package com.poterion.monitor.ui

import java.awt.Image
import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.swing.ImageIcon

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class Icon(file: String) {
    INACTIVE("/icons/inactive.png"),
    OK("/icons/ok.png"),
    UNKNOWN("/icons/unknown.png"),
    INFO("/icons/info.png"),
    NOTIFICATION("/icons/notification.png"),
    WARNING("/icons/warning.png"),
    ERROR("/icons/error.png"),
    FATAL("/icons/fatal.png");

    val image: Image? = Icon::class.java.getResource(file)
            ?.let { ImageIcon(it) }
            ?.image

    val bytes: ByteArray? = Icon::class.java.getResourceAsStream(file)?.use { it.readBytes() }

    val inputStream: InputStream?
        get() = bytes?.let { ByteArrayInputStream(it) }
}