package com.poterion.monitor.api.controllers

import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.data.ModuleConfig
import javafx.scene.Node
import javafx.scene.Parent

interface ModuleInterface<out Config : ModuleConfig> {
	val icon: Icon
	val config: Config
	val controller: ControllerInterface
	val navigationRoot: NavigationItem?
		get() = null
	val configurationRows: List<Pair<Node, Node>>?
		get() = null
	val configurationAddition: List<Parent>?
		get() = null
	val configurationTab: Parent?
		get() = null

	fun initialize() {
	}
}