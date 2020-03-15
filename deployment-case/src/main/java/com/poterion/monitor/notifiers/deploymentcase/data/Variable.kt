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
package com.poterion.monitor.notifiers.deploymentcase.data

import com.fasterxml.jackson.annotation.JsonIgnore
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class Variable(name: String = "",
			   type: VariableType = VariableType.BOOLEAN,
			   value: String = "false") {

	var name: String
		get() = nameProperty.get()
		set(value) = nameProperty.set(value)

	val nameProperty: StringProperty = SimpleStringProperty(name)
		@JsonIgnore get

	var type: VariableType
		get() = typeProperty.get()
		set(value) = typeProperty.set(value)

	val typeProperty: ObjectProperty<VariableType> = SimpleObjectProperty(type)
		@JsonIgnore get

	var value: String
		get() = valueProperty.get()
		set(value) = valueProperty.set(value)

	val valueProperty: StringProperty = SimpleStringProperty(value)
		@JsonIgnore get

	fun copy() = Variable(name, type, value)
}