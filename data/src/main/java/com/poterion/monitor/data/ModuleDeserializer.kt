package com.poterion.monitor.data

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import kotlin.reflect.KClass


object ModuleDeserializer : StdDeserializer<ModuleConfig>(ModuleConfig::class.java) {
	private val registry = mutableMapOf<String, KClass<out ModuleConfig>>()

	fun register(notifierClass: KClass<out ModuleConfig>) {
		registry[notifierClass.simpleName!!] = notifierClass
	}

	override fun deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): ModuleConfig? {
		val mapper = jsonParser.codec as ObjectMapper
		val root = mapper.readTree(jsonParser) as JsonNode
		var moduleConfig: KClass<out ModuleConfig>? = null
		val elementsIterator = root.fields()
		while (elementsIterator.hasNext()) {
			val element = elementsIterator.next()
			val (name, value) = element
			if (name == "type" && value.isTextual && registry.containsKey(value.textValue())) {
				moduleConfig = registry[value.textValue()]
				break
			}
		}
		return moduleConfig?.let { mapper.readValue(root.toString(), moduleConfig.java) }
	}
}