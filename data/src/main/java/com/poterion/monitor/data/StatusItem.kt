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
package com.poterion.monitor.data

import java.time.Instant

/*
 * @startuml
 * class StatusItem {
 *   id: String
 *   parentId: String?
 *   parentRequired: Boolean
 *   serviceId: String
 *   priority: Priority
 *   status: Status
 *   title: String
 *   detail: String
 *   labels: Map<String, String>
 *   link: String?
 *   children: Collection<String>
 *   startedAt: Instant
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
 * @author Jan Kubovy [jan@kubovy.eu]
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
					  val children: Collection<String> = emptyList(),
					  val isRepeatable: Boolean = false,
					  val startedAt: Instant? = null)