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
package com.poterion.monitor.data.auth

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
object AuthDeserializer : StdDeserializer<AuthConfig>(AuthConfig::class.java) {
	private val registry = mutableMapOf<String, KClass<out AuthConfig>>()

	init {
		register(BasicAuthConfig::class)
		register(TokenAuthConfig::class)
	}

	private fun register(authClass: KClass<out AuthConfig>) {
		registry[authClass.simpleName!!] = authClass
	}

	override fun deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): AuthConfig? {
		val mapper = jsonParser.codec as ObjectMapper
		val root = mapper.readTree(jsonParser) as JsonNode
		var serviceConfig: KClass<out AuthConfig>? = null
		val elementsIterator = root.fields()
		while (elementsIterator.hasNext()) {
			val element = elementsIterator.next()
			val (name, value) = element
			if (name == "type" && value.isTextual && registry.containsKey(value.textValue())) {
				serviceConfig = registry[value.textValue()]
				break
			}
		}
		return serviceConfig?.let { mapper.readValue(root.toString(), serviceConfig.java) }
	}
}