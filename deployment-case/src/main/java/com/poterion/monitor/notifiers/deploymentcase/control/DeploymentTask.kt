package com.poterion.monitor.notifiers.deploymentcase.control

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.hubspot.jinjava.Jinjava
import com.poterion.monitor.api.communication.BluetoothCommunicator
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.notifiers.deploymentcase.data.Configuration
import javafx.application.Platform
import okhttp3.OkHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class DeploymentTask(private val configuration: Configuration,
					 private val communicator: BluetoothCommunicator,
					 private val context: Map<String, Any?>,
					 private val onFinish: () -> Unit) : Runnable {

	companion object {
		private val LOGGER: Logger = LoggerFactory.getLogger(DeploymentTask::class.java)
	}

	private val trustAllCerts = arrayOf(object : X509TrustManager {
		override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {
		}

		override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {
		}

		override fun getAcceptedIssuers() = emptyArray<X509Certificate>()
	})

	private val jinjava = Jinjava()
	private var failedCount = AtomicInteger(0)
	private val states = mutableMapOf<String, String>()
	private var isFinished = AtomicBoolean(false)
	private var previousBuildNumber: Int = 0
	private var status = "pending"

	override fun run() {
		Platform.runLater { communicator.send("state,status,${status}") }

		try {
			val response = service.buildStatus(configuration.jobName).execute()
			val body = response.body()
			previousBuildNumber = body?.elements()
					?.asSequence()
					?.map { it.get("id").asInt(0) }
					?.max()
					?: 0
			val lastBuildState = body?.elements()
					?.asSequence()
					?.find { it.get("id").asInt(0) == previousBuildNumber }
					?.get("state")
					?.asText()
					?.toLowerCase()
			if (lastBuildState == "in_progress") previousBuildNumber = -1

			if (previousBuildNumber != -1) {
				val parameters = jinjava.render(configuration.parameters.replace("[\\n\\r]".toRegex(), ""), context)
				val url = "${configuration.url}job/${configuration.jobName}/buildWithParameters?${parameters}"
				service.fire(url).execute()
			}
			updateLooper()
			LOGGER.info("FINISHED: ${isFinished.get()} (failed ${failedCount.get()} times)")
			status = "success"
			Platform.runLater { communicator.send("state,status,${status}") }
		} catch (e: IOException) {
			LOGGER.error("FAILED: ${isFinished.get()} (failed ${failedCount.get()} times)")
			status = "failure"
			Platform.runLater { communicator.send("state,status,${status}") }
		} finally {
			onFinish()
		}
	}

	private val retrofit: Retrofit
		get() = Retrofit.Builder()
				.baseUrl(configuration.url)
				.client(OkHttpClient.Builder()
						.sslSocketFactory(SSLContext.getInstance("SSL").apply {
							init(null, trustAllCerts, SecureRandom())
						}.socketFactory, trustAllCerts[0])
						.hostnameVerifier { _, _ -> true }
						.addInterceptor { chain ->
							val requestBuilder = chain.request().newBuilder()

							if (configuration.username.isNotEmpty()) requestBuilder.header("Authorization",
									Base64.getEncoder()
											.encodeToString("${configuration.username}:${configuration.password}".toByteArray())
											.let { "Basic ${it}" })

							val request = requestBuilder.build()
							Service.LOGGER.debug("${request.method()} ${request.url()}...")
							chain.proceed(request)
						}.build())
				.addConverterFactory(JacksonConverterFactory.create(ObjectMapper(JsonFactory()).apply {
					disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				}))
				.build()

	private val service = retrofit.create(JenkinsRestService::class.java)

	private fun updateLooper() {
		while (!isFinished.get() && failedCount.get() < 5) {
			states.clear()
			try {
				val response = service.buildStatus(configuration.jobName).execute()
				val body = response.body()
				val lastBuildNumber = body?.elements()
						?.asSequence()
						?.map { it.get("id").asInt(0) }
						?.max()
				LOGGER.info("Last build number: ${lastBuildNumber}, previous build number: ${previousBuildNumber}")
				if (lastBuildNumber != previousBuildNumber) {
					if (status != "in_progress") {
						status = "in_progress"
						Platform.runLater { communicator.send("state,status,${status}") }
					}
					val lastBuild = body?.elements()
							?.asSequence()
							?.find { it.get("id").asInt(0) == lastBuildNumber }
					if (response.isSuccessful && lastBuild?.has("status") == true) {
						failedCount.set(0)

						LOGGER.debug("${configuration.jobName}: ${lastBuild.get("status").asText()}")
						states[configuration.jobName.sanitizeJobName()] = lastBuild.get("status").asText("unknown").toLowerCase()

						lastBuild.get("stages").elements().forEach { stage ->
							val name = stage.get("name").asText().sanitizeJobName()

							val status = if (stage.has("_links")
									&& stage.get("_links").has("self")
									&& stage.get("_links").get("self").has("href")) {
								val url = stage.get("_links").get("self").get("href").asText()
								try {
									service.get(url).execute().body()
											?.takeIf { it.has("status") }
											?.let { it.get("status").asText("unknown") }
											?.toLowerCase()
											?: "unknown"
								} catch (e: IOException) {
									stage.get("status").asText("unknown").toLowerCase()
								}
							} else stage.get("status").asText("unknown").toLowerCase()

							LOGGER.info("${name}: ${status}")
							states[name] = status
						}

						if (lastBuild.get("status").asText() != "IN_PROGRESS") isFinished.set(true)
					} else {
						failedCount.incrementAndGet()
					}
				}
			} catch (e: IOException) {
				failedCount.incrementAndGet()
			}
			states.map { (name, status) -> "state,job_${name},${status}" }
					.joinToString(";")
					.also { Platform.runLater { communicator.send(it) } }
			Thread.sleep(5_000)
		}
	}

	private fun String.sanitizeJobName() = this.toLowerCase()
			.replace(" ", "_")
			.replace("[^a-z_\\-]".toRegex(), "x")
}