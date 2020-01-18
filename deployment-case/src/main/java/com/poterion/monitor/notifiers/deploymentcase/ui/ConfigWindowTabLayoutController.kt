package com.poterion.monitor.notifiers.deploymentcase.ui

import com.poterion.monitor.notifiers.deploymentcase.api.DeploymentCaseMessageListener
import com.poterion.monitor.notifiers.deploymentcase.control.DeploymentCaseNotifier
import com.poterion.monitor.notifiers.deploymentcase.data.*
import javafx.collections.ListChangeListener
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ConfigWindowTabLayoutController : DeploymentCaseMessageListener {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(ConfigWindowTabLayoutController::class.java)

        internal fun getRoot(notifier: DeploymentCaseNotifier): Pair<ConfigWindowTabLayoutController, Parent> =
                FXMLLoader(ConfigWindowTabLayoutController::class.java.getResource("config-window-tab-layout.fxml"))
                        .let { it.load<Parent>() to it.getController<ConfigWindowTabLayoutController>() }
                        .let { (root, ctrl) ->
                            ctrl.notifier = notifier
                            ctrl to root
                        }
    }

    @FXML private lateinit var lcd: TextArea
    @FXML private lateinit var led0: Circle
    @FXML private lateinit var led1: Circle
    @FXML private lateinit var led2: Circle
    @FXML private lateinit var led3: Circle
    @FXML private lateinit var led4: Circle
    @FXML private lateinit var rgb00: Circle
    @FXML private lateinit var rgb01: Circle
    @FXML private lateinit var rgb02: Circle
    @FXML private lateinit var rgb03: Circle
    @FXML private lateinit var rgb04: Circle
    @FXML private lateinit var rgb05: Circle
    @FXML private lateinit var rgb06: Circle
    @FXML private lateinit var rgb07: Circle
    @FXML private lateinit var rgb08: Circle
    @FXML private lateinit var rgb09: Circle
    @FXML private lateinit var rgb10: Circle
    @FXML private lateinit var rgb11: Circle
    @FXML private lateinit var rgb12: Circle
    @FXML private lateinit var rgb13: Circle
    @FXML private lateinit var rgb14: Circle
    @FXML private lateinit var rgb15: Circle
    @FXML private lateinit var rgb16: Circle
    @FXML private lateinit var rgb17: Circle
    @FXML private lateinit var rgb18: Circle
    @FXML private lateinit var rgb19: Circle
    @FXML private lateinit var rgb20: Circle
    @FXML private lateinit var rgb21: Circle
    @FXML private lateinit var rgb22: Circle
    @FXML private lateinit var rgb23: Circle
    @FXML private lateinit var rgb24: Circle
    @FXML private lateinit var rgb25: Circle
    @FXML private lateinit var rgb26: Circle
    @FXML private lateinit var rgb27: Circle
    @FXML private lateinit var rgb28: Circle
    @FXML private lateinit var rgb29: Circle
    @FXML private lateinit var rgb30: Circle
    @FXML private lateinit var rgb31: Circle
    @FXML private lateinit var btn00: ToggleButton
    @FXML private lateinit var btn01: RadioButton
    @FXML private lateinit var btn0102: RadioButton
    @FXML private lateinit var btn02: RadioButton
    @FXML private lateinit var btn03: ToggleButton
    @FXML private lateinit var btn04: ToggleButton
    @FXML private lateinit var btn05: ToggleButton
    @FXML private lateinit var btn06: ToggleButton
    @FXML private lateinit var btn07: ToggleButton
    @FXML private lateinit var btn08: ToggleButton
    @FXML private lateinit var btn09: ToggleButton
    @FXML private lateinit var btn10: ToggleButton
    @FXML private lateinit var btn11: ToggleButton
    @FXML private lateinit var btn12: ToggleButton
    @FXML private lateinit var btn13: ToggleButton
    @FXML private lateinit var btn14: ToggleButton
    @FXML private lateinit var btn15: ToggleButton
    @FXML private lateinit var btn16: ToggleButton
    @FXML private lateinit var btn17: ToggleButton
    @FXML private lateinit var btn18: ToggleButton
    @FXML private lateinit var btn19: ToggleButton
    @FXML private lateinit var btn20: ToggleButton
    @FXML private lateinit var btn21: ToggleButton
    @FXML private lateinit var btn22: ToggleButton
    @FXML private lateinit var btn23: ToggleButton
    @FXML private lateinit var btn24: ToggleButton
    @FXML private lateinit var btn25: ToggleButton
    @FXML private lateinit var rotarySwitch: ToggleGroup
    @FXML private lateinit var keypad0: Button
    @FXML private lateinit var keypad1: Button
    @FXML private lateinit var keypad2: Button
    @FXML private lateinit var keypad3: Button
    @FXML private lateinit var keypad4: Button
    @FXML private lateinit var keypad5: Button
    @FXML private lateinit var keypad6: Button
    @FXML private lateinit var keypad7: Button
    @FXML private lateinit var keypad8: Button
    @FXML private lateinit var keypad9: Button
    @FXML private lateinit var keypadA: Button
    @FXML private lateinit var keypadB: Button
    @FXML private lateinit var keypadC: Button
    @FXML private lateinit var keypadD: Button
    @FXML private lateinit var keypadStar: Button
    @FXML private lateinit var keypadCross: Button
    @FXML private lateinit var lblData0: Label
    @FXML private lateinit var lblData1: Label
    @FXML private lateinit var lblData2: Label
    @FXML private lateinit var lblData3: Label
    @FXML private lateinit var lblData4: Label
    @FXML private lateinit var lblData5: Label
    @FXML private lateinit var lblData6: Label
    @FXML private lateinit var lblData7: Label
    @FXML private lateinit var lblData8: Label
    @FXML private lateinit var lblData9: Label

    private lateinit var notifier: DeploymentCaseNotifier

    @FXML
    fun initialize() {
        lcd.text = ""
        listOf(lblData0, lblData1, lblData2, lblData3, lblData4, lblData5, lblData6, lblData7, lblData8, lblData9)
                .forEach { label -> label.text = "" }
        SharedUiData.devicesProperty.addListener { _, _, devices -> updateButtonNames(devices) }
        SharedUiData.devices.addListener(ListChangeListener { updateButtonNames(it.list) })
    }

    private fun updateButtonNames(devices: List<Device>) {
        listOf(btn00, btn01, btn02,
                btn03, btn04, btn05, btn06, btn07, btn08, btn09, btn10, btn11, btn12, btn13, btn14, btn15,
                btn16, btn17, btn18, btn19, btn20, btn21, btn22, btn23, btn24, btn25).forEachIndexed { index, button ->
            button.text = devices.find { it.kind == DeviceKind.MCP23017 && it.key == "${index}" }
                    ?.name ?: ""
        }
    }

    override fun onAction(device: Device, value: String) {
        super.onAction(device, value)
        if (device.kind == DeviceKind.MCP23017 && device.key.toInt() < 0x20) listOf(btn00, btn01, btn02,
                btn03, btn04, btn05, btn06, btn07, btn08, btn09, btn10, btn11, btn12, btn13, btn14, btn15,
                btn16, btn17, btn18, btn19, btn20, btn21, btn22, btn23, btn24, btn25)
                .find { it.id == "btn%02d".format(device.key.toInt()) }
                ?.isSelected = value.toBoolean()
        btn0102.isSelected = !btn01.isSelected && !btn02.isSelected

        if (device.kind == DeviceKind.MCP23017 && device.key.toInt() >= 0x20) listOf(led0, led1, led2, led3, led4)
                .find { it.id == "led%d".format(device.key.toInt() - 0x20) }
                ?.fill = if (value.toBoolean()) Color.RED else Color.BLACK

        if (device.kind == DeviceKind.WS281x) listOf(rgb00, rgb01, rgb02, rgb03, rgb04, rgb05, rgb06, rgb07,
                rgb08, rgb09, rgb10, rgb11, rgb12, rgb13, rgb14, rgb15, rgb16, rgb17, rgb18, rgb19, rgb20,
                rgb21, rgb22, rgb23, rgb24, rgb25, rgb26, rgb27, rgb28, rgb29, rgb30, rgb31)
                .find { it.id == "rgb%02d".format(device.key.toInt()) }
                ?.fill = value
                .split(",")
                .mapNotNull { it.toIntOrNull() }
                .takeIf { it.size >= 4 }
                ?.let { (_, r, g, b) -> Color.rgb(r, g, b) }
                ?: Color.BLACK

        if (device.kind == DeviceKind.LCD && device.key.toInt() == LcdKey.MESSAGE.key) lcd.text = value
                .replace("\\n", "\n")

        if (device.kind == DeviceKind.VIRTUAL && device.key == VirtualKey.ENTER.key) value
                .split("|")
                .let { (id, content) -> id.toIntOrNull()?.let { it to content } }
                ?.let { (id, content) ->
                    listOf(lblData0, lblData1, lblData2, lblData3, lblData4, lblData5, lblData6, lblData7, lblData8,
                            lblData9).find { it.id == "lblData${id}" }?.text = content
                }
    }

    @FXML
    fun onTestButton(event: ActionEvent) {
        LOGGER.info("ID: ${(event.source as? Node)?.id}")
//        (event.source as? Node)?.id?.also { id ->
//            if (id == "btn0102") {
//                val data = (1..2)
//                        .asSequence()
//                        .map { Device(kind = DeviceKind.MCP23017, key = "${it}") }
//                        .map { device -> device to Variable(type = VariableType.BOOLEAN, value = "false") }
//                        .map { (device, value) -> Action(device = device, value = value) }
//                        .map { it.toData(SharedUiData.stateMachine).toByteArray() }
//                        .reduce { acc, bytes -> acc.plus(bytes) }
//                        .let { byteArrayOf(2).plus(it) }
//                notifier.communicator?.send(DeploymentCaseMessageKind.SET_STATE, data)
//            } else if (id.startsWith("btn")) {
//                val data = Device(kind = DeviceKind.MCP23017, key = "${id.substring(3, 5).toInt()}")
//                        .let { device -> device to (event.source as? ToggleButton)?.isSelected }
//                        .let { (device, value) -> device to Variable(type = VariableType.BOOLEAN, value = value.toString()) }
//                        .let { (device, value) -> Action(device = device, value = value) }
//                        .let { it.toData(SharedUiData.stateMachine).toByteArray() }
//                        .let { byteArrayOf(1).plus(it) }
//                notifier.communicator.send(DeploymentCaseMessageKind.SET_STATE, data)
//            }
//        }
    }

}
