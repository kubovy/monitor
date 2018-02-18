package com.poterion.monitor.sensors.sonar.control

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ServiceController
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.sensors.sonar.data.SonarConfig
import com.poterion.monitor.sensors.sonar.data.SonarProjectResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.URI

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class SonarServiceController(override val controller: ControllerInterface, config: SonarConfig) : ServiceController<SonarConfig>(config) {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(SonarServiceController::class.java)
	}

	private val service = retrofit.create(SonarService::class.java)
	private val projects = config.projects.map { it.name to it }.toMap()
	private var lastFoundProjectNames: Collection<String> = projects.keys

	override fun check(updater: (Collection<StatusItem>) -> Unit) {
		service.check().enqueue(object : Callback<Collection<SonarProjectResponse>> {
			override fun onResponse(call: Call<Collection<SonarProjectResponse>>?, response: Response<Collection<SonarProjectResponse>>?) {
				LOGGER.info("${call?.request()?.method()} ${call?.request()?.url()}: ${response?.code()} ${response?.message()}")

				if (response?.isSuccessful == true) {
					val foundProjects = response.body()
							?.filter { job -> config.filter?.let { job.name.matches(it.toRegex()) } ?: true }

					lastFoundProjectNames = foundProjects?.map { it.name } ?: projects.keys

					foundProjects
							?.map { StatusItem(config.name, it.priority, it.severity, it.name, link = it.uri) }
							?.also(updater)
				} else {
					lastFoundProjectNames
							.mapNotNull { projects[it] }
							.map { StatusItem(config.name, it.priority, Status.SERVICE_ERROR, it.name) }
							.also(updater)
				}
			}

			override fun onFailure(call: Call<Collection<SonarProjectResponse>>?, response: Throwable?) {
				call?.request()?.also { LOGGER.warn("${it.method()} ${it.url()}: ${response?.message}", response) }
						?: LOGGER.warn(response?.message, response)
				lastFoundProjectNames
						.mapNotNull { projects[it] }
						.map { StatusItem(config.name, it.priority, Status.CONNECTION_ERROR, it.name) }
						.also(updater)
			}
		})
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

	private val SonarProjectResponse.uri
		get() = URI("${config.url}dashboard/index/${id}")
}