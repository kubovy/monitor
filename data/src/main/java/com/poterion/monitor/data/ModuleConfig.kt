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
package com.poterion.monitor.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import javafx.beans.property.BooleanProperty
import javafx.beans.property.StringProperty
import javafx.collections.ObservableMap

/**
 * Module config interface.
 *
 * @author Jan Kubovy [jan@kubovy.eu]
 */
@JsonInclude(value = JsonInclude.Include.NON_NULL, content = JsonInclude.Include.NON_NULL)
interface ModuleConfig {
	/** Module type constant for polymorphic deserialization. */
	val type: String
		@JsonInclude(value = JsonInclude.Include.ALWAYS)
		get

	/** Module UUID for unique identification. */
	var uuid: String
		@JsonInclude(value = JsonInclude.Include.ALWAYS)
		get

	/**
	 * Module name
	 * @see nameProperty
	 */
	var name: String
		@JsonInclude(value = JsonInclude.Include.ALWAYS) get

	/**
	 * Module name property
	 * @see name
	 */
	val nameProperty: StringProperty
		@JsonIgnore get

	/**
	 * Whether module is enabled (`true`) or not (`false`)
	 * @see enabledProperty
	 */
	var enabled: Boolean

	/**
	 * Enabled property
	 * @see enabled
	 */
	val enabledProperty: BooleanProperty
		@JsonIgnore get

	/**
	 * Saved UI table column widths.
	 *
	 * A map containing name of the column as a key and the saved width as value.
	 */
	val tableColumnWidths: ObservableMap<String, Int>
		@JsonInclude(value = JsonInclude.Include.ALWAYS) get
}