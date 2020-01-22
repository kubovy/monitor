package com.poterion.monitor.api.controllers

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.poterion.monitor.api.utils.noop
import com.poterion.monitor.data.HttpConfig
import com.poterion.monitor.data.HttpProxy
import com.poterion.monitor.data.auth.AuthConfig
import com.poterion.monitor.data.auth.BasicAuthConfig
import com.poterion.monitor.data.auth.TokenAuthConfig
import okhttp3.Authenticator
import okhttp3.ConnectionPool
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okio.ByteString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.net.InetSocketAddress
import java.net.Proxy
import java.nio.charset.Charset
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class HttpServiceModule(private val config: HttpConfig) {
	companion object {
		private val LOGGER: Logger = LoggerFactory.getLogger(HttpServiceModule::class.java)
	}

	private var urlCache: String? = null
	private var authCache: AuthConfig? = null
	private var proxyCache: HttpProxy? = null
	private var proxyAuthCache: AuthConfig? = null
	private var connectTimeoutCache: Long? = null
	private var readTimeoutCache: Long? = null
	private var writeTimeoutCache: Long? = null

	val objectMapper: ObjectMapper = ObjectMapper(JsonFactory())
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.registerModule(KotlinModule())

	private val trustAllCerts = arrayOf(object : X509TrustManager {
		override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) = noop()

		override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) = noop()

		override fun getAcceptedIssuers() = emptyArray<X509Certificate>()
	})

	var retrofit: Retrofit? = null
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
								.also {
									if (config.trustCertificate) {
										it.sslSocketFactory(SSLContext.getInstance("SSL").apply {
											init(null, trustAllCerts, SecureRandom())
										}.socketFactory, trustAllCerts[0]).hostnameVerifier { _, _ -> true }
									}
								}
								.addInterceptor { chain ->
									val requestBuilder = chain.request().newBuilder()
									val auth = config.auth
									//authCache = auth

									if (auth is BasicAuthConfig) requestBuilder.header("Authorization",
											Base64.getEncoder()
													.encodeToString("${auth.username}:${auth.password}".toByteArray())
													.let { "Basic ${it}" })

									val request = requestBuilder
											.build()
									LOGGER.debug("${request.method()} ${request.url()}...")
									chain.proceed(request)
								}
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

	private fun HttpProxy?.getProxy() = this
			?.takeIf { it.address != null }
			?.let { Proxy(Proxy.Type.HTTP, InetSocketAddress(it.address, it.port ?: 80)) }
			?: Proxy.NO_PROXY

	private fun AuthConfig?.getAuthenticator(headerName: String) = when (this) {
		is BasicAuthConfig -> this
				.let { auth -> auth.username.takeIf { it.isNotBlank() } to auth.password.takeIf { it.isNotBlank() } }
				.let { (username, password) -> username?.let { u -> password?.let { p -> u to p } } }
				?.let { (username, password) ->
					Authenticator { _, response ->
						val credential = Credentials.basic(username, password)
						response.request().newBuilder().header(headerName, credential).build()
					}
				}
				?: Authenticator.NONE
		is TokenAuthConfig -> this
				.let { auth -> auth.token.takeIf { it.isNotBlank() } }
				?.toByteArray(Charset.forName("ISO-8859-1"))
				?.let { ByteString.of(*it).base64() }
				?.let { "Bearer ${it}" }
				?.let { Authenticator { _, response -> response.request().newBuilder().header(headerName, it).build() } }
				?: Authenticator.NONE
		else -> Authenticator.NONE
	}
}