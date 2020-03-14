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

import com.fasterxml.jackson.annotation.JsonIgnore
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/**
 * @param type Class simple name for polymorphic deserialization
 * @param token Authentication token
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class TokenAuthConfig(override var type: String = TokenAuthConfig::class.java.simpleName,
					  token: String = "") : AuthConfig {

	/**
	 * Token.
	 * @see tokenProperty
	 */
	var token: String
		get() = tokenProperty.get()
		set(value) = tokenProperty.set(value)

	/**
	 * Token property
	 * @see token
	 */
	val tokenProperty: StringProperty = SimpleStringProperty(token)
		@JsonIgnore get
}
