package com.poterion.monitor.api.communication

fun ByteArray.calculateChecksum() = (map { it.toInt() }.takeIf { it.isNotEmpty() }?.reduce { acc, i -> acc + i }
		?: 0) and 0xFF