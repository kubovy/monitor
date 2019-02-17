package com.poterion.monitor.notifiers.deploymentcase.ui

import javafx.scene.control.TreeItem

fun TreeItem<*>.expandAll() {
	if (!isLeaf) {
		isExpanded = true
		for (child in children) child?.expandAll()
	}
}