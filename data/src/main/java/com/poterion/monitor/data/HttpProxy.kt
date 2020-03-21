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
import com.poterion.monitor.data.auth.AuthConfig
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class HttpProxy(address: String? = null,
				port: Int? = null,
				noProxy: String? = null,
				auth: AuthConfig? = null) {

	var address: String?
		get() = addressProperty.get()
		set(value) = addressProperty.set(value)

	val addressProperty: StringProperty = SimpleStringProperty(address)
		@JsonIgnore get

	var port: Int?
		get() = portProperty.get()
		set(value) = portProperty.set(value)

	val portProperty: ObjectProperty<Int?> = SimpleObjectProperty(port)
		@JsonIgnore get

	var noProxy: String?
		get() = noProxyProperty.get()
		set(value) = noProxyProperty.set(value)

	val noProxyProperty: StringProperty = SimpleStringProperty(noProxy)
		@JsonIgnore get

	var auth: AuthConfig?
		get() = authProperty.get()
		set(value) = authProperty.set(value)

	val authProperty: ObjectProperty<AuthConfig?> = SimpleObjectProperty(auth)
		@JsonIgnore get

}