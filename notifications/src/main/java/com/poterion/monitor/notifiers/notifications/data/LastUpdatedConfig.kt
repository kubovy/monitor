package com.poterion.monitor.notifiers.notifications.data

import com.poterion.monitor.data.Status
import java.time.Instant

data class LastUpdatedConfig(var status: Status? = null,
							 var startedAt: Long? = null,
							 var updatedAt: Long = Instant.now().toEpochMilli())