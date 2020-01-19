package com.poterion.monitor.data.auth

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy <jan@kubovy.eu>
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