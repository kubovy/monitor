package com.poterion.monitor.api

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import io.reactivex.subjects.PublishSubject
import org.slf4j.LoggerFactory

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
object StatusCollector {
	private val LOGGER = LoggerFactory.getLogger(StatusCollector::class.java)
	private val itemMap = mutableMapOf<String, Collection<StatusItem>>()
	var items = emptyList<StatusItem>()
		private set
	val status: PublishSubject<StatusCollector> = PublishSubject.create<StatusCollector>()

	fun filter(silencedIds: Collection<String>, minPriority: Priority, minStatus: Status = Status.NONE, serviceIds: Set<String> = emptySet()) = items
			.filterNot { silencedIds.contains(it.id) }
			.filter { it.priority >= minPriority }
			.filter { it.status >= minStatus }
			.filter { serviceIds.isEmpty() || serviceIds.contains(it.serviceId) }

	fun maxStatus(silencedIds: Collection<String>, minPriority: Priority, minStatus: Status = Status.NONE, serviceIds: Set<String> = emptySet()): Status =
			topStatus(silencedIds, minPriority, minStatus, serviceIds)?.status ?: Status.NONE

	fun topStatuses(silencedIds: Collection<String>, minPriority: Priority, minStatus: Status = Status.NONE, serviceIds: Set<String> = emptySet()) =
			filter(silencedIds, minPriority, minStatus, serviceIds)
					.filter { it.status == maxStatus(silencedIds, minPriority, minStatus, serviceIds) }
					.distinctBy { it.serviceId }

	fun topStatus(silencedIds: Collection<String>, minPriority: Priority, minStatus: Status = Status.NONE, serviceIds: Set<String> = emptySet()) =
			filter(silencedIds, minPriority, minStatus, serviceIds).maxBy { it.status }

	@Synchronized
	fun update(statusItems: Collection<StatusItem>, update: Boolean) {
		if (!update) statusItems.forEach { itemMap.remove(it.serviceId) }
		itemMap.putAll(statusItems.groupBy { it.serviceId })

		items = itemMap.values.flatten()
		LOGGER.info("Updating status: ${items}")
		status.onNext(this)
	}
}