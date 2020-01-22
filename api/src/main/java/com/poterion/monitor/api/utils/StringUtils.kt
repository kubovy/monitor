package com.poterion.monitor.api.utils

import java.net.URI
import java.net.URISyntaxException

fun String.cut(maxLength: Int, ellipsis: String = "..."): String {
	var value = this
	while (value.length > maxLength) {
		value = value.substringBeforeLast(" ", value.substring(0, value.length - 1))
	}
	if (value != this) value += ellipsis
	return value
}

fun String.toSet(separator: String) = split(separator)
		.map { it.trim() }
		.filterNot { it.isBlank() }
		.toSet()

fun String.toUri(): URI = URI(this)

fun String.toUriOrNull(): URI? = try {
	toUri()
} catch (e: URISyntaxException) {
	null
}