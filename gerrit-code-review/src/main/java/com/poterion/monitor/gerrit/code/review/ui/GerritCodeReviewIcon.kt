package com.poterion.monitor.gerrit.code.review.ui

import com.poterion.monitor.api.ui.Icon
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class GerritCodeReviewIcon(private val file: String) : Icon {
	GERRIT("/icons/gerrit.png");

	override val inputStream: InputStream
		get() = GerritCodeReviewIcon::class.java.getResourceAsStream(file)
				.use { it.readBytes() }
				.let { ByteArrayInputStream(it) }
}