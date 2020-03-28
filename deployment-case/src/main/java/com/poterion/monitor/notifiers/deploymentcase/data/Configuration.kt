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
package com.poterion.monitor.notifiers.deploymentcase.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.poterion.monitor.data.HttpConfigInterface
import com.poterion.monitor.data.auth.AuthConfig
import com.poterion.utils.javafx.toObservableList
import com.poterion.utils.javafx.toObservableMap
import com.poterion.utils.kotlin.setAll
import javafx.beans.property.*
import javafx.collections.ObservableList
import javafx.collections.ObservableMap

/**
 * Deployment case configuration item.
 *
 * @param name Configuration item name
 * @param isActive Whether the item is active or not (only one item may be active)
 * @param method HTTP method
 * @param url URL
 * @param trustCertificate Trust all certificates
 * @param auth HTTP Authentication
 * @param connectTimeout Connection timeout
 * @param readTimeout Read timeout
 * @param writeTimeout Write timeout
 * @param jobName
 * @param parameters
 * @param variables List of state machine variables
 * @param devices List of state machine devices
 * @param jobStatus
 * @param pipelineStatus
 * @param stateMachine State machine
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class Configuration(name: String = "",
					isActive: Boolean = false,
					method: String = "GET",
					url: String = "",
					trustCertificate: Boolean = false,
					auth: AuthConfig? = null,
					connectTimeout: Long? = null,
					readTimeout: Long? = null,
					writeTimeout: Long? = null,
					jobName: String = "",
					parameters: String = "",
					variables: List<Variable> = emptyList(),
					devices: List<Device> = emptyList(),
					jobStatus: Map<String, String> = emptyMap(),
					pipelineStatus: Map<String, String> = emptyMap(),
					stateMachine: List<State> = emptyList()) : HttpConfigInterface {

	var name: String
		get() = nameProperty.get()
		set(value) = nameProperty.set(value)

	val nameProperty: StringProperty = SimpleStringProperty(name)
		@JsonIgnore get

	var isActive: Boolean
		get() = isActiveProperty.get()
		set(value) = isActiveProperty.set(value)

	val isActiveProperty: BooleanProperty = SimpleBooleanProperty(isActive)
		@JsonIgnore get

	var method: String
		get() = methodProperty.get()
		set(value) = methodProperty.set(value)

	val methodProperty: StringProperty = SimpleStringProperty(method)
		@JsonIgnore get

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

	var jobName: String
		get() = jobNameProperty.get()
		set(value) = jobNameProperty.set(value)

	val jobNameProperty: StringProperty = SimpleStringProperty(jobName)
		@JsonIgnore get

	var parameters: String
		get() = parametersProperty.get()
		set(value) = parametersProperty.set(value)

	val parametersProperty: StringProperty = SimpleStringProperty(parameters)
		@JsonIgnore get

	@Suppress("unused")
	private var _variables: List<Variable>
		@JsonProperty("variables") get() = variables
		set(value) {
			variables.setAll(value)
		}

	val variables: ObservableList<Variable> = variables.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _devices: List<Device>
		@JsonProperty("devices") get() = devices
		set(value) {
			devices.setAll(value)
		}

	val devices: ObservableList<Device> = devices.toObservableList()
		@JsonIgnore get

	@Suppress("unused")
	private var _jobStatus: Map<String, String>
		@JsonProperty("jobStatus") get() = jobStatus
		set(value) = jobStatus.setAll(value)

	val jobStatus: ObservableMap<String, String> = jobStatus.toObservableMap()
		@JsonIgnore get

	@Suppress("unused")
	private var _pipelineStatus: Map<String, String>
		@JsonProperty("pipelineStatus") get() = pipelineStatus
		set(value) = pipelineStatus.setAll(value)

	val pipelineStatus: ObservableMap<String, String> = pipelineStatus.toObservableMap()
		@JsonIgnore get

	@Suppress("unused")
	private var _stateMachine: List<State>
		@JsonProperty("stateMachine") get() = stateMachine
		set(value) {
			stateMachine.setAll(value)
		}

	val stateMachine: ObservableList<State> = stateMachine.toObservableList()
		@JsonIgnore get

}