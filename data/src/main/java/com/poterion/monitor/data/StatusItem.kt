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
data class StatusItem(val id: String = "",
					  val serviceId: String = "",
					  val priority: Priority = Priority.NONE,
					  val status: Status = Status.NONE,
					  val title: String = "",
					  val group: String? = null,
					  val detail: String? = null,
					  val labels: Map<String, String> = emptyMap(),
					  val link: String? = null,
					  val parentId: String? = null,
					  val parentRequired: Boolean = false,
					  val isRepeatable: Boolean = false,
					  val startedAt: Instant? = null)