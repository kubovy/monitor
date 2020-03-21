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
package com.poterion.monitor.sensors.jira.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.auth.AuthConfig
import com.poterion.monitor.data.services.AbstractServiceConfig
import com.poterion.utils.javafx.toObservableList
import com.poterion.utils.javafx.toObservableMap
import com.poterion.utils.kotlin.setAll
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import java.util.*

/**
 * JIRA service module configuration.
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
 * @param priorityMapping [Priority] mapping
 * @param statusMapping [Status] mapping
 * @param queries Set of JQL queries (see [JiraQueryConfig])
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class JiraConfig(override var type: String = JiraConfig::class.java.simpleName,
				 override var uuid: String = UUID.randomUUID().toString(),
				 name: String = "",
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
				 tableColumnWidths: Map<String, Int> = emptyMap(),
				 priorityMapping: Map<String, Priority> = mapOf(
						 "Minor" to Priority.NONE,
						 "Trivial" to Priority.NONE,
						 "Lowest" to Priority.LOW,
						 "Low" to Priority.LOW,
						 "Medium" to Priority.MEDIUM,
						 "High" to Priority.HIGH,
						 "Highest" to Priority.MAXIMUM,
						 "Blocker" to Priority.MAXIMUM),
				 statusMapping: Map<String, Status> = mapOf(
						 "Public Incident" to Status.FATAL,
						 "Incident" to Status.FATAL,
						 "Problem" to Status.ERROR,
						 "Bug" to Status.ERROR,
						 "Open" to Status.WARNING,
						 "Pending" to Status.WARNING,
						 "Waiting for Customer" to Status.NOTIFICATION,
						 "In Progress" to Status.INFO,
						 "Complete" to Status.OK,
						 "Canceled" to Status.OK,
						 "Closed" to Status.NONE),
				 queries: List<JiraQueryConfig> = emptyList()) :
		AbstractServiceConfig<JiraQueryConfig>(name, enabled, url, trustCertificate, auth, order, priority,
				checkInterval, connectTimeout, readTimeout, writeTimeout, tableColumnWidths) {

	@Suppress("unused")
	private var _priorityMapping: Map<String, Priority>
		@JsonProperty("priorityMapping") get() = priorityMapping
		set(value) = priorityMapping.setAll(value)

	/** [Priority] mapping. */
	val priorityMapping: ObservableMap<String, Priority> = priorityMapping.toObservableMap()
		@JsonIgnore get

	@Suppress("unused")
	private var _statusMapping: Map<String, Status>
		@JsonProperty("statusMapping") get() = statusMapping
		set(value) = statusMapping.setAll(value)

	/** [Status] mapping. */
	val statusMapping: ObservableMap<String, Status> = statusMapping.toObservableMap()
		@JsonIgnore get

	@Suppress("unused")
	private var _queries: List<JiraQueryConfig>
		@JsonProperty("queries") get() = subConfig
		set(value) {
			subConfig.setAll(value)
		}

	/**
	 * Set of JQL queries.
	 * @see JiraQueryConfig
	 */
	override val subConfig: ObservableList<JiraQueryConfig> = queries.toObservableList()
		@JsonIgnore get
}