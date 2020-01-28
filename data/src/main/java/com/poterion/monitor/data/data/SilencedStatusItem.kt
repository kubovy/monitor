package com.poterion.monitor.data.data

import com.poterion.monitor.data.StatusItem
import java.time.Instant

data class SilencedStatusItem(var item: StatusItem = StatusItem(),
							  var silencedAt: Instant = Instant.now(),
							  val lastChange: Instant? = Instant.now(),
							  val untilChanged: Boolean = false)