package com.poterion.monitor.api.utils

import java.net.URI
import java.net.URISyntaxException

fun String.toSet(separator: String) = split(separator)
		.map { it.trim() }
		.filterNot {  it.isBlank() }
		.toSet()

fun String.toUri(): URI = URI(this)

fun String.toUriOrNull(): URI? = try {
	toUri()
} catch (e: URISyntaxException) {
	null
}