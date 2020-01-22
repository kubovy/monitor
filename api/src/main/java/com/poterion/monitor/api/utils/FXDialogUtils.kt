package com.poterion.monitor.api.utils

import javafx.scene.control.Alert
import javafx.scene.control.ButtonType

fun confirm(title: String,
			content: String,
			header: String? = null,
			action: () -> Unit) {
	Alert(Alert.AlertType.CONFIRMATION)
			.also { alert ->
				alert.title = title
				alert.headerText = header ?: title
				alert.contentText = content
				alert.buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
			}
			.showAndWait()
			.ifPresent { it.takeIf { it == ButtonType.YES }?.also { action() } }
}