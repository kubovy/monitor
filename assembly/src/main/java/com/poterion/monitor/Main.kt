package com.poterion.monitor

import com.poterion.monitor.control.Controller
import com.poterion.monitor.control.notifiers.Notifiers
import com.poterion.monitor.control.services.Services
import com.poterion.monitor.ui.TrayObject
import javafx.application.Application
import javafx.application.Platform
import javafx.stage.Stage

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class Main : Application() {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(Main::class.java, *args)
        }
    }

    override fun start(primaryStage: Stage) {
        Platform.setImplicitExit(false)
        val controller = Controller()
        controller.start()
        TrayObject.initialize(controller)
    }
}


