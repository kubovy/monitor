package com.poterion.monitor.api

import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.key
import io.reactivex.subjects.PublishSubject
import org.slf4j.LoggerFactory

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
object StatusCollector {
	private val LOGGER = LoggerFactory.getLogger(StatusCollector::class.java)
	private val statusItems = mutableMapOf<String, StatusItem>()
	val status: PublishSubject<Collection<StatusItem>> = PublishSubject.create<Collection<StatusItem>>()

	@Synchronized
	fun update(items: Collection<StatusItem>) {
		statusItems.putAll(items.map { it.key() to it })

		statusItems.values.also { statusItems ->
			LOGGER.info("Updating status: ${statusItems}")
			status.onNext(statusItems)
		}
	}
}