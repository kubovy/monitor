package com.poterion.monitor.api.utils

import com.poterion.monitor.data.HttpProxy
import com.poterion.monitor.data.auth.AuthConfig
import com.poterion.monitor.data.auth.BasicAuthConfig
import com.poterion.monitor.data.auth.TokenAuthConfig
import com.poterion.utils.kotlin.toUriOrNull
import okhttp3.Authenticator
import okhttp3.Credentials
import okio.ByteString
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy

private val LOGGER = LoggerFactory.getLogger("com.poterion.monitor.api.utils.HttpUtils")

fun HttpProxy.useProxyFor(url: String): Boolean {
	val hostname = url.toUriOrNull()?.host
	val ip = hostname?.let {
		try {
			InetAddress.getByName(it)
		} catch (e: Exception) {
			LOGGER.error(e.message, e)
			null
		}
	}?.hostName
	val noProxies = noProxy?.split("[,; ]")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
	return noProxies
			.map { p -> listOfNotNull(hostname, ip).none { it.endsWith(p) } }
			.takeIf { it.isNotEmpty() }
			?.reduce { acc, b -> acc && b }
			?: true
}

fun HttpProxy?.toProxy(url: String): Proxy = this
		?.let { proxy -> proxy.address?.let { address -> address to proxy } }
		?.takeIf { useProxyFor(url) }
		?.let { (_, proxy) -> Proxy(Proxy.Type.HTTP, InetSocketAddress(proxy.address, proxy.port ?: 80)) }
		?: Proxy.NO_PROXY

fun AuthConfig?.toAuthenticator(headerName: String): Authenticator = when (this) {
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
			?.toByteArray(Charsets.ISO_8859_1)
			?.let { ByteString.of(*it).base64() }
			?.let { "Bearer ${it}" }
			?.let { Authenticator { _, response -> response.request().newBuilder().header(headerName, it).build() } }
			?: Authenticator.NONE
	else -> Authenticator.NONE
}