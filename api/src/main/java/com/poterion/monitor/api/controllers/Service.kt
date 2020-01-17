package com.poterion.monitor.api.controllers

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.data.HttpProxy
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.auth.BasicAuthConfig
import com.poterion.monitor.data.services.ServiceConfig
import okhttp3.Authenticator
import okhttp3.ConnectionPool
import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit


/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
abstract class Service<out Config : ServiceConfig>(override val config: Config) : ModuleInstanceInterface<Config> {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(Service::class.java)
	}

	override val navigationRoot: NavigationItem
		get() = NavigationItem(
				title = config.name,
				icon = definition.icon,
				sub = mutableListOf())

	val objectMapper: ObjectMapper = ObjectMapper(JsonFactory())
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.registerModule(KotlinModule())


	private var urlCache: String? = null
	private var authCache: BasicAuthConfig? = null
	private var proxyCache: HttpProxy? = null
	private var proxyAuthCache: BasicAuthConfig? = null
	private var connectTimeoutCache: Long? = null
	private var readTimeoutCache: Long? = null
	private var writeTimeoutCache: Long? = null

	private fun HttpProxy?.getProxy() = this
			?.takeIf { it.address != null }
			?.let { Proxy(Proxy.Type.HTTP, InetSocketAddress(it.address, it.port ?: 80)) }
			?: Proxy.NO_PROXY

	private fun BasicAuthConfig?.getAuthenticator(headerName: String) = this
			?.let { auth -> auth.username.takeIf { it.isNotBlank() } to auth.password.takeIf { it.isNotBlank() } }
			?.let { (username, password) -> username?.let { u -> password?.let { p -> u to p } } }
			?.let { (username, password) ->
				Authenticator { route, response ->
					val credential = Credentials.basic(username, password)
					response.request().newBuilder().header(headerName, credential).build()
				}
			}
			?: Authenticator.NONE

	protected var retrofit: Retrofit? = null
		get() {
			if (field == null
					|| urlCache != config.url
					|| authCache != config.auth
					|| proxyCache != config.proxy
					|| proxyAuthCache != config.proxy?.auth
					|| connectTimeoutCache != config.connectTimeout
					|| readTimeoutCache != config.readTimeout
					|| writeTimeoutCache != config.writeTimeout) try {
				field = Retrofit.Builder()
						.baseUrl(config.url)
						.client(OkHttpClient.Builder()
								.proxy(config.proxy.getProxy())
								.proxyAuthenticator(config.proxy?.auth.getAuthenticator("Proxy-Authorization"))
								.connectionPool(ConnectionPool(1, 1, TimeUnit.MINUTES))
								.connectTimeout(config.connectTimeout ?: 10_000L, TimeUnit.MILLISECONDS)
								.readTimeout(config.readTimeout ?: 10_000L, TimeUnit.MILLISECONDS)
								.writeTimeout(config.writeTimeout ?: 10_000L, TimeUnit.MILLISECONDS)
								.authenticator(config.auth.getAuthenticator("Authorization"))
//								.addInterceptor { chain ->
//									val requestBuilder = chain.request().newBuilder()
//									val auth = config.auth
//									authCache = auth
//
//									if (auth is BasicAuthConfig) requestBuilder.header("Authorization",
//											Base64.getEncoder()
//													.encodeToString("${auth.username}:${auth.password}".toByteArray())
//													.let { "Basic ${it}" })
//
//									val request = requestBuilder.build()
//									LOGGER.debug("${request.method()} ${request.url()}...")
//									chain.proceed(request)
//								}
								.build())
						.addConverterFactory(ScalarsConverterFactory.create())
						.addConverterFactory(JacksonConverterFactory.create(objectMapper))
						.build()
				urlCache = config.url
				authCache = config.auth
				proxyCache = config.proxy
				proxyAuthCache = config.proxy?.auth
				connectTimeoutCache = config.connectTimeout
				readTimeoutCache = config.readTimeout
				writeTimeoutCache = config.writeTimeout
			} catch (e: IllegalArgumentException) {
				LOGGER.error(e.message)
			}
			return field
		}
		private set

	/**
	 * Check implementation.
	 *
	 * @param updater Status updater callback
	 */
	abstract fun check(updater: (Collection<StatusItem>) -> Unit)
}