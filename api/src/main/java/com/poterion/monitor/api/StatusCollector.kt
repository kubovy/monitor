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
package com.poterion.monitor.api

import com.fasterxml.jackson.core.type.TypeReference
import com.poterion.monitor.data.StatusItem
import io.reactivex.subjects.BehaviorSubject
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
	var items: List<StatusItem> = emptyList()
		private set
	val status: BehaviorSubject<StatusCollector> = BehaviorSubject.create()

	init {
		if (Shared.cacheFile.exists()) {
			val statusItems = objectMapper
				.readValue(Shared.cacheFile, object : TypeReference<List<StatusItem?>?>() {})
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

	@Synchronized
	fun update(statusItems: Collection<StatusItem>, update: Boolean) {
		if (!update) statusItems.map { it.serviceId }.distinct().forEach { itemMap.remove(it) }
		val deduplicated = statusItems
			.groupBy { it.id }
			.mapNotNull { (_, duplicates) -> duplicates.maxByOrNull { it.status.ordinal * 100 + it.priority.ordinal } }
				.groupBy { it.serviceId }
		itemMap.putAll(deduplicated)

		items = itemMap.values.flatten()
		status.onNext(this)
	}
}