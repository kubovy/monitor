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
package com.poterion.monitor.sensors.feed.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.auth.AuthConfig
import com.poterion.monitor.data.services.AbstractServiceConfig
import com.poterion.utils.javafx.toObservableList
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import java.util.*

/**
 * Syndication feed service module configuration.
 *
 * @param name Module name
 * @param enabled Whether module is enabled (`true`) or not (`false`)
 * @param url Service [URL][java.net.URL] (see [HttpConfig][com.poterion.monitor.data.HttpConfigInterface])
 * @param trustCertificate Whether to trust all certificates (see
 *        [HttpConfig][com.poterion.monitor.data.HttpConfigInterface])
 * @param auth Service [authentication][AuthConfig] (see [HttpConfig][com.poterion.monitor.data.HttpConfigInterface])
 * @param order Order of the service in which it will be evaluated
 * @param priority Priority of the service used for [items][com.poterion.monitor.data.StatusItem] yield by it unless
 *        otherwise additionally configured.
 * @param checkInterval Interval in which the service will be periodically checked for new
 *        [items][com.poterion.monitor.data.StatusItem].
 * @param connectTimeout Connection timeout (see [HttpConfig][com.poterion.monitor.data.HttpConfigInterface])
 * @param readTimeout Read timeout (see [HttpConfig][com.poterion.monitor.data.HttpConfigInterface])
 * @param writeTimeout Write timeout (see [HttpConfig][com.poterion.monitor.data.HttpConfigInterface])
 * @param tableColumnWidths Saved UI table column widths (column name -> width)
 * @param status Default [Status] if no `filter` applies
 * @param filters Title/summary - [Priority]/[Status] mapping (see [SyndicationFeedFilterConfig])
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class SyndicationFeedConfig(override var type: String = SyndicationFeedConfig::class.java.simpleName,
							override var uuid: String = UUID.randomUUID().toString(),
							name: String = "",
							enabled: Boolean = true,
							url: String = "",
							trustCertificate: Boolean = false,
							auth: AuthConfig? = null,
							priority: Priority = Priority.NONE,
							checkInterval: Long? = null,
							connectTimeout: Long? = null,
							readTimeout: Long? = null,
							writeTimeout: Long? = null,
							tableColumnWidths: Map<String, Int> = emptyMap(),
							status: Status = Status.NONE,
							filters: List<SyndicationFeedFilterConfig> = emptyList()) :
		AbstractServiceConfig<SyndicationFeedFilterConfig>(name, enabled, url, trustCertificate, auth, priority,
				checkInterval, connectTimeout, readTimeout, writeTimeout, tableColumnWidths) {

	/**
	 * Default [Status] if no `filter` applies.
	 * @see statusProperty
	 */
	var status: Status
		get() = statusProperty.get()
		set(value) = statusProperty.set(value)

	/**
	 * Default [Status] property.
	 * @see status
	 */
	val statusProperty: ObjectProperty<Status> = SimpleObjectProperty(status)
		@JsonIgnore get

	@Suppress("unused")
	private var _filters: List<SyndicationFeedFilterConfig>
		@JsonProperty("filters") get() = subConfig
		set(value) {
			subConfig.setAll(value)
		}

	/**
	 * Title/description - [Priority]/[Status] mapping
	 * @see SyndicationFeedFilterConfig
	 */
	override val subConfig: ObservableList<SyndicationFeedFilterConfig> = filters.toObservableList()
		@JsonIgnore get
}