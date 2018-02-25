package com.poterion.monitor.sensors.jenkins.control

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.sensors.jenkins.data.JenkinsConfig
import com.poterion.monitor.sensors.jenkins.data.JenkinsJobResponse
import com.poterion.monitor.sensors.jenkins.data.JenkinsResponse
import com.poterion.monitor.sensors.jenkins.ui.JenkinsIcon
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.net.URI

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class JenkinsService(override val controller: ControllerInterface, config: JenkinsConfig) : Service<JenkinsConfig>(config) {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(JenkinsService::class.java)
	}

	private val service
			get() = retrofit.create(JenkinsRestService::class.java)
	private val jobs = config.jobs.map { it.name to it }.toMap()
	private var lastFoundJobNames: Collection<String> = jobs.keys
	override val icon: Icon = JenkinsIcon.JENKINS

	override fun check(updater: (Collection<StatusItem>) -> Unit) {
		try {
			service.check().enqueue(object : Callback<JenkinsResponse> {
				override fun onResponse(call: Call<JenkinsResponse>?, response: Response<JenkinsResponse>?) {
					LOGGER.info("${call?.request()?.method()} ${call?.request()?.url()}: ${response?.code()} ${response?.message()}")

					if (response?.isSuccessful == true) {
						val foundJobs = response.body()
								?.jobs
								?.filter { job -> config.filter?.let { job.name.matches(it.toRegex()) } ?: true }

						lastFoundJobNames = foundJobs?.map { it.name } ?: jobs.keys

						foundJobs
								?.map { StatusItem(config.name, it.priority, it.severity, it.name, link = it.uri) }
								?.also(updater)
					} else {
						lastFoundJobNames
								.mapNotNull { jobs[it] }
								.map { StatusItem(config.name, it.priority, Status.SERVICE_ERROR, it.name) }
								.also(updater)
					}
				}

				override fun onFailure(call: Call<JenkinsResponse>?, response: Throwable?) {
					call?.request()?.also { LOGGER.warn("${it.method()} ${it.url()}: ${response?.message}", response) }
							?: LOGGER.warn(response?.message, response)
					lastFoundJobNames
							.mapNotNull { jobs[it] }
							.map { StatusItem(config.name, it.priority, Status.CONNECTION_ERROR, it.name) }
							.also(updater)
				}
			})
		} catch (e: IOException) {
			LOGGER.error(e.message, e)
			lastFoundJobNames
					.mapNotNull { jobs[it] }
					.map { StatusItem(config.name, it.priority, Status.CONNECTION_ERROR, it.name) }
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

	private val JenkinsJobResponse.uri
		get() = url?.let { URI(it) }
}