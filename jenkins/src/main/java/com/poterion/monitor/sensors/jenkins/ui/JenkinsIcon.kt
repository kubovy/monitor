package com.poterion.monitor.sensors.jenkins.ui

import com.poterion.monitor.api.ui.Icon
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class JenkinsIcon(private val file: String) : Icon {
	JENKINS("/icons/jenkins.png");

	override val inputStream: InputStream
		get() = JenkinsIcon::class.java.getResourceAsStream(file)
				.use { it.readBytes() }
				.let { ByteArrayInputStream(it) }
}