package com.poterion.monitor.ui

import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.control.Controller
import com.poterion.monitor.control.Props
import com.poterion.monitor.control.notifiers.Notifiers
import com.poterion.monitor.control.open
import com.poterion.monitor.control.services.Services
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.key
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.data.notifiers.NotifierConfig
import com.poterion.monitor.data.notifiers.RaspiW2812Config
import com.poterion.monitor.data.services.JenkinsConfig
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.monitor.data.services.SonarConfig
import dorkbox.systemTray.*
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.stage.Modality
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
object TrayObject {
    private val LOGGER = LoggerFactory.getLogger("Main")
    private var systemTray: SystemTray = SystemTray.get() ?: throw RuntimeException("Unable to load SystemTray!")
    //private var trayIcon: TrayIcon? = null
    private var serviceMenus = mutableMapOf<String, Menu>()
    private var notifierMenus = mutableMapOf<String, Menu>()

    fun initialize(controller: Controller) {
        //SystemTray.SWING_UI = CustomSwingUI()
        try {
            Icon.INACTIVE.inputStream?.use { systemTray.setImage(it) }
            systemTray.status = "Monitor"
            createMenu(controller)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        //StatusCollector.statuses.subscribe(::update)
        StatusCollector.subscribeToStatusUpdate(::update)
    }

    private fun update(statusItems: Collection<StatusItem>) {
        statusItems.map { it.serviceName }.distinct()
                .map { it to serviceMenus[it] }
                .forEach { (serviceName, serviceMenu) ->
                    val menu = serviceMenu ?: Menu().apply {
                        text = serviceName
                        enabled = true
                        serviceMenus[serviceName] = this
                    }
                    menu.updateSubMenu(statusItems.filter { it.serviceName == serviceName })
                }

        statusItems
                .filter { it.priority > Priority.NONE }
                .maxBy { it.status }
                ?.toIcon()
                ?.inputStream
                ?.use { systemTray.setImage(it) }
    }

    private fun createMenu(controller: Controller) {
        try {
            systemTray.menu.apply {
                createServicesMenuItems(controller.config.services)
                createNotifiersMenuItems(controller.config.notifiers)

                add(MenuItem().apply {
                    text = "Refresh"
                    shortcut = 'r'
                    setCallback { Services.check(force = true) }
                })
                add(Separator())

                /*add(MenuItem().apply {
                    text = "Settings"
                    shortcut = 's'
                    setCallback { event -> }
                })*/

                add(MenuItem().apply {
                    text = "About"
                    shortcut = 'a'
                    setCallback {
                        Platform.runLater {
                            Alert(AlertType.INFORMATION).apply {
                                title = "About"
                                headerText = "${Props.APP_NAME}"
                                contentText = "Version: ${Props.VERSION}"
                                initModality(Modality.NONE)
                            }.showAndWait()
                        }
                    }
                })
                add(Separator())

                add(MenuItem().apply {
                    text = "Quit"
                    shortcut = 'q'
                    setCallback {
                        Platform.runLater { controller.quit() }
                        TrayObject.systemTray.shutdown()
                    }
                })
            }
        } catch (e: java.awt.HeadlessException) {
            LOGGER.error("Popup menu could not be created.", e)
        }
    }

    private fun Menu.createServicesMenuItems(services: List<ServiceConfig>) {
        services.sortedBy { it.order }.forEach { service ->
            add(Menu().apply {
                text = service.name
                Icon.UNKNOWN.inputStream?.use { setImage(it) }
                enabled = service.enabled

                when (service) {
                    is JenkinsConfig -> createSubMenu(service)
                    is SonarConfig -> createSubMenu(service)
                }

                serviceMenus[service.name] = this
            })
        }
        if (services.isNotEmpty()) add(Separator())
    }

    private fun Menu.createNotifiersMenuItems(notifiers: List<NotifierConfig>) {
        notifiers.sortedBy { it.name }.forEach { notifier ->
            add(Menu().apply {
                text = notifier.name
                //Icon.UNKNOWN.inputStream?.use { setImage(it) }
                enabled = notifier.enabled

                val enabled = Checkbox().apply {
                    text = "Enabled"
                    checked = notifier.enabled
                    setCallback {
                        notifier.enabled = checked
                        Notifiers[notifier.name]?.apply {
                            execute(if (notifier.enabled) NotifierAction.ENABLE else NotifierAction.DISABLE)
                            if (!notifier.enabled) execute(NotifierAction.SHUTDOWN)
                        }
                    }
                }
                add(enabled)

                when (notifier) {
                    is RaspiW2812Config -> createSubMenu(notifier, enabled)
                }

                notifierMenus[notifier.name] = this
            })

        }
        if (notifiers.isNotEmpty()) add(Separator())
    }

    private fun Menu.createSubMenu(jenkinsConfig: JenkinsConfig) = updateSubMenu(jenkinsConfig.jobs.map {
        StatusItem(
                serviceName = jenkinsConfig.name,
                priority = it.priority,
                status = Status.UNKNOWN,
                label = it.name,
                link = URI("${jenkinsConfig.url}job/${it.name}/"))
    })

    private fun Menu.createSubMenu(sonarConfig: SonarConfig) = updateSubMenu(sonarConfig.projects.map {
        StatusItem(
                serviceName = sonarConfig.name,
                priority = it.priority,
                status = Status.UNKNOWN,
                label = it.name,
                link = URI("${sonarConfig.url}dashboard/index/${it.id}"))
    })

    private fun Menu.createSubMenu(raspiW2812Config: RaspiW2812Config, enabledMenuItem: Checkbox) {
        raspiW2812Config.items.forEachIndexed { index, itemConfig ->
            if (index == 0) add(Separator())
            add(Menu().apply {
                text = itemConfig.id.takeIf { it.isNotEmpty() } ?: "Default"
                listOf("None" to itemConfig.statusNone,
                        "Unknown" to itemConfig.statusUnknown,
                        "OK" to itemConfig.statusOk,
                        "Info" to itemConfig.statusInfo,
                        "Notification" to itemConfig.statusNotification,
                        "Connection Error" to itemConfig.statusConnectionError,
                        "Service Error" to itemConfig.statusServiceError,
                        "Warning" to itemConfig.statusWarning,
                        "Error" to itemConfig.statusError,
                        "Fatal" to itemConfig.statusFatal)
                        .forEach { (label, lights) ->
                            add(MenuItem().apply {
                                text = label
                                setCallback {
                                    enabledMenuItem.checked = false
                                    Platform.runLater {
                                        Notifiers[raspiW2812Config.name]?.apply {
                                            execute(NotifierAction.DISABLE)
                                            execute(NotifierAction.NOTIFY, *lights.toTypedArray())
                                        }
                                    }
                                }
                            })
                        }
            })
        }
    }

    //private var menuEntries: MutableMap<Menu, MutableCollection<Entry>> = mutableMapOf()
    private var menuItems: MutableMap<String, MenuItem> = mutableMapOf()

    private fun Menu.updateSubMenu(statusItems: Collection<StatusItem>) {
        //while ((get(0) ?: first ?: last) != null) remove(get(0) ?: first ?: last)
        //menuEntries[this] = menuEntries[this]
        //        ?.also { it.forEach { remove(it) } }
        //        ?.also { it.clear() }
        //        ?: mutableListOf()

        var prioritised = true
        statusItems
                .sortedWith(compareByDescending(StatusItem::priority).thenBy(StatusItem::label))
                .forEachIndexed { index, statusItem ->
                    val menuItem = menuItems[statusItem.key()] ?: MenuItem().apply {
                        prioritised = separateNonePriorityItems(index, statusItem.priority, prioritised)
                        text = statusItem.label
                        setCallback { statusItem.link?.also { open(it) } }
                        //menuEntries[this@updateSubMenu]?.add(this)
                        menuItems[statusItem.key()] = this
                        this@updateSubMenu.add(this)
                    }
                    if (statusItem.priority == Priority.NONE) prioritised = false
                    statusItem.toIcon().inputStream.use { menuItem.setImage(it) }
                }

        statusItems
                .filter { it.priority > Priority.NONE }
                .maxBy { it.status }
                ?.toIcon()
                ?.inputStream
                ?.use { setImage(it) }
    }

    private fun Menu.separateNonePriorityItems(index: Int, priority: Priority, prioritised: Boolean): Boolean {
        if (priority == Priority.NONE && prioritised) {
            if (index > 0) {
                val separator = Separator()
                //menuEntries[this]?.add(separator)
                add(separator)
            }
            return false
        }
        return prioritised
    }
}