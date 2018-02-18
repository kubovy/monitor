package com.poterion.monitor.data.services

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.poterion.monitor.data.notifiers.NotifierConfig
import kotlin.reflect.KClass


object ServiceDeserializer : StdDeserializer<ServiceConfig>(ServiceConfig::class.java) {
	private val registry = mutableMapOf<String, KClass<out ServiceConfig>>()

	fun register(notifierClass: KClass<out ServiceConfig>) {
		registry[notifierClass.simpleName!!] = notifierClass
	}

	override fun deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): ServiceConfig? {
		val mapper = jsonParser.codec as ObjectMapper
		val root = mapper.readTree(jsonParser) as JsonNode
		var serviceConfig: KClass<out ServiceConfig>? = null
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