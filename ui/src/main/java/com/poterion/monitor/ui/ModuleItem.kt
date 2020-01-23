package com.poterion.monitor.ui

import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.utils.javafx.Icon
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
data class ModuleItem(val title: StringProperty = SimpleStringProperty(),
					  val icon: Icon? = null,
					  val module: ModuleInstanceInterface<*>? = null)