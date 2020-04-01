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
package com.poterion.monitor.sensors.jenkins.control

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.TableSettingsPlugin
import com.poterion.monitor.api.utils.toIcon
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.sensors.jenkins.JenkinsModule
import com.poterion.monitor.sensors.jenkins.data.JenkinsConfig
import com.poterion.monitor.sensors.jenkins.data.JenkinsJobConfig
import com.poterion.monitor.sensors.jenkins.data.JenkinsJobResponse
import com.poterion.utils.javafx.toObservableList
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.control.TextField
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException


/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class JenkinsService(override val controller: ControllerInterface, config: JenkinsConfig) :
		Service<JenkinsJobConfig, JenkinsConfig>(config) {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(JenkinsService::class.java)
	}

	override val definition: Module<JenkinsConfig, ModuleInstanceInterface<JenkinsConfig>> = JenkinsModule
	private val service
		get() = retrofit?.create(JenkinsRestService::class.java)
	private val jobs = config.subConfig.map { it.name to it }.toMap()
	private var lastFoundJobNames: Collection<String> = jobs.keys

	private val jobTableSettingsPlugin = TableSettingsPlugin(
			tableName = "jobTable",
			buttonText = "Add job",
			controller = controller,
			config = config,
			createItem = { JenkinsJobConfig() },
			items = config.subConfig,
			displayName = { name },
			columnDefinitions = listOf(
					TableSettingsPlugin.ColumnDefinition(
							name = "Job Name",
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
			actions = listOfNotNull { _ ->
				//Button("", CommonIcon.LINK.toImageView()).apply { setOnAction { gotoSubConfig(item) } }
				null
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
				}, jobTableSettingsPlugin.rowNewItem)
	override val configurationAddition: List<Parent>
		get() = super.configurationAddition + listOf(jobTableSettingsPlugin.vbox)

	override fun gotoSubConfig(subConfig: JenkinsJobConfig) {
		//subConfig.let { URLEncoder.encode(it.name, Charsets.UTF_8.name()) }
		//		.let { "/...TODO.../${it}" }
		//		.let { path -> config.url.toUriOrNull()?.resolve(path) }
		//		?.openInExternalApplication()
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
				val foundJobs = response.body()
						?.jobs
						?.filter { job -> config.filter?.let { job.name.matches(it.toRegex()) } ?: true }

				lastFoundJobNames = foundJobs?.map { it.name } ?: jobs.keys

				return foundJobs
						?.map {
							StatusItem(
									id = "${config.uuid}-${it.name}",
									serviceId = config.uuid,
									configIds = listOfNotNull(jobs[it.name]?.configTitle).toMutableList(),
									priority = jobs[it.name]?.priority ?: config.priority,
									status = it.severity,
									title = it.name,
									link = it.url,
									isRepeatable = false)
						}
						?: emptyList()
			} else {
				LOGGER.warn("${call?.request()?.method()} ${call?.request()?.url()}:" +
						" ${response?.code()} ${response?.message()}")
				error = response?.let { "Code: ${it.code()} ${it.message()}" } ?: "Service error"
				return lastFoundJobNames
						.mapNotNull { jobs[it] }
						.map { jobConfig ->
							jobConfig.toStatusItem(Status.SERVICE_ERROR,
									response?.let { "Code: ${it.code()} ${it.message()}" })
						}
			}
		} catch (e: IOException) {
			LOGGER.error(e.message)
			error = e.message ?: "Connection error"
			return lastFoundJobNames
					.mapNotNull { jobs[it] }
					.map { it.toStatusItem(Status.CONNECTION_ERROR, e.message) }
		} finally {
			lastErrorProperty.set(error)
		}
	}

	private val JenkinsJobResponse.severity
		get() = (color?.toLowerCase() ?: "").let {
			when {
				it.startsWith("disabled") -> Status.OFF
				it.startsWith("notbuild") -> Status.NONE
				it.startsWith("aborted") -> Status.NOTIFICATION
				it.startsWith("blue") -> Status.OK
				it.startsWith("green") -> Status.OK
				it.startsWith("yellow") -> Status.WARNING
				it.startsWith("red") -> Status.ERROR
				else -> Status.UNKNOWN
			}
		}

	private fun JenkinsJobConfig.toStatusItem(status: Status, detail: String?) = StatusItem(
			id = "${config.uuid}-${name}",
			serviceId = config.uuid,
			configIds = mutableListOf(configTitle),
			priority = priority,
			status = status,
			title = name,
			detail = detail,
			link = config.url,
			isRepeatable = false)
}