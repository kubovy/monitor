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
package com.poterion.monitor.notifiers.devopslight.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.poterion.utils.javafx.toObservableList
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.ObservableList

/**
 * DevOps light configuration item
 *
 * @param id Configuation item ID
 * @param statusNone List of [LightConfig] for [Status.NONE][com.poterion.monitor.data.Status.NONE]
 * @param statusUnknown List of [LightConfig] for [Status.UNKNOWN][com.poterion.monitor.data.Status.UNKNOWN]
 * @param statusOk List of [LightConfig] for [Status.OK][com.poterion.monitor.data.Status.OK]
 * @param statusInfo List of [LightConfig] for [Status.INFO][com.poterion.monitor.data.Status.INFO]
 * @param statusNotification List of [LightConfig] for
 *        [Status.NOTIFICATION][com.poterion.monitor.data.Status.NOTIFICATION]
 * @param statusConnectionError List of [LightConfig] for
 *        [Status.CONNECTION_ERROR][com.poterion.monitor.data.Status.CONNECTION_ERROR]
 * @param statusServiceError List of [LightConfig] for
 *        [Status.SERVICE_ERROR][com.poterion.monitor.data.Status.SERVICE_ERROR]
 * @param statusWarning List of [LightConfig] for [Status.WARNING][com.poterion.monitor.data.Status.WARNING]
 * @param statusError List of [LightConfig] for [Status.ERROR][com.poterion.monitor.data.Status.ERROR]
 * @param statusFatal List of [LightConfig] for [Status.FATAL][com.poterion.monitor.data.Status.FATAL]
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class DevOpsLightItemConfig(id: String = "",
							statusNone: List<LightConfig> = emptyList(),
							statusUnknown: List<LightConfig> = emptyList(),
							statusOk: List<LightConfig> = emptyList(),
							statusInfo: List<LightConfig> = emptyList(),
							statusNotification: List<LightConfig> = emptyList(),
							statusConnectionError: List<LightConfig> = emptyList(),
							statusServiceError: List<LightConfig> = emptyList(),
							statusWarning: List<LightConfig> = emptyList(),
							statusError: List<LightConfig> = emptyList(),
							statusFatal: List<LightConfig> = emptyList()) {

	var id: String
		get() = idProperty.get()
		set(value) = idProperty.set(value)

	val idProperty: StringProperty = SimpleStringProperty(id)
		@JsonIgnore get

	@Suppress("unused")
	private var _statusNone: List<LightConfig>
		@JsonProperty("statusNone") get() = statusNone
		set(value) {
			statusNone.setAll(value)
		}

	val statusNone: ObservableList<LightConfig> = statusNone.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _statusUnknown: List<LightConfig>
		@JsonProperty("statusUnknown") get() = statusUnknown
		set(value) {
			statusUnknown.setAll(value)
		}

	val statusUnknown: ObservableList<LightConfig> = statusUnknown.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _statusOk: List<LightConfig>
		@JsonProperty("statusOk") get() = statusOk
		set(value) {
			statusOk.setAll(value)
		}

	val statusOk: ObservableList<LightConfig> = statusOk.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _statusInfo: List<LightConfig>
		@JsonProperty("statusInfo") get() = statusInfo
		set(value) {
			statusInfo.setAll(value)
		}

	val statusInfo: ObservableList<LightConfig> = statusInfo.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _statusNotification: List<LightConfig>
		@JsonProperty("statusNotification") get() = statusNotification
		set(value) {
			statusNotification.setAll(value)
		}

	val statusNotification: ObservableList<LightConfig> = statusNotification.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _statusConnectionError: List<LightConfig>
		@JsonProperty("statusConnectionError") get() = statusConnectionError
		set(value) {
			statusConnectionError.setAll(value)
		}

	val statusConnectionError: ObservableList<LightConfig> = statusConnectionError.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _statusServiceError: List<LightConfig>
		@JsonProperty("statusServiceError") get() = statusServiceError
		set(value) {
			statusServiceError.setAll(value)
		}

	val statusServiceError: ObservableList<LightConfig> = statusServiceError.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _statusWarning: List<LightConfig>
		@JsonProperty("statusWarning") get() = statusWarning
		set(value) {
			statusWarning.setAll(value)
		}

	val statusWarning: ObservableList<LightConfig> = statusWarning.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _statusError: List<LightConfig>
		@JsonProperty("statusError") get() = statusError
		set(value) {
			statusError.setAll(value)
		}

	val statusError: ObservableList<LightConfig> = statusError.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _statusFatal: List<LightConfig>
		@JsonProperty("statusFatal") get() = statusFatal
		set(value) {
			statusFatal.setAll(value)
		}

	val statusFatal: ObservableList<LightConfig> = statusFatal.toObservableList()
		@JsonIgnore get
}