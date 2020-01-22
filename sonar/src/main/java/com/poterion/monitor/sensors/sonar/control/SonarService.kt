package com.poterion.monitor.sensors.sonar.control

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
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.control.TextField
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class SonarService(override val controller: ControllerInterface, config: SonarConfig) : Service<SonarConfig>(config) {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(SonarService::class.java)
	}

	override val definition: Module<SonarConfig, ModuleInstanceInterface<SonarConfig>> = SonarModule
	private val service
		get() = retrofit?.create(SonarRestService::class.java)
	private val projects = config.projects.map { it.name to it }.toMap()
	private var lastFoundProjectNames: Collection<String> = projects.keys

	private val projectTableSettingsPlugin = TableSettingsPlugin(
			tableName = "projectTable",
			newLabel = "Project ID",
			buttonText = "Add project",
			controller = controller,
			config = config,
			createItem = { SonarProjectConfig() },
			items = config.projects,
			displayName = { name },
			columnDefinitions = listOf(
					TableSettingsPlugin.ColumnDefinition(
							name = "ID",
							getter = { id },
							setter = { id = it },
							initialValue = 0,
							isEditable = true),
					TableSettingsPlugin.ColumnDefinition(
							name = "Project Name",
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
			//			.let { "/#!/project/${it}" }
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
				},
				projectTableSettingsPlugin.rowNewItem)
	override val configurationAddition: List<Parent>
		get() = super.configurationAddition + listOf(projectTableSettingsPlugin.vbox)

	override fun check(updater: (Collection<StatusItem>) -> Unit) {
		try {
			service?.check()?.enqueue(object : Callback<Collection<SonarProjectResponse>> {
				override fun onResponse(call: Call<Collection<SonarProjectResponse>>?, response: Response<Collection<SonarProjectResponse>>?) {
					LOGGER.info("${call?.request()?.method()} ${call?.request()?.url()}: ${response?.code()} ${response?.message()}")

					if (response?.isSuccessful == true) {
						val foundProjects = response.body()
								?.filter { job -> config.filter?.let { job.name.matches(it.toRegex()) } ?: true }

						lastFoundProjectNames = foundProjects?.map { it.name } ?: projects.keys

						foundProjects
								?.map {
									StatusItem(
											id = "${config.uuid}-${it.id}",
											serviceId = config.uuid,
											priority = it.priority,
											status = it.severity,
											title = it.name,
											link = "${config.url}dashboard/index/${it.id}",
											isRepeatable = false)
								}
								?.also(updater)
					} else {
						lastFoundProjectNames
								.mapNotNull { projects[it] }
								.map {
									StatusItem(
											id = "${config.uuid}-${it.id}",
											serviceId = config.uuid,
											priority = it.priority,
											status = Status.SERVICE_ERROR,
											title = it.name,
											link = config.url,
											isRepeatable = false)
								}
								.also(updater)
					}
				}

				override fun onFailure(call: Call<Collection<SonarProjectResponse>>?, response: Throwable?) {
					call?.request()?.also { LOGGER.warn("${it.method()} ${it.url()}: ${response?.message}", response) }
							?: LOGGER.warn(response?.message, response)
					lastFoundProjectNames
							.mapNotNull { projects[it] }
							.map {
								StatusItem(
										id = "${config.uuid}-${it.id}",
										serviceId = config.uuid,
										priority = it.priority,
										status = Status.CONNECTION_ERROR,
										title = it.name,
										link = config.url,
										isRepeatable = false)
							}
							.also(updater)
				}
			})
		} catch (e: IOException) {
			LOGGER.error(e.message, e)
			lastFoundProjectNames
					.mapNotNull { projects[it] }
					.map {
						StatusItem(
								id = "${config.uuid}-${it.id}",
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

	private val SonarProjectResponse.priority
		get() = projects[name]?.priority ?: config.priority

	private val SonarProjectResponse.severity
		get() = (msr.firstOrNull { it.key == "alert_status" }?.data?.toUpperCase() ?: "UNKNOWN").let {
			when (it) {
				"OK" -> Status.OK
				"WARNING" -> Status.WARNING
				"ERROR" -> Status.ERROR
				else -> Status.UNKNOWN
			}
		}
}