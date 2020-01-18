package com.poterion.monitor.data

import java.time.Instant

/*
 * @startuml
 * class StatusItem {
 *   serviceName: String
 *   priority: Priority
 *   status: Status
 *   label: String
 *   detail: String?
 *   link: URI?
 * }
 *
 * enum Priority {
 *   NONE
 *   LOW
 *   MEDIUM
 *   HIGH
 *   MAXIMUM
 * }
 *
 * enum Status {
 *   NONE
 *   OFF
 *   UNKNOWN
 *   OK
 *   INFO
 *   NOTIFICATION
 *   CONNECTION_ERROR
 *   SERVICE_ERROR
 *   WARNING
 *   ERROR
 *   FATAL
 * }
 *
 * StatusItem -left- Status
 * StatusItem -right- Priority
 * @enduml
 */
/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class StatusItem(val serviceId: String,
					  val priority: Priority,
					  val status: Status,
					  val title: String,
					  val group: String? = null,
					  val detail: String? = null,
					  val labels: Map<String, String> = emptyMap(),
					  val link: String? = null,
					  val startedAt: Instant = Instant.now())