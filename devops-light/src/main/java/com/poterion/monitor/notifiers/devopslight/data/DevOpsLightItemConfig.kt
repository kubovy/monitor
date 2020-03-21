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
import com.poterion.communication.serial.payload.RgbLightConfiguration
import com.poterion.utils.javafx.toObservableList
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.ObservableList

/**
 * DevOps light configuration item
 *
 * @param id Configuration item ID
 * @param statusNone List of [RgbLightConfiguration] for [Status.NONE][com.poterion.monitor.data.Status.NONE]
 * @param statusUnknown List of [RgbLightConfiguration] for [Status.UNKNOWN][com.poterion.monitor.data.Status.UNKNOWN]
 * @param statusOk List of [RgbLightConfiguration] for [Status.OK][com.poterion.monitor.data.Status.OK]
 * @param statusInfo List of [RgbLightConfiguration] for [Status.INFO][com.poterion.monitor.data.Status.INFO]
 * @param statusNotification List of [RgbLightConfiguration] for
 *        [Status.NOTIFICATION][com.poterion.monitor.data.Status.NOTIFICATION]
 * @param statusConnectionError List of [RgbLightConfiguration] for
 *        [Status.CONNECTION_ERROR][com.poterion.monitor.data.Status.CONNECTION_ERROR]
 * @param statusServiceError List of [RgbLightConfiguration] for
 *        [Status.SERVICE_ERROR][com.poterion.monitor.data.Status.SERVICE_ERROR]
 * @param statusWarning List of [RgbLightConfiguration] for [Status.WARNING][com.poterion.monitor.data.Status.WARNING]
 * @param statusError List of [RgbLightConfiguration] for [Status.ERROR][com.poterion.monitor.data.Status.ERROR]
 * @param statusFatal List of [RgbLightConfiguration] for [Status.FATAL][com.poterion.monitor.data.Status.FATAL]
 * @author Jan Kubovy [jan@kubovy.eu]
 */
@Suppress("MemberVisibilityCanBePrivate")
class DevOpsLightItemConfig(id: String? = null,
							subId: String? = null,
							statusNone: List<RgbLightConfiguration> = emptyList(),
							statusUnknown: List<RgbLightConfiguration> = emptyList(),
							statusOk: List<RgbLightConfiguration> = emptyList(),
							statusInfo: List<RgbLightConfiguration> = emptyList(),
							statusNotification: List<RgbLightConfiguration> = emptyList(),
							statusConnectionError: List<RgbLightConfiguration> = emptyList(),
							statusServiceError: List<RgbLightConfiguration> = emptyList(),
							statusWarning: List<RgbLightConfiguration> = emptyList(),
							statusError: List<RgbLightConfiguration> = emptyList(),
							statusFatal: List<RgbLightConfiguration> = emptyList()) {

	var id: String?
		get() = idProperty.get()
		set(value) = idProperty.set(value)

	val idProperty: StringProperty = SimpleStringProperty(id)
		@JsonIgnore get

	var subId: String?
		get() = subIdProperty.get()
		set(value) = subIdProperty.set(value)

	val subIdProperty: StringProperty = SimpleStringProperty(subId)
		@JsonIgnore get

	@Suppress("unused")
	private var _statusNone: List<RgbLightConfiguration>
		@JsonProperty("statusNone") get() = statusNone
		set(value) {
			statusNone.setAll(value)
		}

	val statusNone: ObservableList<RgbLightConfiguration> = statusNone.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _statusUnknown: List<RgbLightConfiguration>
		@JsonProperty("statusUnknown") get() = statusUnknown
		set(value) {
			statusUnknown.setAll(value)
		}

	val statusUnknown: ObservableList<RgbLightConfiguration> = statusUnknown.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _statusOk: List<RgbLightConfiguration>
		@JsonProperty("statusOk") get() = statusOk
		set(value) {
			statusOk.setAll(value)
		}

	val statusOk: ObservableList<RgbLightConfiguration> = statusOk.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _statusInfo: List<RgbLightConfiguration>
		@JsonProperty("statusInfo") get() = statusInfo
		set(value) {
			statusInfo.setAll(value)
		}

	val statusInfo: ObservableList<RgbLightConfiguration> = statusInfo.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _statusNotification: List<RgbLightConfiguration>
		@JsonProperty("statusNotification") get() = statusNotification
		set(value) {
			statusNotification.setAll(value)
		}

	val statusNotification: ObservableList<RgbLightConfiguration> = statusNotification.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _statusConnectionError: List<RgbLightConfiguration>
		@JsonProperty("statusConnectionError") get() = statusConnectionError
		set(value) {
			statusConnectionError.setAll(value)
		}

	val statusConnectionError: ObservableList<RgbLightConfiguration> = statusConnectionError.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _statusServiceError: List<RgbLightConfiguration>
		@JsonProperty("statusServiceError") get() = statusServiceError
		set(value) {
			statusServiceError.setAll(value)
		}

	val statusServiceError: ObservableList<RgbLightConfiguration> = statusServiceError.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _statusWarning: List<RgbLightConfiguration>
		@JsonProperty("statusWarning") get() = statusWarning
		set(value) {
			statusWarning.setAll(value)
		}

	val statusWarning: ObservableList<RgbLightConfiguration> = statusWarning.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _statusError: List<RgbLightConfiguration>
		@JsonProperty("statusError") get() = statusError
		set(value) {
			statusError.setAll(value)
		}

	val statusError: ObservableList<RgbLightConfiguration> = statusError.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _statusFatal: List<RgbLightConfiguration>
		@JsonProperty("statusFatal") get() = statusFatal
		set(value) {
			statusFatal.setAll(value)
		}

	val statusFatal: ObservableList<RgbLightConfiguration> = statusFatal.toObservableList()
		@JsonIgnore get
}