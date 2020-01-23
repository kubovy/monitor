package com.poterion.monitor.api.ui

import com.poterion.utils.javafx.Icon
import com.poterion.utils.javafx.toImageView
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Region

class CollectionSettingsPlugin<T>(
		items: List<T>,
		private val value: T.() -> String,
		private val setter: T.(String) -> Unit,
		subject: String? = null,
		private val title: T.() -> String = { toString() },
		private val promptText: String = "",
		private val suffix: String? = null,
		private val width: Double = Region.USE_COMPUTED_SIZE,
		private val maxWidth: Double = Double.MAX_VALUE,
		private val icon: T.() -> Icon? = { null }) {

	private fun T.createLabel() = this.icon()
			?.toImageView(24, 24)
			?.let { imageView ->
				HBox(imageView, Label(title()).apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					padding = Insets(5.0)
				})
			}
			?.apply { spacing = 5.0 }
			?: Label(title()).apply {
				maxWidth = Double.MAX_VALUE
				maxHeight = Double.MAX_VALUE
				alignment = Pos.CENTER_RIGHT
				padding = Insets(5.0)
			}

	private fun T.createControl() = (
			TextField(value())
					.apply {
						prefWidth = this@CollectionSettingsPlugin.width
						maxWidth = this@CollectionSettingsPlugin.maxWidth
						promptText = this@CollectionSettingsPlugin.promptText
						focusedProperty().addListener { _, _, focused ->
							if (!focused) setter(text)
						}
					} to suffix?.let { Label(it) }?.apply { maxHeight = Double.MAX_VALUE })
			.let { (field, suffix) -> if (suffix == null) field else HBox(field, suffix).apply { spacing = 5.0 } }

	val rowItems: List<Pair<Node, Node>> = listOfNotNull(
			subject?.let { Label(it) }
					?.apply {
						maxWidth = Double.MAX_VALUE
						maxHeight = Double.MAX_VALUE
						alignment = Pos.CENTER_RIGHT
						padding = Insets(5.0)
					}
					?.let { it to Pane() },
			*items.map { item -> item.createLabel() to item.createControl() }.toTypedArray())
}