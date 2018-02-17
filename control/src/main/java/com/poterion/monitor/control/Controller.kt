package com.poterion.monitor.control

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.poterion.monitor.control.notifiers.Notifiers
import com.poterion.monitor.control.services.Services
import com.poterion.monitor.data.Config
import javafx.application.Platform
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Executors

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class Controller(configFileName: String = "config.yaml") {
    companion object {
        val LOGGER = LoggerFactory.getLogger(Controller::class.java)
    }

    private val configFile = File(configFileName)
    private val executor = Executors.newSingleThreadExecutor()
    private var worker: ControllerWorker? = null
    private val mapper = ObjectMapper(YAMLFactory())

    var config: Config = Config()
        private set(value) {
            Services.configure(value)
            Notifiers.configure(value)
            field = value
        }

    init {
        config = mapper.readValue(configFile, Config::class.java)
    }

    fun start() {
        worker?.stop()
        worker = ControllerWorker()
        executor.submit(worker)
    }

    fun run(body: () -> Unit) {
        if (Platform.isFxApplicationThread()) body.invoke()
        else Platform.runLater(body)
    }

    fun quit() {
        executor.shutdown()
        worker?.stop()
        executor.shutdownNow()
        Notifiers.shutDown()
        saveConfig()
        System.exit(0) // not necessary if all non-daemon threads have stopped.
    }

    fun saveConfig() {
        try {
            mapper.writeValue(configFile, config)
        } catch (e: Exception) {
            LOGGER.error(e.message, e)
        }
    }
}