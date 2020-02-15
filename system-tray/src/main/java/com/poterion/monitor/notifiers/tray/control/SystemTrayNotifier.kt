/******************************************************************************
 * Copyright (C) 2020 Jan Kubovy (jan@kubovy.eu)                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.      *
 ******************************************************************************/
package com.poterion.monitor.notifiers.tray.control

import com.poterion.monitor.api.CommonIcon
import com.poterion.monitor.api.Props
import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.api.utils.toIcon
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.notifiers.tray.SystemTrayIcon
import com.poterion.monitor.notifiers.tray.SystemTrayModule
import com.poterion.monitor.notifiers.tray.data.SystemTrayConfig
import com.poterion.monitor.ui.ConfigurationController
import com.poterion.utils.javafx.Icon
import com.poterion.utils.javafx.openInExternalApplication
import com.poterion.utils.javafx.toImageView
import dorkbox.systemTray.*
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.stage.Modality
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class SystemTrayNotifier(override val controller: ControllerInterface, config: SystemTrayConfig) : Notifier<SystemTrayConfig>(config) {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(SystemTrayNotifier::class.java)
	}

	override val definition: Module<SystemTrayConfig, ModuleInstanceInterface<SystemTrayConfig>> = SystemTrayModule
	private var systemTray: SystemTray? = SystemTray.get()
	private var serviceMenus = mutableMapOf<String, Menu>()
	private var lastStatusIcon: Icon? = null

	override fun initialize() {
		super.initialize()
		try {
			LOGGER.info("Tray image size: ${systemTray?.trayImageSize}")
			CommonIcon.APPLICATION.inputStream.use { ImageIO.read(it) }.also { systemTray?.setImage(it) }
			systemTray?.status = "Monitor"
			createMenu()
			controller.registerForConfigUpdates { createMenu() }
		} catch (e: IOException) {
			LOGGER.error(e.message, e)
		}
		StatusCollector.status.sample(10, TimeUnit.SECONDS, true).subscribe {
			Platform.runLater { update(it) }
		}
	}

	override fun destroy() {
		systemTray?.shutdown()
	}

	override val exitRequest: Boolean = false

	override val configurationRows: List<Pair<Node, Node>>
		get() = super.configurationRows + listOf(
				Label("Refresh").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to CheckBox().apply {
					maxHeight = Double.MAX_VALUE
					selectedProperty().addListener { _, _, value ->
						config.refresh = value
						controller.saveConfig()
					}
				})

	override fun execute(action: NotifierAction): Unit = when (action) {
		NotifierAction.ENABLE -> {
			config.enabled = true
			lastStatusIcon?.inputStream.use { systemTray?.setImage(it) }
			controller.saveConfig()
		}
		NotifierAction.DISABLE -> {
			config.enabled = false
			CommonIcon.APPLICATION.inputStream.use { systemTray?.setImage(it) }
			controller.saveConfig()
		}
		NotifierAction.TOGGLE -> execute(if (config.enabled) NotifierAction.DISABLE else NotifierAction.ENABLE)
		else -> LOGGER.debug("Executing action ${action}")
	}

	private fun update(collector: StatusCollector) {
		collector.items
				.filterNot { controller.applicationConfiguration.silenced.keys.contains(it.id) }
				.map { it.serviceId }
				.distinct()
				.map { it to serviceMenus[it] }
				.forEach { (serviceId, serviceMenu) ->
					serviceMenu
							?.also { it.updateSubMenu(collector.items.filter { item -> item.serviceId == serviceId }) }
							?: LOGGER.error("Unknown service ${serviceId} - no menu for it")
				}

		lastStatusIcon = collector.maxStatus(controller.applicationConfiguration.silenced.keys, config.minPriority,
				config.minStatus, config.services).toIcon()
		lastStatusIcon?.inputStream?.use { systemTray?.setImage(it) }
	}

	private fun createMenu() {
		if (systemTray != null && (systemTray?.menu?.first == null || config.refresh)) try {
			while (systemTray?.menu?.first != null) systemTray?.menu?.first?.also { systemTray?.menu?.remove(it) }
			systemTray?.menu?.apply {
				controller.services.sortedBy { it.config.order }.forEach { service ->
					val menu = service.navigationRoot.toMenu(controller, service.config)
					if (menu is Menu) {
						serviceMenus[service.config.name] = menu
						add(menu)
					}
				}
				if (controller.services.isNotEmpty()) add(Separator())

				controller.notifiers.sortedBy { it.config.name }.forEach { notifier ->
					add(notifier.navigationRoot.toMenu(controller, notifier.config))
				}
				if (controller.notifiers.isNotEmpty()) add(Separator())

				//add(MenuItem().apply {
				//	text = "Refresh"
				//	shortcut = 'r'
				//	SystemTrayIcon.REFRESH.inputStream.use { setImage(it) }
				//	setCallback { Platform.runLater { controller.check(force = true) } }
				//})
				add(Separator())

				add(MenuItem().apply {
					text = "Settings"
					shortcut = 's'
					CommonIcon.SETTINGS.inputStream.use { setImage(it) }
					setCallback { Platform.runLater { ConfigurationController.create(controller) } }
				})

				add(MenuItem().apply {
					text = "About"
					shortcut = 'a'
					SystemTrayIcon.ABOUT.inputStream.use { setImage(it) }
					setCallback { _ ->
						Platform.runLater {
							Alert(Alert.AlertType.INFORMATION).apply {
								graphic = CommonIcon.APPLICATION.toImageView()
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
					SystemTrayIcon.QUIT.inputStream.use { setImage(it) }
					setCallback {
						Platform.runLater { controller.quit() }
						systemTray.shutdown()
					}
				})
			}
		} catch (e: java.awt.HeadlessException) {
			LOGGER.error("Popup menu could not be created.", e)
		}
	}

	private var menuItems: MutableMap<String, MenuItem> = mutableMapOf()

	private fun Menu.updateSubMenu(statusItems: Collection<StatusItem>) {
		var prioritised = true
		statusItems
				.sortedWith(compareByDescending(StatusItem::priority).thenBy(StatusItem::title))
				.forEachIndexed { index, statusItem ->
					val menuItem = menuItems[statusItem.id] ?: MenuItem().apply {
						prioritised = separateNonePriorityItems(index, statusItem.priority, prioritised)
						text = statusItem.title
						setCallback { _ ->
							statusItem.link
									?.let {
										try {
											URI(it)
										} catch (e: URISyntaxException) {
											LOGGER.warn(e.message, e)
											null
										}
									}
									?.openInExternalApplication()
						}
						//menuEntries[this@updateSubMenu]?.add(this)
						menuItems[statusItem.id] = this
						this@updateSubMenu.add(this)
					}
					if (statusItem.priority == Priority.NONE) prioritised = false
					statusItem.status.toIcon().inputStream.use { menuItem.setImage(it) }
				}

		val icon = statusItems
				.filter { it.priority > Priority.NONE }
				.maxBy { it.status }
				?.status
				?.toIcon() ?: CommonIcon.STATUS_OK
		icon.inputStream.use { setImage(it) }
	}

	private fun NavigationItem.toMenu(controller: ControllerInterface, moduleConfig: ModuleConfig): Entry? {
		return if (title != null && sub != null) { // Menu
			Menu(title).also { menu ->
				icon?.also { icon -> icon.inputStream.use { menu.setImage(it) } }
				sub?.forEach { subItem -> menu.add(subItem.toMenu(controller, moduleConfig)) }
			}
		} else if (title != null && sub == null && checked == null) { // Menu Item
			MenuItem(title) { Platform.runLater { action?.invoke() } }.also { menuItem ->
				icon?.also { icon -> icon.inputStream.use { menuItem.setImage(it) } }
				menuItem.enabled = enabled
			}
		} else if (title != null && sub == null && checked != null) { // Checkbox
			Checkbox(title).also { checkbox ->
				checkbox.enabled = enabled
				checkbox.checked = checked ?: false
				checkbox.setCallback { Platform.runLater { action?.invoke() } }
			}
		} else if (title == null) { // Separator
			Separator()
		} else null
	}

	private fun Menu.separateNonePriorityItems(index: Int, priority: Priority, prioritised: Boolean): Boolean {
		if (priority == Priority.NONE && prioritised) {
			if (index > 0) add(Separator())
			return false
		}
		return prioritised
	}
}