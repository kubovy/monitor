package com.poterion.monitor.notifiers.deploymentcase.api

import javafx.scene.input.KeyEvent

interface ConfigurationWindowActionListener {
    fun onUpload() {
    }

    fun onKeyPressed(keyEvent: KeyEvent): Unit? = null
}