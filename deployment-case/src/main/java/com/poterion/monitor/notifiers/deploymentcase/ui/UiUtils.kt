package com.poterion.monitor.notifiers.deploymentcase.ui

import javafx.scene.control.TreeItem
import javafx.scene.paint.Color

fun TreeItem<*>.expandAll() {
	if (!isLeaf) {
		isExpanded = true
		for (child in children) child?.expandAll()
	}
}

fun Int.adjustColor(maxColorComponent: Int) = (255.0 / maxColorComponent.toDouble() * this.toDouble()).toInt()

fun Color.adjust(maxColorComponent: Int) = Color.rgb(
		(red * 255.0).toInt().adjustColor(maxColorComponent),
		(green * 255.0).toInt().adjustColor(maxColorComponent),
		(blue * 255.0).toInt().adjustColor(maxColorComponent))