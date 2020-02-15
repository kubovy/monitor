package com.poterion.monitor.notifiers.deploymentcase.control

import com.hubspot.jinjava.Jinjava
import com.poterion.monitor.api.controllers.HttpServiceModule
import com.poterion.monitor.notifiers.deploymentcase.data.Configuration
import javafx.application.Platform
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Triggers a Jenkins job, if not already running and checks its status including all downstream jobs until the root
 * job is finished.
 *
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class DeploymentTask(configuration: Configuration,
					 context: Map<String, Any?>,
					 private val onUpdate: (Collection<Pair<String, String>>) -> Unit,
					 private val onFinish: () -> Unit): Runnable {

	companion object {
		private val LOGGER: Logger = LoggerFactory.getLogger(DeploymentTask::class.java)
		private const val NOT_RUN_YET = 0
		private const val IN_PROGRESS = -1
	}

	private val jinjava = Jinjava()
	private var failedCount = AtomicInteger(0)
	private val states = mutableMapOf<String, String>()
	private var isFinished = AtomicBoolean(false)
	private var previousBuildNumber: Int = 0
	private var status = "pending"
	private val jobName = configuration.jobName
	private val triggerUrl = "${configuration.url}job/${configuration.jobName}/buildWithParameters?" +
			jinjava.render(configuration.parameters.replace("[\\n\\r]".toRegex(), ""), context)

	override fun run() {
		triggerUpdate("pending")

		try {
			previousBuildNumber = getPreviousCompletedBuildNumber()

			if (previousBuildNumber != IN_PROGRESS) { // Trigger if currently not running
				service?.post(triggerUrl)?.execute()
			}
			updateLooper()
			LOGGER.info("FINISHED: ${isFinished.get()} (failed ${failedCount.get()} times)")
			triggerUpdate("success")
		} catch (e: IOException) {
			LOGGER.error("FAILED: ${isFinished.get()} (failed ${failedCount.get()} times)")
			triggerUpdate("failure")
		} finally {
			onFinish()
		}
	}

	private var httpServiceModule: HttpServiceModule? = null

	private val retrofit: Retrofit?
		get() = httpServiceModule?.retrofit

	private val service = retrofit?.create(JenkinsRestService::class.java)

	private fun getPreviousCompletedBuildNumber() = service
			?.buildStatus(jobName)
			?.execute()
			?.body()
			?.elements()
			?.asSequence()
			?.map { it.get("id").asInt(0) to it.get("state").asText() }
			?.maxBy { (buildNumber, _) -> buildNumber }
			?.let { (buildNumber, state) -> if (state == "in_progress") IN_PROGRESS else buildNumber }
			?: NOT_RUN_YET

	private fun updateLooper() {
		while (!isFinished.get() && failedCount.get() < 5) try {
			val response = service?.buildStatus(jobName)?.execute()
			val body = response?.body()
			val lastBuildNumber = body?.elements()
					?.asSequence()
					?.map { it.get("id").asInt(0) }
					?.max()
			LOGGER.info("Last build number: ${lastBuildNumber}, previous build number: ${previousBuildNumber}")
			if (lastBuildNumber != previousBuildNumber) {
				triggerUpdate("in_progress")
				val lastBuild = body?.elements()
						?.asSequence()
						?.find { it.get("id").asInt(0) == lastBuildNumber }
				if (response?.isSuccessful == true && lastBuild?.has("status") == true) {
					failedCount.set(0)

					LOGGER.debug("${jobName}: ${lastBuild.get("status").asText()}")
					states["job_${jobName.sanitizeJobName()}"] = lastBuild
							.get("status").asText("unknown").toLowerCase()

					lastBuild.get("stages").elements().forEach { stage ->
						val name = stage.get("name").asText().sanitizeJobName()

						val status = if (stage.has("_links")
								&& stage.get("_links").has("self")
								&& stage.get("_links").get("self").has("href")) {
							val url = stage.get("_links").get("self").get("href").asText()
							try {
								service?.get(url)
										?.execute()
										?.body()
										?.takeIf { it.has("status") }
										?.get("status")
										?.asText("unknown")
										?.toLowerCase()
										?: "unknown"
							} catch (e: IOException) {
								stage.get("status").asText("unknown").toLowerCase()
							}
						} else stage.get("status").asText("unknown").toLowerCase()

						LOGGER.info("${name}: ${status}")
						states["job_${name}"] = status
					}

					if (lastBuild.get("status").asText() != "IN_PROGRESS") isFinished.set(true)
				} else {
					failedCount.incrementAndGet()
				}
			}
		} catch (e: IOException) {
			failedCount.incrementAndGet()
		} finally {
			triggerUpdate()
			Thread.sleep(5_000)
		}
	}

	private fun triggerUpdate(status: String? = null) {
		val statesToSend = states.map { (k, v) -> k to v }.toMutableList()

		if (status != null && this.status != status) {
			this.status = status
			statesToSend.add("status" to status)
		}

		if (statesToSend.isNotEmpty()) Platform.runLater { onUpdate(statesToSend) }
		states.clear()
	}

	private fun String.sanitizeJobName() = this.toLowerCase()
			.replace(" ", "_")
			.replace("[^a-z_\\-]".toRegex(), "x")
}