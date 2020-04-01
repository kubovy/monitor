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
import com.poterion.monitor.api.Shared
import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.maxStatus
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.api.utils.toIcon
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.Priority
import com.poterion.monitor.notifiers.tray.SystemTrayIcon
import com.poterion.monitor.notifiers.tray.SystemTrayModule
import com.poterion.monitor.notifiers.tray.data.SystemTrayConfig
import com.poterion.monitor.ui.ConfigurationController
import com.poterion.utils.javafx.Icon
import com.poterion.utils.javafx.toImageView
import com.poterion.utils.kotlin.noop
import dorkbox.systemTray.*
import javafx.application.Platform
import javafx.collections.ListChangeListener
import javafx.scene.control.Alert
import javafx.stage.Modality
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
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
	private var menus = mutableMapOf<String?, MutableMap<String, Entry?>>()
	private var lastStatusIcon: Icon? = null

	override fun initialize() {
		super.initialize()
		try {
			LOGGER.info("Tray image size: ${systemTray?.trayImageSize}")
			CommonIcon.APPLICATION.inputStream.use { ImageIO.read(it) }.also { systemTray?.setImage(it) }
			systemTray?.status = "Poterion Monitor" +
					" ${controller.applicationConfiguration.version.takeIf { it != "0" }?.let { " (v${it})" } ?: ""}"
			createMenu()
		} catch (e: IOException) {
			LOGGER.error(e.message, e)
		}
		config.enabledProperty.addListener { _, _, enabled ->
			(if (enabled) lastStatusIcon else CommonIcon.APPLICATION)?.inputStream.use { systemTray?.setImage(it) }
		}
	}

	override fun destroy() {
		systemTray?.shutdown()
	}

	override val exitRequest: Boolean = false

	override fun update() {
		lastStatusIcon = StatusCollector.items.maxStatus(controller.applicationConfiguration.silencedMap.keys,
				config.minPriority, config.minStatus, config.services).toIcon()
		lastStatusIcon?.inputStream?.use { systemTray?.setImage(it) }
	}

	override fun shutdown() = noop()

	private fun addMenu(menu: Menu, root: String?, index: Int, navigationItem: NavigationItem, config: ModuleConfig) {
		val item = navigationItem.toMenu(config)
		menus.getOrPut(root) { mutableMapOf() }[config.uuid] = item
		if (index < 0) menu.add(item) else menu.add(item, index)
	}

	private fun updateMenu(menu: Menu, part: Int, change: ListChangeListener.Change<out ModuleInstanceInterface<ModuleConfig>>) {
		while (change.next()) when {
			change.wasRemoved() -> change.removed.forEach { module ->
				menus[null]?.get(module.config.uuid)?.also { menu.remove(it) }
				menus[null]?.remove(module.config.uuid)
			}
			change.wasAdded() -> change.addedSubList.forEach { module ->
				module.navigationRoot?.also { navigationItem ->
					var i = 0
					(0 until part).forEach { _ ->
						while (menu.get(i) !is Separator) i++
						i++
					}
					while (menu.get(i) != menu.last
							&& menu.get(i) !is Separator
							&& module.config.name > (menu.get(i) as? MenuItem)?.text ?: "") i++
					addMenu(menu, null, i, navigationItem, module.config)
				}
			}
		}
	}

	private fun updateMenu(menu: Menu, change: ListChangeListener.Change<out NavigationItem>, moduleConfig: ModuleConfig) {
		while (change.next()) when {
			change.wasRemoved() -> change.removed.forEach { navigationItem ->
				menus[moduleConfig.uuid]?.get(navigationItem.uuid)?.also { menu.remove(it) }
				menus[moduleConfig.uuid]?.remove(navigationItem.uuid)
			}
			change.wasAdded() -> change.addedSubList.forEach { navigationItem ->
				var i = 0
				while (menu.get(i) != menu.last
						&& menu.get(i) !is Separator
						&& (navigationRoot.title ?: "") > (menu.get(i) as? MenuItem)?.text ?: "") i++
				addMenu(menu, moduleConfig.uuid, i, navigationItem, moduleConfig)
			}
		}
	}

	private fun createMenu() {
		if (systemTray != null && (systemTray?.menu?.first == null)) try {
			systemTray?.menu?.also { menu ->
				controller.services.sortedBy { it.config.name }
						.forEach { service -> addMenu(menu, null, -1, service.navigationRoot, service.config) }
				controller.services.addListener(ListChangeListener { change -> updateMenu(menu, 0, change) })

				menu.add(Separator())

				controller.notifiers.sortedBy { it.config.name }
						.forEach { notifier -> addMenu(menu, null, -1, notifier.navigationRoot, notifier.config) }
				controller.notifiers.addListener(ListChangeListener { change -> updateMenu(menu, 1, change) })

				menu.add(Separator())
				menu.add(Checkbox("Pause").apply {
					shortcut = 'p'
					checked = controller.applicationConfiguration.paused
					controller.applicationConfiguration.pausedProperty.addListener { _, _, paused -> checked = paused }
					setCallback { Platform.runLater { controller.applicationConfiguration.paused = checked } }
				})
				menu.add(MenuItem("Refresh").apply {
					shortcut = 'r'
					SystemTrayIcon.REFRESH.inputStream.use { setImage(it) }
					setCallback {
						Platform.runLater {
							controller.services.filter { it.config.enabled }.forEach { it.refresh = true }
						}
					}
				})
				menu.add(Separator())
				menu.add(MenuItem("Settings").apply {
					shortcut = 's'
					CommonIcon.SETTINGS.inputStream.use { setImage(it) }
					setCallback { Platform.runLater { ConfigurationController.create(controller) } }
				})
				menu.add(MenuItem("About").apply {
					shortcut = 'a'
					SystemTrayIcon.ABOUT.inputStream.use { setImage(it) }
					setCallback { _ ->
						Platform.runLater {
							Alert(Alert.AlertType.INFORMATION).apply {
								graphic = CommonIcon.APPLICATION.toImageView()
								title = "About"
								headerText = "Poterion Monitor"
								contentText = "Version: ${Shared.properties.getProperty("version", "0")}\n" +
										"Author: Jan Kubovy (jan@kubovy.eu)\n" +
										"Icons: Icon8 (https://icons8.com)"
								initModality(Modality.NONE)
							}.showAndWait()
						}
					}
				})
				menu.add(Separator())
				menu.add(MenuItem("Quit").apply {
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

	private fun NavigationItem.toMenu(moduleConfig: ModuleConfig): Entry? {
		return if (title != null && sub != null) { // Menu
			Menu(title).also { menu ->
				titleProperty.addListener { _, _, value -> menu.text = value }
				icon?.also { icon -> icon.inputStream.use { menu.setImage(it) } }
				sub?.forEach { subItem -> addMenu(menu, moduleConfig.uuid, -1, subItem, moduleConfig) }
				sub?.addListener(ListChangeListener { change -> updateMenu(menu, change, moduleConfig) })
			}
		} else if (title != null && sub == null && !isCheckable) { // Menu Item
			MenuItem(title) { Platform.runLater { action?.invoke() } }.also { menuItem ->
				titleProperty.addListener { _, _, value -> menuItem.text = value }
				icon?.also { icon -> icon.inputStream.use { menuItem.setImage(it) } }
				menuItem.enabled = enabled
			}
		} else if (title != null && sub == null && isCheckable) { // Checkbox
			Checkbox(title).also { checkbox ->
				titleProperty.addListener { _, _, value -> checkbox.text = value }
				checkbox.enabled = enabled
				checkbox.checked = checked ?: false
				checkbox.setCallback {
					Platform.runLater {
						checked = checkbox.checked
						action?.invoke()
					}
				}
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