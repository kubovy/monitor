package com.poterion.monitor.api

import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.key
import org.slf4j.LoggerFactory

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
object StatusCollector {
	private val LOGGER = LoggerFactory.getLogger(StatusCollector::class.java)
	private val statusItems = mutableMapOf<String, StatusItem>()
	private val statusObservers = mutableListOf<(Collection<StatusItem>) -> Unit>()
	private val worstObservers = mutableListOf<(StatusItem) -> Unit>()

	fun subscribeToStatusUpdate(observer: (Collection<StatusItem>) -> Unit) = statusObservers.add(observer)
	fun subscribeToWorstUpdate(observer: (StatusItem) -> Unit) = worstObservers.add(observer)

	fun update(items: Collection<StatusItem>) {
		statusItems.putAll(items.map { it.key() to it })

		statusItems.values.also { statusItems ->
			LOGGER.info("Updating status: ${statusItems}")
			statusObservers.forEach { it.invoke(statusItems) }
			val worstStatus = statusItems.map { it.status }.max()
			statusItems
					.filter { it.priority > Priority.NONE }
					.filter { it.status == worstStatus }
					.sortedWith(compareByDescending(StatusItem::priority))
					.firstOrNull()
					?.also { statusItem ->
						LOGGER.info("Updating worst: ${statusItem}")
						//worst.onNext(it)
						worstObservers.forEach { it.invoke(statusItem) }
					}
		}
	}
}