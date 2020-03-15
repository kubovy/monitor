/******************************************************************************
 * Copyright (c) 2020 Jan Kubovy <jan@kubovy.eu>                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify it    *
 * under the terms of the GNU General Public License as published by the Free *
 * Software Foundation, version 3.                                            *
 *                                                                            *
 * This program is distributed in the hope that it will be useful, but        *
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY *
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License    *
 * for more details.                                                          *
 *                                                                            *
 * You should have received a copy of the GNU General Public License along    *
 * with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ******************************************************************************/
package com.poterion.monitor.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.poterion.utils.javafx.toObservableMap
import com.poterion.utils.kotlin.setAll
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.ObservableMap

/**
 * Abstract module configuration with common implementation of [ModuleConfig].
 *
 * @param name Module name
 * @param enabled Whether module is enabled (`true`) or not (`false`)
 * @param tableColumnWidths Saved UI table column widths (column name -> width)
 * @author Jan Kubovy [jan@kubovy.eu]
 */
abstract class AbstractModuleConfig(name: String = "",
									enabled: Boolean = false,
									tableColumnWidths: Map<String, Int> = emptyMap()) : ModuleConfig {

	final override var name: String
		get() = nameProperty.get()
		set(value) = nameProperty.set(value)

	final override val nameProperty: StringProperty = SimpleStringProperty(name)
		@JsonIgnore get

	final override var enabled: Boolean
		get() = enabledProperty.get()
		set(value) = enabledProperty.set(value)

	final override val enabledProperty: BooleanProperty = SimpleBooleanProperty(enabled)
		@JsonIgnore get

	private var _tableColumnWidths: Map<String, Int>
		@JsonProperty("tableColumnWidths") get() = tableColumnWidths
		set(value) = tableColumnWidths.setAll(value)

	final override val tableColumnWidths: ObservableMap<String, Int> = tableColumnWidths.toObservableMap()
		@JsonIgnore get

	override fun toString(): String {
		var klass: Class<*>? = this::class.java
		val pairs = mutableListOf<Pair<String, String>>()
		while (klass?.superclass != null) {
			for (field in klass.declaredFields) {
				field.isAccessible = true
				pairs.add(field.name to field.get(this).toString())
			}
			klass = klass.superclass
		}
		return "${this::class.java.simpleName}[${pairs.joinToString(", ") { (k, v) -> "${k}=${v}" }}]"
	}
}