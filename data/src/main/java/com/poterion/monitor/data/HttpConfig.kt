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
import com.poterion.monitor.data.auth.AuthConfig
import javafx.beans.property.*

/**
 * HTTP configuration
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class HttpConfig(url: String = "",
				 trustCertificate: Boolean = false,
				 auth: AuthConfig? = null,
				 connectTimeout: Long? = null,
				 readTimeout: Long? = null,
				 writeTimeout: Long? = null) : HttpConfigInterface {

	override var url: String
		get() = urlProperty.get()
		set(value) = urlProperty.set(value)

	override val urlProperty: StringProperty = SimpleStringProperty(url)
		@JsonIgnore get

	override var trustCertificate: Boolean
		get() = trustCertificateProperty.get()
		set(value) = trustCertificateProperty.set(value)

	override val trustCertificateProperty: BooleanProperty = SimpleBooleanProperty(trustCertificate)
		@JsonIgnore get

	override var auth: AuthConfig?
		get() = authProperty.get()
		set(value) = authProperty.set(value)

	override val authProperty: ObjectProperty<AuthConfig?> = SimpleObjectProperty(auth)
		@JsonIgnore get

	override var connectTimeout: Long?
		get() = connectTimeoutProperty.get()
		set(value) = connectTimeoutProperty.set(value)

	override val connectTimeoutProperty: ObjectProperty<Long?> = SimpleObjectProperty(connectTimeout)
		@JsonIgnore get

	override var readTimeout: Long?
		get() = readTimeoutProperty.get()
		set(value) = readTimeoutProperty.set(value)

	override val readTimeoutProperty: ObjectProperty<Long?> = SimpleObjectProperty(readTimeout)
		@JsonIgnore get

	override var writeTimeout: Long?
		get() = writeTimeoutProperty.get()
		set(value) = writeTimeoutProperty.set(value)

	override val writeTimeoutProperty: ObjectProperty<Long?> = SimpleObjectProperty(writeTimeout)
		@JsonIgnore get
}