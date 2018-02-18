package com.poterion.monitor.notifiers.raspiw2812.data

import com.poterion.monitor.ui.Icon

data class StateConfig(val title: String,
					   val icon: Icon? = null,
					   var lightConigs: List<String>? = null)