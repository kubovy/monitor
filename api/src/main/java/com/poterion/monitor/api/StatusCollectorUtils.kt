/******************************************************************************
 * Copyright (c) 2020 Jan Kubovy <jan@kubovy.eu>                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify it    *
 * under the terms of the GNU General Public License as published by the Free *
 * Software Foundation, version 3.                                            *
 *                                                                            *
 * This program is distributed in the hope that it will be useful, but        *
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY *
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License    *
 * for more details.                                                          *
 *                                                                            *
 * You should have received a copy of the GNU General Public License along    *
 * with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ******************************************************************************/
package com.poterion.monitor.api

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.notifiers.NotifierServiceReference

fun Collection<StatusItem>.filter(silencedIds: Collection<String>,
								  minPriority: Priority,
								  minStatus: Status = Status.NONE,
								  serviceReferences: Collection<NotifierServiceReference> = emptySet(),
								  includingChildren: Boolean = false) = asSequence()
		.filter { it.parentId == null || includingChildren }
		.filterNot { silencedIds.contains(it.id) }
		.filter { it.priority >= minPriority }
		.filter { it.status >= minStatus }
		.filter { item ->
			serviceReferences.isEmpty()
					|| serviceReferences.any {
				it.uuid == item.serviceId
						&& it.minPriority <= item.priority
						&& it.minStatus <= item.status
			}
		}
		.toList()

fun Collection<StatusItem>.maxStatus(silencedIds: Collection<String>,
									 minPriority: Priority,
									 minStatus: Status = Status.NONE,
									 serviceReferences: Collection<NotifierServiceReference> = emptySet(),
									 includingChildren: Boolean = false): Status =
		topStatus(silencedIds, minPriority, minStatus, serviceReferences, includingChildren)?.status ?: Status.NONE

fun Collection<StatusItem>.topStatusesPerService(silencedIds: Collection<String>,
												 minPriority: Priority,
												 minStatus: Status = Status.NONE,
												 serviceReferences: Collection<NotifierServiceReference> = emptySet(),
												 includingChildren: Boolean = false) =
		topStatuses(silencedIds, minPriority, minStatus, serviceReferences, includingChildren)
				.sortedByDescending { it.status.ordinal * 100 + it.priority.ordinal }
				.distinctBy { it.serviceId }

fun Collection<StatusItem>.topStatuses(silencedIds: Collection<String>,
									   minPriority: Priority,
									   minStatus: Status = Status.NONE,
									   serviceReferences: Collection<NotifierServiceReference> = emptySet(),
									   includingChildren: Boolean = false) =
		filter(silencedIds, minPriority, minStatus, serviceReferences, includingChildren)
				.filter { it.status == maxStatus(silencedIds, minPriority, minStatus, serviceReferences) }

fun Collection<StatusItem>.topStatus(silencedIds: Collection<String>,
									 minPriority: Priority,
									 minStatus: Status = Status.NONE,
									 serviceReferences: Collection<NotifierServiceReference> = emptySet(),
									 includingChildren: Boolean = false) =
		filter(silencedIds, minPriority, minStatus, serviceReferences, includingChildren)
				.maxBy { it.status.ordinal * 100 + it.priority.ordinal }