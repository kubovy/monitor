package com.poterion.monitor.api.controllers

import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.api.utils.noop
import com.poterion.monitor.data.ModuleConfig
import javafx.beans.property.ObjectProperty
import javafx.scene.Node
import javafx.scene.Parent

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface ModuleInstanceInterface<out Config : ModuleConfig> {
	/** Module definition singleton */
	val definition: Module<Config, ModuleInstanceInterface<Config>>

	/** Module configuration model */
	val config: Config

	/** UI controller */
	val controller: ControllerInterface

	/** Navigation root of the sub menu in the tray */
	val navigationRoot: NavigationItem?
		get() = null

	/** Configuration rows for the module if any. */
	val configurationRows: List<Pair<Node, Node>>
		get() = emptyList()

	/** Configuration rows for the module if any. */
	val configurationRowsLast: List<Pair<Node, Node>>
		get() = emptyList()

	/** Additional configuration if not fitting the rows pattern. */
	val configurationAddition: List<Parent>
		get() = emptyList()

	/** Own tab for the module if any. */
	val configurationTab: Parent?
		get() = null

	val configurationTabIcon: ObjectProperty<Node?>

	val exitRequest: Boolean
		get() = true

	/** Initialization of a module controller. Call after controllers are created. */
	fun initialize() = noop()

	fun destroy() = noop()
}