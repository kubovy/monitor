package com.poterion.monitor.notifiers.tray.control

import com.poterion.monitor.api.Props
import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.lib.open
import com.poterion.monitor.api.lib.toIcon
import com.poterion.monitor.api.lib.toImageView
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.CommonIcon
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.key
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.notifiers.tray.SystemTrayIcon
import com.poterion.monitor.notifiers.tray.SystemTrayModule
import com.poterion.monitor.notifiers.tray.data.SystemTrayConfig
import com.poterion.monitor.ui.ConfigurationController
import dorkbox.systemTray.*
import javafx.application.Platform
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
import javax.imageio.ImageIO

/**
 * @author Jan Kubovy <jan@kubovy.eu>
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
		try {
			LOGGER.info("Tray image size: ${systemTray?.trayImageSize}")
			CommonIcon.APPLICATION.inputStream.use { ImageIO.read(it) }.also { systemTray?.setImage(it) }
			systemTray?.status = "Monitor"
			createMenu()
			controller.registerForConfigUpdates { createMenu() }
		} catch (e: IOException) {
			LOGGER.error(e.message, e)
		}
		StatusCollector.status.subscribe(::update)
	}

	override fun destroy() {
		systemTray?.shutdown()
	}

	override val exitRequest: Boolean = false

	override val configurationRows: List<Pair<Node, Node>>?
		get() = listOf(Label("Refresh") to CheckBox().apply {
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

	private fun update(statusItems: Collection<StatusItem>) {
		statusItems.map { it.serviceName }
				.distinct()
				.map { it to serviceMenus[it] }
				.forEach { (serviceName, serviceMenu) ->
					serviceMenu
							?.also { it.updateSubMenu(statusItems.filter { item -> item.serviceName == serviceName }) }
							?: LOGGER.error("Unknown service ${serviceName} - no menu for it")
				}

		statusItems
				.filter { it.priority >= config.minPriority }
				.maxBy { it.status }
				?.toIcon()
				?.also { lastStatusIcon = it }

		lastStatusIcon
				?.inputStream
				?.use { systemTray?.setImage(it) }
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

				add(MenuItem().apply {
					text = "Refresh"
					shortcut = 'r'
					SystemTrayIcon.REFRESH.inputStream.use { setImage(it) }
					setCallback { Platform.runLater { controller.check(force = true) } }
				})
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
				.sortedWith(compareByDescending(StatusItem::priority).thenBy(StatusItem::label))
				.forEachIndexed { index, statusItem ->
					val menuItem = menuItems[statusItem.key()] ?: MenuItem().apply {
						prioritised = separateNonePriorityItems(index, statusItem.priority, prioritised)
						text = statusItem.label
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
									?.also { open(it) }
						}
						//menuEntries[this@updateSubMenu]?.add(this)
						menuItems[statusItem.key()] = this
						this@updateSubMenu.add(this)
					}
					if (statusItem.priority == Priority.NONE) prioritised = false
					statusItem.toIcon().inputStream.use { menuItem.setImage(it) }
				}

		val icon = statusItems
				.filter { it.priority > Priority.NONE }
				.maxBy { it.status }
				?.toIcon() ?: CommonIcon.OK
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