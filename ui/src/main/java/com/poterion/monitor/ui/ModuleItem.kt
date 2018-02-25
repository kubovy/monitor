package com.poterion.monitor.ui

import com.poterion.monitor.api.controllers.ModuleInterface
import javafx.scene.Node

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class ModuleItem(val title: String? = null,
					  val graphic: Node? = null,
					  val module: ModuleInterface<*>? = null)