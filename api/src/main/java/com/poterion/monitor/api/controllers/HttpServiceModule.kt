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
package com.poterion.monitor.api.controllers

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.poterion.monitor.api.utils.toAuthenticator
import com.poterion.monitor.api.utils.toProxy
import com.poterion.monitor.data.HttpConfig
import com.poterion.monitor.data.HttpProxy
import com.poterion.monitor.data.auth.AuthConfig
import com.poterion.monitor.data.auth.BasicAuthConfig
import com.poterion.monitor.data.auth.TokenAuthConfig
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.utils.kotlin.noop
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class HttpServiceModule(private val appConfig: ApplicationConfiguration, private val config: HttpConfig) {
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

	private val trustAllCerts = arrayOf(object: X509TrustManager {
		override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) = noop()

		override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) = noop()

		override fun getAcceptedIssuers() = emptyArray<X509Certificate>()
	})

	var retrofit: Retrofit? = null
		get() {
			if (field == null
					|| urlCache != config.url
					|| authCache != config.auth
					|| proxyCache != appConfig.proxy
					|| proxyAuthCache != appConfig.proxy?.auth
					|| connectTimeoutCache != config.connectTimeout
					|| readTimeoutCache != config.readTimeout
					|| writeTimeoutCache != config.writeTimeout
			) try {
				field = Retrofit.Builder()
						.baseUrl(config.url)
						.client(OkHttpClient.Builder()
								.proxy(appConfig.proxy.toProxy(config.url))
								.proxyAuthenticator(appConfig.proxy?.auth.toAuthenticator("Proxy-Authorization"))
								.connectionPool(ConnectionPool(1, 1, TimeUnit.MINUTES))
								.connectTimeout(config.connectTimeout ?: 10_000L, TimeUnit.MILLISECONDS)
								.readTimeout(config.readTimeout ?: 10_000L, TimeUnit.MILLISECONDS)
								.writeTimeout(config.writeTimeout ?: 10_000L, TimeUnit.MILLISECONDS)
								.authenticator(config.auth.toAuthenticator("Authorization"))
								.also {
									if (config.trustCertificate) {
										it.sslSocketFactory(SSLContext.getInstance("SSL").apply {
											init(null, trustAllCerts, SecureRandom())
										}.socketFactory, trustAllCerts[0]).hostnameVerifier { _, _ -> true }
									}
								}
								.addInterceptor { chain ->
									val requestBuilder = chain.request().newBuilder()
									when (val auth = config.auth) {
										is BasicAuthConfig -> requestBuilder
												.header("Authorization", Base64.getEncoder()
														.encodeToString("${auth.username}:${auth.password}".toByteArray())
														.let { "Basic ${it}" })
										is TokenAuthConfig -> requestBuilder
												.header("Authorization", "Bearer ${auth.token}")
									}
									val request = requestBuilder.build()
									try {
										val response = chain.proceed(request)
										LOGGER.debug("${request.method()} ${request.url()}...")
										response
									} catch (t: Throwable) {
										LOGGER.error("${request.method()} ${request.url()}: ${t.message}", t)
										chain.proceed(chain.request())
									}
								}
								.build())
						.addConverterFactory(ScalarsConverterFactory.create())
						.addConverterFactory(JacksonConverterFactory.create(objectMapper))
						.build()
				urlCache = config.url
				authCache = config.auth
				proxyCache = appConfig.proxy
				proxyAuthCache = appConfig.proxy?.auth
				connectTimeoutCache = config.connectTimeout
				readTimeoutCache = config.readTimeout
				writeTimeoutCache = config.writeTimeout
			} catch (e: IllegalArgumentException) {
				LOGGER.error(e.message)
			}
			return field
		}
		private set
}