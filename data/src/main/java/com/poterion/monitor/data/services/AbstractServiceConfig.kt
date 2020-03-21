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
package com.poterion.monitor.data.services

import com.fasterxml.jackson.annotation.JsonIgnore
import com.poterion.monitor.data.AbstractModuleConfig
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.auth.AuthConfig
import javafx.beans.property.BooleanProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/**
 * Abstract service configuration with common implementation of [ServiceConfig].
 *
 * @param name Module name
 * @param enabled Whether module is enabled (`true`) or not (`false`)
 * @param url Service [URL][java.net.URL] (see [HttpConfig][com.poterion.monitor.data.HttpConfig])
 * @param trustCertificate Whether to trust all certificates (see [HttpConfig][com.poterion.monitor.data.HttpConfig])
 * @param auth Service [authentication][AuthConfig] (see [HttpConfig][com.poterion.monitor.data.HttpConfig])
 * @param order Order of the service in which it will be evaluated
 * @param priority Priority of the service used for [items][com.poterion.monitor.data.StatusItem] yield by it unless
 *        otherwise additionally configured.
 * @param checkInterval Interval in which the service will be periodically checked for new
 *        [items][com.poterion.monitor.data.StatusItem].
 * @param connectTimeout Connection timeout (see [HttpConfig][com.poterion.monitor.data.HttpConfig])
 * @param readTimeout Read timeout (see [HttpConfig][com.poterion.monitor.data.HttpConfig])
 * @param writeTimeout Write timeout (see [HttpConfig][com.poterion.monitor.data.HttpConfig])
 * @param tableColumnWidths Saved UI table column widths (column name -> width)
 * @author Jan Kubovy [jan@kubovy.eu]
 */
abstract class AbstractServiceConfig<SC>(name: String = "",
										 enabled: Boolean = false,
										 url: String = "",
										 trustCertificate: Boolean = false,
										 auth: AuthConfig? = null,
										 order: Int = Int.MAX_VALUE,
										 priority: Priority = Priority.NONE,
										 checkInterval: Long? = null,
										 connectTimeout: Long? = null,
										 readTimeout: Long? = null,
										 writeTimeout: Long? = null,
										 tableColumnWidths: Map<String, Int> = emptyMap()) :
		AbstractModuleConfig(name, enabled, tableColumnWidths), ServiceConfig<SC> {

	final override var order: Int
		get() = orderProperty.get()
		set(value) = orderProperty.set(value)

	final override val orderProperty: IntegerProperty = SimpleIntegerProperty(order)
		@JsonIgnore get

	final override var priority: Priority
		get() = priorityProperty.get()
		set(value) = priorityProperty.set(value)

	final override val priorityProperty: ObjectProperty<Priority> = SimpleObjectProperty(priority)
		@JsonIgnore get

	final override var checkInterval: Long?
		get() = checkIntervalProperty.get()
		set(value) = checkIntervalProperty.set(value)

	final override val checkIntervalProperty: ObjectProperty<Long?> = SimpleObjectProperty(checkInterval)
		@JsonIgnore get

	final override var url: String
		get() = urlProperty.get()
		set(value) = urlProperty.set(value)

	final override val urlProperty: StringProperty = SimpleStringProperty(url)
		@JsonIgnore get

	final override var trustCertificate: Boolean
		get() = trustCertificateProperty.get()
		set(value) = trustCertificateProperty.set(value)

	final override val trustCertificateProperty: BooleanProperty = SimpleBooleanProperty(trustCertificate)
		@JsonIgnore get

	final override var auth: AuthConfig?
		get() = authProperty.get()
		set(value) = authProperty.set(value)

	final override val authProperty: ObjectProperty<AuthConfig?> = SimpleObjectProperty(auth)
		@JsonIgnore get

	final override var connectTimeout: Long?
		get() = connectTimeoutProperty.get()
		set(value) = connectTimeoutProperty.set(value)

	final override val connectTimeoutProperty: ObjectProperty<Long?> = SimpleObjectProperty(connectTimeout)
		@JsonIgnore get

	final override var readTimeout: Long?
		get() = readTimeoutProperty.get()
		set(value) = readTimeoutProperty.set(value)

	final override val readTimeoutProperty: ObjectProperty<Long?> = SimpleObjectProperty(readTimeout)
		@JsonIgnore get

	final override var writeTimeout: Long?
		get() = writeTimeoutProperty.get()
		set(value) = writeTimeoutProperty.set(value)

	final override val writeTimeoutProperty: ObjectProperty<Long?> = SimpleObjectProperty(writeTimeout)
		@JsonIgnore get
}