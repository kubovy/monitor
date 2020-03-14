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
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.StringProperty

/**
 * HTTP configuration interface.
 *
 * @author Jan Kubovy [jan@kubovy.eu]
 */
interface HttpConfig {
	/**
	 * Service [URL][java.net.URL]
	 * @see urlProperty
	 */
	var url: String

	/**
	 * [URL][java.net.URL] property.
	 * @see url
	 */
	val urlProperty: StringProperty
		@JsonIgnore get

	/**
	 * If `true`, all certificates will be trusted, if `false` only trusted certificates will be trusted.
	 * @see trustCertificateProperty
	 */
	var trustCertificate: Boolean

	/**
	 * Trust certificate property.
	 * @see trustCertificate
	 */
	val trustCertificateProperty: BooleanProperty
		@JsonIgnore get

	/**
	 * Service [authentication][AuthConfig]
	 * @see authProperty
	 */
	var auth: AuthConfig?

	/**
	 * Service [authentication][AuthConfig] property.
	 * @see auth
	 */
	val authProperty: ObjectProperty<AuthConfig?>
		@JsonIgnore get

	/**
	 * Connection timeout.
	 * @see connectTimeoutProperty
	 */
	var connectTimeout: Long?

	/**
	 * Connection timeout property
	 * @see connectTimeout
	 */
	val connectTimeoutProperty: ObjectProperty<Long?>
		@JsonIgnore get

	/**
	 * Read timeout
	 * @see readTimeoutProperty
	 */
	var readTimeout: Long?

	/**
	 * Read timeout property
	 * @see readTimeout
	 */
	val readTimeoutProperty: ObjectProperty<Long?>
		@JsonIgnore get

	/**
	 * Write timeout.
	 * @see writeTimeoutProperty
	 */
	var writeTimeout: Long?

	/**
	 * Write timeout property.
	 * @see writeTimeout
	 */
	val writeTimeoutProperty: ObjectProperty<Long?>
		@JsonIgnore get
}