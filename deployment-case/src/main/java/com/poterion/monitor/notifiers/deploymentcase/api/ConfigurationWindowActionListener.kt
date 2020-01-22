package com.poterion.monitor.notifiers.deploymentcase.api

import com.poterion.monitor.api.utils.noop
import javafx.scene.input.KeyEvent

interface ConfigurationWindowActionListener {
	fun onUpload() = noop()

	fun onKeyPressed(keyEvent: KeyEvent): Unit? = null
}