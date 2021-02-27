/******************************************************************************
 * Copyright (C) 2020 Jan Kubovy (jan@kubovy.eu)                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.      *
 ******************************************************************************/
package com.poterion.monitor.api

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.ModuleDeserializer
import com.poterion.monitor.data.auth.AuthConfig
import com.poterion.monitor.data.auth.AuthDeserializer
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.notifiers.NotifierConfig
import com.poterion.monitor.data.notifiers.NotifierDeserializer
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.monitor.data.services.ServiceDeserializer
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime
import kotlin.math.pow

private val LOGGER = LoggerFactory.getLogger("com.poterion.monitor.api.Utils")

val objectMapper = ObjectMapper(YAMLFactory()).apply {
	registerModule(ParameterNamesModule())
	registerModule(Jdk8Module())
	registerModule(JavaTimeModule())
	registerModule(SimpleModule("PolymorphicServiceDeserializerModule", Version.unknownVersion()).apply {
		addDeserializer(AuthConfig::class.java, AuthDeserializer)
	})

	registerModule(SimpleModule("PolymorphicServiceDeserializerModule", Version.unknownVersion()).apply {
		addDeserializer(ServiceConfig::class.java, ServiceDeserializer)
	})
	registerModule(SimpleModule("PolymorphicNotifierDeserializerModule", Version.unknownVersion()).apply {
		addDeserializer(NotifierConfig::class.java, NotifierDeserializer)
	})
	registerModule(SimpleModule("PolymorphicNotifierDeserializerModule", Version.unknownVersion()).apply {
		addDeserializer(ModuleConfig::class.java, ModuleDeserializer)
	})
	configure(SerializationFeature.CLOSE_CLOSEABLE, true)
	configure(SerializationFeature.FLUSH_AFTER_WRITE_VALUE, true)
	configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
	configure(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS, true)
	configure(DeserializationFeature.WRAP_EXCEPTIONS, true)
	configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
	configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
	configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false)
	configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
	configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
	configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

fun String.toVersionNumber(): Long {
	if (contains("SNAPSHOT")) return Long.MAX_VALUE
	return split(".")
			.map { it.toDoubleOrNull() ?: 0.0 }
			.reversed()
			.takeIf { it.isNotEmpty() }
			?.reduceIndexed { index, acc, i -> acc + (i * 10_000.0.pow(index)) }
			?.toLong()
			?: 0L
}

fun ApplicationConfiguration.save() {
	val backupFile = File(Shared.configFile.absolutePath + "-" + LocalDateTime.now().toString())
	try {
		val tempFile = File(Shared.configFile.absolutePath + ".tmp")
		var success = tempFile.parentFile.exists() || tempFile.parentFile.mkdirs()
		objectMapper.writeValue(tempFile, this)
		success = success
				&& (!backupFile.exists() || backupFile.delete())
				&& (Shared.configFile.parentFile.exists() || Shared.configFile.parentFile.mkdirs())
				&& (!Shared.configFile.exists() || Shared.configFile.renameTo(backupFile))
				&& tempFile.renameTo(Shared.configFile.absoluteFile)
		if (success) backupFile.delete()
		else LOGGER.error("Failed saving configuration to ${Shared.configFile.absolutePath} (backup ${backupFile})")
	} catch (e: Exception) {
		LOGGER.error(e.message, e)
	} finally {
		if (!Shared.configFile.exists() && backupFile.exists() && !backupFile.renameTo(Shared.configFile)) {
			LOGGER.error("Restoring ${backupFile} failed!")
		}
	}
}