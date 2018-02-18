package com.poterion.monitor.api.controllers

import com.poterion.monitor.api.ui.Item

interface ModuleController {
	val controller: ControllerInterface
	val menuItem: Item?
}