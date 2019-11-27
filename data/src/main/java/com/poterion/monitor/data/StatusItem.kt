package com.poterion.monitor.data

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
data class StatusItem(val serviceName: String,
					  val priority: Priority,
					  val status: Status,
					  val label: String,
					  val detail: String? = null,
					  val link: String? = null)