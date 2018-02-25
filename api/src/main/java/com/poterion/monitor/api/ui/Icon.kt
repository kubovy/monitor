package com.poterion.monitor.api.ui

import javafx.scene.image.Image
import java.io.InputStream

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface Icon {
	val inputStream: InputStream
	fun image(width: Int = 0, height: Int = 0): Image
}