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
package com.poterion.monitor.sensors.gerritcodereview.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.auth.AuthConfig
import com.poterion.monitor.data.services.AbstractServiceConfig
import com.poterion.utils.javafx.toObservableList
import javafx.collections.ObservableList
import java.util.*

/**
 * Gerrit code review service module configuration.
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
 * @param queries Query - [Priority],[Status][com.poterion.monitor.data.Status] mapping.
 *        See [GerritCodeReviewQueryConfig]
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class GerritCodeReviewConfig(override val type: String = GerritCodeReviewConfig::class.java.simpleName,
							 override var uuid: String = UUID.randomUUID().toString(),
							 name: String = "",
							 enabled: Boolean = true,
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
							 queries: List<GerritCodeReviewQueryConfig> = emptyList()) :
		AbstractServiceConfig<GerritCodeReviewQueryConfig>(name, enabled, url, trustCertificate, auth, order, priority,
				checkInterval, connectTimeout, readTimeout, writeTimeout, tableColumnWidths) {

	@Suppress("unused")
	private var _queries: Collection<GerritCodeReviewQueryConfig>
		@JsonProperty("queries") get() = subConfig
		set(value) {
			subConfig.setAll(value)
		}

	/**
	 * Query - [Priority],[Status][com.poterion.monitor.data.Status] mapping.
	 * @see [GerritCodeReviewQueryConfig]
	 */
	override val subConfig: ObservableList<GerritCodeReviewQueryConfig> = queries.toObservableList()
		@JsonIgnore get
}