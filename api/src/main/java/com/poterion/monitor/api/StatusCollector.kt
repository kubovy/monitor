package com.poterion.monitor.api

import com.fasterxml.jackson.core.type.TypeReference
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import io.reactivex.subjects.PublishSubject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit


/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
object StatusCollector {
	private val LOGGER: Logger = LoggerFactory.getLogger(StatusCollector::class.java)

	private val itemMap = mutableMapOf<String, Collection<StatusItem>>()
	var items = emptyList<StatusItem>()
		private set
	val status: PublishSubject<StatusCollector> = PublishSubject.create<StatusCollector>()

	init {
		if (Shared.cacheFile.exists()) {
			val statusItems = objectMapper
					.readValue(Shared.cacheFile, object: TypeReference<List<StatusItem?>?>() {})
					?.filterNotNull()
			if (statusItems != null) update(statusItems, false)
		}

		status.sample(10, TimeUnit.SECONDS, true).subscribe { collector ->
			val backupFile = File(Shared.cacheFile.absolutePath + "-" + LocalDateTime.now().toString())
			try {
				val tempFile = File(Shared.cacheFile.absolutePath + ".tmp")
				var success = tempFile.parentFile.exists() || tempFile.parentFile.mkdirs()
				objectMapper.writeValue(tempFile, collector.items)
				success = success
						&& (!backupFile.exists() || backupFile.delete())
						&& (Shared.cacheFile.parentFile.exists() || Shared.cacheFile.parentFile.mkdirs())
						&& (!Shared.cacheFile.exists() || Shared.cacheFile.renameTo(backupFile))
						&& tempFile.renameTo(Shared.cacheFile.absoluteFile)
				if (success) backupFile.delete()
				else LOGGER.error("Failed saving status items to ${Shared.cacheFile.absolutePath}"
						+ " (backup ${backupFile})")
			} catch (e: Exception) {
				LOGGER.error(e.message, e)
			} finally {
				if (!Shared.cacheFile.exists() && backupFile.exists() && !backupFile.renameTo(Shared.cacheFile)) {
					LOGGER.error("Restoring ${backupFile} failed!")
				}
			}
		}
	}

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