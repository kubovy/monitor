package com.poterion.monitor.api

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import io.reactivex.subjects.PublishSubject

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
object StatusCollector {
	private val itemMap = mutableMapOf<String, Collection<StatusItem>>()
	var items = emptyList<StatusItem>()
		private set
	val status: PublishSubject<StatusCollector> = PublishSubject.create<StatusCollector>()

	fun filter(silencedIds: Collection<String>,
			   minPriority: Priority,
			   minStatus: Status = Status.NONE,
			   serviceIds: Set<String> = emptySet(),
			   includingChildren: Boolean = false) = items
			.asSequence()
			.filter { it.parentId == null || includingChildren }
			.filterNot { silencedIds.contains(it.id) }
			.filter { it.priority >= minPriority }
			.filter { it.status >= minStatus }
			.filter { serviceIds.isEmpty() || serviceIds.contains(it.serviceId) }
			.toList()

	fun maxStatus(silencedIds: Collection<String>,
				  minPriority: Priority,
				  minStatus: Status = Status.NONE,
				  serviceIds: Set<String> = emptySet(),
				  includingChildren: Boolean = false): Status =
			topStatus(silencedIds, minPriority, minStatus, serviceIds, includingChildren)?.status ?: Status.NONE

	fun topStatuses(silencedIds: Collection<String>,
					minPriority: Priority,
					minStatus: Status = Status.NONE,
					serviceIds: Set<String> = emptySet(),
					includingChildren: Boolean = false) =
			filter(silencedIds, minPriority, minStatus, serviceIds, includingChildren)
					.filter { it.status == maxStatus(silencedIds, minPriority, minStatus, serviceIds) }
					.distinctBy { it.serviceId }

	fun topStatus(silencedIds: Collection<String>,
				  minPriority: Priority,
				  minStatus: Status = Status.NONE,
				  serviceIds: Set<String> = emptySet(),
				  includingChildren: Boolean = false) =
			filter(silencedIds, minPriority, minStatus, serviceIds, includingChildren).maxBy { it.status }

	@Synchronized
	fun update(statusItems: Collection<StatusItem>, update: Boolean) {
		if (!update) statusItems.map { it.serviceId }.distinct().forEach { itemMap.remove(it) }
		val deduplicated = statusItems
				.groupBy { it.id }
				.mapNotNull { (_, duplicates) -> duplicates.maxBy { it.status.ordinal * 100 + it.priority.ordinal } }
				.groupBy { it.serviceId }
		itemMap.putAll(deduplicated)

		items = itemMap.values.flatten()
		status.onNext(this)
	}
}