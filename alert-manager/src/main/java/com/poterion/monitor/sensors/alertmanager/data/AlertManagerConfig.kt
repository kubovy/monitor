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
package com.poterion.monitor.sensors.alertmanager.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.auth.AuthConfig
import com.poterion.monitor.data.services.AbstractServiceConfig
import com.poterion.utils.javafx.toObservableList
import com.poterion.utils.javafx.toObservableSet
import com.poterion.utils.kotlin.setAll
import javafx.collections.ObservableList
import javafx.collections.ObservableSet
import java.util.*

/**
 * Alert manager service module configuration.
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
 * @param nameRefs Set of annotations or labels to be used as [status item's][com.poterion.monitor.data.StatusItem]
 *        name (first one found will be used).
 * @param descriptionRefs Set of annotations or labels to be used as
 *        [status item's][com.poterion.monitor.data.StatusItem] description (first one found will be used).
 * @param receivers Set of receivers to be considered. Empty set means all receivers will be considered.
 * @param labelFilter Set of labels or annotations to be used as [status item's][com.poterion.monitor.data.StatusItem]
 *        tags. The `!` prefix can be used for negation.
 * @param labels Label/Annotation/Value to [Priority]/[Status][com.poterion.monitor.data.Status] mapping
 *        (see [AlertManagerLabelConfig])
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class AlertManagerConfig(override val type: String = AlertManagerConfig::class.java.simpleName,
						 override var uuid: String = UUID.randomUUID().toString(),
						 name: String = "",
						 enabled: Boolean = false,
						 url: String = "",
						 trustCertificate: Boolean = false,
						 auth: AuthConfig? = null,
						 priority: Priority = Priority.NONE,
						 checkInterval: Long? = null,
						 connectTimeout: Long? = null,
						 readTimeout: Long? = null,
						 writeTimeout: Long? = null,
						 tableColumnWidths: Map<String, Int> = emptyMap(),
						 nameRefs: Set<String> = emptySet(),
						 descriptionRefs: Set<String> = emptySet(),
						 receivers: Set<String> = emptySet(),
						 labelFilter: Set<String> = emptySet(),
						 labels: List<AlertManagerLabelConfig> = emptyList()) :
		AbstractServiceConfig<AlertManagerLabelConfig>(name, enabled, url, trustCertificate, auth, priority,
				checkInterval, connectTimeout, readTimeout, writeTimeout, tableColumnWidths) {

	@Suppress("unused")
	private var _nameRefs: Set<String>
		@JsonProperty("nameRefs") get() = nameRefs
		set(value) {
			nameRefs.setAll(value)
		}

	/**
	 * Set of annotations or labels to be used as [status item's][com.poterion.monitor.data.StatusItem] name
	 * (first one found will be used).
	 */
	val nameRefs: ObservableSet<String> = nameRefs.toObservableSet()
		@JsonIgnore get

	@Suppress("unused")
	private var _descriptionRefs: Set<String>
		@JsonProperty("descriptionRefs") get() = descriptionRefs
		set(value) {
			descriptionRefs.setAll(value)
		}

	/**
	 * Set of annotations or labels to be used as [status item's][com.poterion.monitor.data.StatusItem] description
	 * (first one found will be used).
	 */
	val descriptionRefs: ObservableSet<String> = descriptionRefs.toObservableSet()
		@JsonIgnore get

	@Suppress("unused")
	private var _receivers: Set<String>
		@JsonProperty("receivers") get() = receivers
		set(value) {
			receivers.setAll(value)
		}

	/** Set of receivers to be considered. Empty set means all receivers will be considered. */
	val receivers: ObservableSet<String> = receivers.toObservableSet()
		@JsonIgnore get

	@Suppress("unused")
	private var _labelFilter: Set<String>
		@JsonProperty("labelFilter") get() = labelFilter
		set(value) {
			labelFilter.setAll(value)
		}

	/**
	 * Set of labels or annotations to be used as [status item's][com.poterion.monitor.data.StatusItem] tags.
	 * The `!` prefix can be used for negation.
	 */
	val labelFilter: ObservableSet<String> = labelFilter.toObservableSet()
		@JsonIgnore get

	@Suppress("unused")
	private var _labels: List<AlertManagerLabelConfig>
		@JsonProperty("labels") get() = subConfig
		set(value) {
			subConfig.setAll(value)
		}

	/**
	 * Label/Annotation/Value to [Priority]/[Status][com.poterion.monitor.data.Status] mapping.
	 * @see AlertManagerLabelConfig
	 */
	override val subConfig: ObservableList<AlertManagerLabelConfig> = labels.toObservableList()
		@JsonIgnore get

}