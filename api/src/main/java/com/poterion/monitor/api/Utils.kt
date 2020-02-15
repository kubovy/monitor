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
import com.poterion.monitor.data.notifiers.NotifierConfig
import com.poterion.monitor.data.notifiers.NotifierDeserializer
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.monitor.data.services.ServiceDeserializer

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