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
package com.poterion.monitor.sensors.sonar.control

import com.poterion.monitor.api.CommonIcon
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.TableSettingsPlugin
import com.poterion.monitor.api.utils.toIcon
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.sensors.sonar.SonarModule
import com.poterion.monitor.sensors.sonar.data.SonarConfig
import com.poterion.monitor.sensors.sonar.data.SonarProjectConfig
import com.poterion.monitor.sensors.sonar.data.SonarProjectResponse
import com.poterion.utils.javafx.openInExternalApplication
import com.poterion.utils.javafx.toImageView
import com.poterion.utils.javafx.toObservableList
import com.poterion.utils.kotlin.setAll
import com.poterion.utils.kotlin.toUriOrNull
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URLEncoder

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class SonarService(override val controller: ControllerInterface, config: SonarConfig) :
		Service<SonarProjectConfig, SonarConfig>(config) {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(SonarService::class.java)
	}

	override val definition: Module<SonarConfig, ModuleInstanceInterface<SonarConfig>> = SonarModule
	private val service
		get() = retrofit?.create(SonarRestService::class.java)
	private var lastFoundProjectIds: MutableList<Int> = mutableListOf()

	private val projectTableSettingsPlugin = TableSettingsPlugin(
			tableName = "projectTable",
			newLabel = "Project ID",
			buttonText = "Add project",
			controller = controller,
			config = config,
			createItem = { SonarProjectConfig() },
			items = config.subConfig,
			displayName = { name },
			columnDefinitions = listOf(
					TableSettingsPlugin.ColumnDefinition<SonarProjectConfig, Number>(
							name = "ID",
							property = { idProperty },
							initialValue = 0,
							isEditable = true),
					TableSettingsPlugin.ColumnDefinition(
							name = "Project Name",
							property = { nameProperty },
							initialValue = "",
							isEditable = true),
					TableSettingsPlugin.ColumnDefinition(
							name = "Priority",
							property = { priorityProperty },
							initialValue = Priority.NONE,
							isEditable = true,
							icon = { toIcon() },
							options = Priority.values().toObservableList())),
			comparator = compareBy({ -it.priority.ordinal }, { it.name }),
			actions = listOf { item ->
				Button("", CommonIcon.LINK.toImageView()).apply { setOnAction { gotoSubConfig(item) } }
			}
	)

	override val configurationRows: List<Pair<Node, Node>>
		get() = super.configurationRows + listOf(
				Label("Filter").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to TextField(config.filter).apply {
					textProperty().bindBidirectional(config.filterProperty)
					focusedProperty().addListener { _, _, focused -> if (!focused) controller.saveConfig() }
				},
				projectTableSettingsPlugin.rowNewItem)
	override val configurationAddition: List<Parent>
		get() = super.configurationAddition + listOf(projectTableSettingsPlugin.vbox)

	override fun gotoSubConfig(subConfig: SonarProjectConfig) {
		subConfig
				.let { URLEncoder.encode(it.name, Charsets.UTF_8.name()) }
				.let { "/#!/project/${it}" }
				.let { path -> config.url.toUriOrNull()?.resolve(path) }
				?.openInExternalApplication()
	}

	override fun doCheck(): List<StatusItem> {
		var error: String? = null
		try {
			val call = service?.check()
			val response = call?.execute()
			checkForInterruptions()

			LOGGER.info("${call?.request()?.method()} ${call?.request()?.url()}:" +
					" ${response?.code()} ${response?.message()}")

			if (response?.isSuccessful == true) {
				val foundProjects = response.body()
						?.filter { project -> config.filter?.let { project.name.matches(it.toRegex()) } != false }

				lastFoundProjectIds.setAll(foundProjects?.map { it.id } ?: config.subConfig.map { it.id })

				return foundProjects
						?.mapNotNull { entity -> config.subConfig.find { it.id == entity.id }?.let { entity to it } }
						?.map { (entity, projectConfig) ->
							StatusItem(
									id = "${config.uuid}-${entity.id}",
									serviceId = config.uuid,
									configIds = mutableListOf(projectConfig.configTitle),
									priority = projectConfig.priority,
									status = entity.severity,
									title = entity.name,
									link = "${config.url}dashboard/index/${entity.id}",
									isRepeatable = false)
						}
						?: emptyList()
			} else {
				LOGGER.warn("${call?.request()?.method()} ${call?.request()?.url()}:" +
						" ${response?.code()} ${response?.message()}")
				error = response?.let { "Code: ${it.code()} ${it.message()}" } ?: "Service error"
				return lastFoundProjectIds
						.mapNotNull { id -> config.subConfig.find { it.id == id } }
						.map { projectConfig ->
							projectConfig.toStatusItem(Status.SERVICE_ERROR,
									response?.let { "Code: ${it.code()} ${it.message()}" })
						}
			}
		} catch (e: IOException) {
			LOGGER.error(e.message)
			error = e.message ?: "Connection error"
			return lastFoundProjectIds
					.mapNotNull { id -> config.subConfig.find { it.id == id } }
					.map { projectConfig -> projectConfig.toStatusItem(Status.CONNECTION_ERROR, e.message) }
		} finally {
			lastErrorProperty.set(error)
		}
	}

	private val SonarProjectResponse.severity
		get() = (msr.firstOrNull { it.key == "alert_status" }?.data?.toUpperCase() ?: "UNKNOWN").let {
			when (it) {
				"OK" -> Status.OK
				"WARNING" -> Status.WARNING
				"ERROR" -> Status.ERROR
				else -> Status.UNKNOWN
			}
		}

	private fun SonarProjectConfig.toStatusItem(status: Status, detail: String?) = StatusItem(
			id = "${config.uuid}-${uuid}",
			serviceId = config.uuid,
			configIds = mutableListOf(configTitle),
			priority = priority,
			status = status,
			title = name,
			detail = detail,
			link = config.url,
			isRepeatable = false)
}