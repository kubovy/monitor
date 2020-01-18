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

	fun maxStatus(minPriority: Priority): Status = topStatus(minPriority)
			?.status
			?: Status.NONE

	fun topStatuses(minPriority: Priority) = items
			.filter { it.priority >= minPriority }
			.filter { it.status == maxStatus(minPriority) }
			.distinctBy { it.serviceId }

	fun topStatus(minPriority: Priority) = items
			.filter { it.priority >= minPriority }
			.maxBy { it.status }

	@Synchronized
	fun update(items: Collection<StatusItem>, update: Boolean) {
		if (!update) items.forEach { itemMap.remove(it.serviceId) }
		itemMap.putAll(items.groupBy { it.serviceId })

		this.items = itemMap.values.flatten()
		LOGGER.info("Updating status: ${this.items}")
		status.onNext(this)
	}
}