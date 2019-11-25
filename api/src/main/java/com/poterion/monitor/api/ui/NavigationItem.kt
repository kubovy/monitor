package com.poterion.monitor.api.ui

import com.poterion.monitor.data.ApplicationConfiguration
import dorkbox.systemTray.Entry

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class NavigationItem(val title: String? = null,
						  val icon: Icon? = null,
						  val enabled: Boolean = true,
						  val checked: Boolean? = null,
						  val update: ((Entry, ApplicationConfiguration) -> Unit)? = null,
						  var action: (() -> Unit)? = null,
						  val sub: MutableList<NavigationItem>? = null)