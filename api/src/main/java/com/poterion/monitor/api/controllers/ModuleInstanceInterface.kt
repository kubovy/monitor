package com.poterion.monitor.api.controllers

import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.data.ModuleConfig
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
	val configurationRows: List<Pair<Node, Node>>?
		get() = null

	/** Additional configuration if not fitting the rows pattern. */
	val configurationAddition: List<Parent>?
		get() = null

	/** Configuration rows for the module if any. */
	val configurationTab: Parent?
		get() = null

	val exitRequest: Boolean
		get() = true

	/** Initialization of a module controller. Call after controllers are created. */
	fun initialize() {
	}

	fun destroy() {
	}
}