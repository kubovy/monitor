package com.poterion.monitor.api.ui

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class NavigationItem(val title: String? = null,
						  val icon: Icon? = null,
						  val enabled: Boolean = true,
						  val checked: Boolean? = null,
						  val recreate: (() -> List<NavigationItem>)? = null,
						  var action: (() -> Unit)? = null,
						  val sub: MutableList<NavigationItem>? = null)