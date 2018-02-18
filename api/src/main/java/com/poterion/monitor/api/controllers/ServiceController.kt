package com.poterion.monitor.api.controllers

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.poterion.monitor.api.ui.Item
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.auth.BasicAuthConfig
import com.poterion.monitor.data.services.ServiceConfig
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
abstract class ServiceController<out Config : ServiceConfig>(val config: Config) : ModuleController {
	companion object {
		val LOGGER = LoggerFactory.getLogger(ServiceController::class.java)
	}

	override val menuItem: Item
		get() = Item(title = config.name, sub = mutableListOf())

	protected val retrofit
		get() = Retrofit.Builder()
				.baseUrl(config.url)
				.client(OkHttpClient.Builder()
						.connectTimeout(config.connectTimeout ?: 10_000L, TimeUnit.MILLISECONDS)
						.readTimeout(config.readTimeout ?: 10_000L, TimeUnit.MILLISECONDS)
						.writeTimeout(config.writeTimeout ?: 10_000L, TimeUnit.MILLISECONDS)
						.addInterceptor { chain ->
							val requestBuilder = chain.request().newBuilder()
							val auth = config.auth

							if (auth is BasicAuthConfig) requestBuilder.header("Authorization",
									Base64.getEncoder()
											.encodeToString("${auth.username}:${auth.password}".toByteArray())
											.let { "Basic ${it}" })

							val request = requestBuilder.build()
							LOGGER.debug("${request.method()} ${request.url()}...")
							chain.proceed(request)
						}.build())
				.addConverterFactory(JacksonConverterFactory.create(ObjectMapper(JsonFactory()).apply {
					disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				}))
				.build()

	abstract fun check(updater: (Collection<StatusItem>) -> Unit)
}