package com.poterion.monitor.api.controllers

import com.poterion.monitor.data.ModuleConfig
import com.poterion.utils.javafx.toImageView
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.Node

abstract class AbstractModule<out Config : ModuleConfig>(final override val config: Config) : ModuleInstanceInterface<Config> {
	final override val configurationTabIcon: ObjectProperty<Node?> = SimpleObjectProperty(null)

	override fun initialize() {
		super.initialize()
		configurationTabIcon.set(definition.icon.toImageView())
	}
}