package com.poterion.monitor.gerrit.code.review.data

import com.poterion.monitor.data.HttpProxy
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.auth.BasicAuthConfig
import com.poterion.monitor.data.services.ServiceConfig
import java.util.*

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class GerritCodeReviewConfig(override var type: String = GerritCodeReviewConfig::class.java.simpleName,
							 override val uuid: String = UUID.randomUUID().toString(),
							 override var name: String = "",
							 override var enabled: Boolean = true,
							 override var url: String = "",
							 override var proxy: HttpProxy? = null,
							 override var trustCertificate: Boolean = false,
							 override var auth: BasicAuthConfig? = null,
							 override var order: Int = Int.MAX_VALUE,
							 override var priority: Priority = Priority.NONE,
							 override var checkInterval: Long = 3600_000L, // 1 hour
							 override var connectTimeout: Long? = null,
							 override var readTimeout: Long? = null,
							 override var writeTimeout: Long? = null,
							 var queries: MutableCollection<GerritCodeReviewQueryConfig> = mutableListOf()) : ServiceConfig