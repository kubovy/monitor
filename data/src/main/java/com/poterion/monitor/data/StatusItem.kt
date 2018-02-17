package com.poterion.monitor.data

import java.net.URI

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class StatusItem(val serviceName: String,
                      val priority: Priority,
                      val status: Status,
                      val label: String,
                      val detail: String? = null,
                      val link: URI? = null)