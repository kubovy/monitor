package com.poterion.monitor.ui

import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.scene.Node

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class ModuleItem(val title: StringProperty = SimpleStringProperty(),
					  val graphic: Node? = null,
					  val module: ModuleInstanceInterface<*>? = null)