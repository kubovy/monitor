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
class JenkinsService(override val controller: ControllerInterface, config: JenkinsConfig) : Service<JenkinsConfig>(config) {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(JenkinsService::class.java)
	}

	override val definition: Module<JenkinsConfig, ModuleInstanceInterface<JenkinsConfig>> = JenkinsModule
	private val service
		get() = retrofit?.create(JenkinsRestService::class.java)
	private val jobs = config.jobs.map { it.name to it }.toMap()
	private var lastFoundJobNames: Collection<String> = jobs.keys

	private val jobTableSettingsPlugin = TableSettingsPlugin(
			tableName = "jobTable",
			buttonText = "Add job",
			controller = controller,
			config = config,
			createItem = { JenkinsJobConfig() },
			items = config.jobs,
			displayName = { name },
			columnDefinitions = listOf(
					TableSettingsPlugin.ColumnDefinition(
							name = "Job Name",
							getter = { name },
							setter = { name = it },
							initialValue = "",
							isEditable = true),
					TableSettingsPlugin.ColumnDefinition(
							name = "Priority",
							getter = { priority },
							setter = { priority = it },
							initialValue = Priority.NONE,
							isEditable = true,
							icon = { toIcon() },
							options = { Priority.values().toList() })),
			comparator = compareBy({ -it.priority.ordinal }, { it.name })//,
			//actions = listOf { item ->
			//	item.let { URLEncoder.encode(it.name, Charsets.UTF_8.name()) }
			//			.let { "/...TODO.../${it}" }
			//			.let { path -> config.url.toUriOrNull()?.resolve(path) }
			//			?.let { uri -> Button("", com.poterion.monitor.api.CommonIcon.LINK.toImageView()) to uri }
			//			?.also { (btn, uri) -> btn.setOnAction { Desktop.getDesktop().browse(uri) } }
			//			?.first
			//}
	)

	override val configurationRows: List<Pair<Node, Node>>
		get() = super.configurationRows + listOf(
				Label("Filter").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to TextField(config.filter).apply {
					textProperty().addListener { _, _, filter -> config.filter = filter }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				}, jobTableSettingsPlugin.rowNewItem)
	override val configurationAddition: List<Parent>
		get() = super.configurationAddition + listOf(jobTableSettingsPlugin.vbox)

	override fun check(updater: (Collection<StatusItem>) -> Unit) {
		try {
			val call = service?.check()
			try {
				val response = call?.execute()
				LOGGER.info("${call?.request()?.method()} ${call?.request()?.url()}:" +
						" ${response?.code()} ${response?.message()}")
				if (response?.isSuccessful == true) {
					val foundJobs = response.body()
						?.jobs
						?.filter { job -> config.filter?.let { job.name.matches(it.toRegex()) } ?: true }

					lastFoundJobNames = foundJobs?.map { it.name } ?: jobs.keys

					foundJobs
						?.map {
							StatusItem(
								id = "${config.uuid}-${it.name}",
								serviceId = config.uuid,
								priority = it.priority,
								status = it.severity,
								title = it.name,
								link = it.url,
								isRepeatable = false)
						}
						?.also(updater)
				} else {
					lastFoundJobNames
						.mapNotNull { jobs[it] }
						.map {
							StatusItem(
								id = "${config.uuid}-${it.name}",
								serviceId = config.uuid,
								priority = it.priority,
								status = Status.SERVICE_ERROR,
								title = it.name,
								link = config.url,
								isRepeatable = false)
						}
						.also(updater)
				}
			} catch (e: Exception) {
				call?.request()?.also { LOGGER.warn("${it.method()} ${it.url()}: ${e.message}", e) }
					?: LOGGER.warn(e.message, e)
				lastFoundJobNames
					.mapNotNull { jobs[it] }
					.map {
						StatusItem(
							id = "${config.uuid}-${it.name}",
							serviceId = config.uuid,
							priority = it.priority,
							status = Status.CONNECTION_ERROR,
							title = it.name,
							link = config.url,
							isRepeatable = false)
					}
					.also(updater)
			}
		} catch (e: IOException) {
			LOGGER.error(e.message, e)
			lastFoundJobNames
					.mapNotNull { jobs[it] }
					.map {
						StatusItem(
								id = "${config.uuid}-${it.name}",
								serviceId = config.uuid,
								priority = it.priority,
								status = Status.CONNECTION_ERROR,
								title = it.name,
								link = config.url,
								isRepeatable = false)
					}
					.also(updater)
		}
	}

	private val JenkinsJobResponse.priority
		get() = jobs[name]?.priority ?: config.priority

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
}