package com.poterion.monitor.ui

import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.ui.Item
import com.poterion.monitor.control.Controller
import com.poterion.monitor.control.Props
import com.poterion.monitor.control.open
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.key
import dorkbox.systemTray.*
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.stage.Modality
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
object TrayObject {
    private val LOGGER = LoggerFactory.getLogger("Main")
    private var systemTray: SystemTray = SystemTray.get() ?: throw RuntimeException("Unable to load SystemTray!")
    //private var trayIcon: TrayIcon? = null
    private var serviceMenus = mutableMapOf<String, Menu>()

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
                createServicesMenuItems(controller)
                createNotifiersMenuItems(controller)

                add(MenuItem().apply {
                    text = "Refresh"
                    shortcut = 'r'
                    setCallback { Platform.runLater { controller.check(force = true) } }
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

    private fun Menu.createServicesMenuItems(controller: Controller) {
        controller.serviceControllers.sortedBy { it.config.order }.forEach { service ->
            add(service.menuItem.toMenu())
        }
        if (controller.serviceControllers.isNotEmpty()) add(Separator())
    }

    private fun Menu.createNotifiersMenuItems(controller: Controller) {
        controller.notifierController.sortedBy { it.config.name }.forEach { notifier ->
            add(notifier.menuItem.toMenu())
        }
        if (controller.notifierController.isNotEmpty()) add(Separator())
    }

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

    private fun Item.toMenu(): Entry? {
        return if (title != null && sub != null) { // Menu
            Menu(title).also { menu ->
                sub?.forEach { subItem -> menu.add(subItem.toMenu()) }
            }
        } else if (title != null && sub == null && checked == null) { // Menu Item
            MenuItem(title, { action?.invoke() }).also {
                it.enabled = enabled
            }
        } else if (title != null && sub == null && checked != null) { // Checkbox
            Checkbox(title).also {
                it.enabled = enabled
                it.checked = checked ?: false
                it.setCallback { action?.invoke() }
            }
        } else if (title == null){ // Separator
            Separator()
        } else null
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