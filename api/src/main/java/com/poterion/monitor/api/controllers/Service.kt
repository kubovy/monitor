package com.poterion.monitor.api.controllers

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.auth.BasicAuthConfig
import com.poterion.monitor.data.services.ServiceConfig
import dorkbox.systemTray.MenuItem
import okhttp3.OkHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
abstract class Service<out Config : ServiceConfig>(override val config: Config) : ModuleInterface<Config> {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(Service::class.java)
	}

	override val navigationRoot: NavigationItem
		get() = NavigationItem(
				title = config.name,
				icon = icon,
				update = { entry, _ -> (entry as MenuItem).text = config.name },
				sub = mutableListOf())

	protected val retrofit: Retrofit
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

	/**
	 * Check implementation.
	 *
	 * @param updater Status updater callback
	 */
	abstract fun check(updater: (Collection<StatusItem>) -> Unit)
}