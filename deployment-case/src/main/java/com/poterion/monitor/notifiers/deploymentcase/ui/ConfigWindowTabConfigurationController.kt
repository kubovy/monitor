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
package com.poterion.monitor.notifiers.deploymentcase.ui


import com.poterion.monitor.data.auth.BasicAuthConfig
import com.poterion.monitor.notifiers.deploymentcase.api.ConfigurationWindowActionListener
import com.poterion.monitor.notifiers.deploymentcase.control.DeploymentCaseNotifier
import com.poterion.monitor.notifiers.deploymentcase.data.*
import com.poterion.monitor.notifiers.deploymentcase.getDisplayNameValue
import com.poterion.monitor.notifiers.deploymentcase.toVariable
import com.poterion.monitor.notifiers.deploymentcase.toVariableFromValue
import javafx.beans.Observable
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Modality
import javafx.util.StringConverter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class ConfigWindowTabConfigurationController : ConfigurationWindowActionListener {
	companion object {
		private val LOGGER: Logger = LoggerFactory.getLogger(ConfigWindowTabConfigurationController::class.java)

		internal fun getRoot(config: DeploymentCaseConfig,
							 notifier: DeploymentCaseNotifier,
							 saveConfig: () -> Unit): Pair<ConfigWindowTabConfigurationController, Parent> =
				FXMLLoader(ConfigWindowTabConfigurationController::class.java.getResource("config-window-tab-configuration.fxml"))
						.let { it.load<Parent>() to it.getController<ConfigWindowTabConfigurationController>() }
						.let { (root, ctrl) ->
							ctrl.config = config
							ctrl.notifier = notifier
							ctrl.saveConfig = saveConfig
							ctrl.load()
							ctrl to root
						}

	}

	@FXML private lateinit var checkboxActive: CheckBox
	@FXML private lateinit var textName: TextField
	@FXML private lateinit var comboboxMethod: ComboBox<String>
	@FXML private lateinit var textURL: TextField
	@FXML private lateinit var textUsername: TextField
	@FXML private lateinit var textPassword: PasswordField
	@FXML private lateinit var textJobName: TextField
	@FXML private lateinit var textParameters: TextArea

	@FXML private lateinit var comboboxJobStatusColorNotExecuted: ComboBox<Variable?>
	@FXML private lateinit var comboboxJobStatusColorPending: ComboBox<Variable?>
	@FXML private lateinit var comboboxJobStatusColorInProgress: ComboBox<Variable?>
	@FXML private lateinit var comboboxJobStatusColorNotBuilt: ComboBox<Variable?>
	@FXML private lateinit var comboboxJobStatusColorSuccess: ComboBox<Variable?>
	@FXML private lateinit var comboboxJobStatusColorUnstable: ComboBox<Variable?>
	@FXML private lateinit var comboboxJobStatusColorAborted: ComboBox<Variable?>
	@FXML private lateinit var comboboxJobStatusColorFailure: ComboBox<Variable?>
	@FXML private lateinit var comboboxJobStatusColorUnknown: ComboBox<Variable?>

	@FXML private lateinit var comboboxPipelineStatusTargetStateSuccess: ComboBox<State?>
	@FXML private lateinit var comboboxPipelineStatusTargetStateFailure: ComboBox<State?>

	@FXML private lateinit var comboboxType: ComboBox<String>
	@FXML private lateinit var comboboxName: ComboBox<String>
	@FXML private lateinit var comboboxValue: ComboBox<String>

	private lateinit var config: DeploymentCaseConfig
	private lateinit var notifier: DeploymentCaseNotifier
	private lateinit var saveConfig: () -> Unit

	private val jobStatusColorComboBoxes = mutableMapOf<String, ComboBox<Variable?>>()
	private val pipelineStatusTargetStateComboBoxes = mutableMapOf<String, ComboBox<State?>>()
	private val saveConfigListener = { _: Observable, _: Any?, _: Any? ->
		if (!SharedUiData.isUpdateInProgress && !isUpdateInProgress) saveConfig()
	}
	private var isUpdateInProgress = false

	@FXML
	fun initialize() {
		// Config
		checkboxActive.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
		checkboxActive.selectedProperty().addListener { _, _, selected ->
			if (selected) config.configurations.forEach { configuration ->
				configuration.isActive = configuration == SharedUiData.configurationProperty.get() && selected
			} else SharedUiData.configurationProperty.get()?.isActive = selected
			saveConfig()
		}

		//textName.textProperty().addListener { _, _, value -> SharedUiData.configurationProperty.get()?.name = value }
		textName.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }

		comboboxMethod.items.addAll("GET", "POST")
		comboboxMethod.selectionModel.select(0)
		comboboxMethod.selectionModel.selectedItemProperty().addListener { _, _, method -> SharedUiData.configurationProperty.get()?.method = method }
		comboboxMethod.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }

		textURL.textProperty().addListener { _, _, value -> SharedUiData.configurationProperty.get()?.url = value }
		textURL.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }

		textUsername.textProperty().addListener { _, _, value ->
			SharedUiData.configurationProperty.get()?.also { config ->
				config.auth = config.auth ?: BasicAuthConfig()
				config.auth?.let { it as? BasicAuthConfig }?.username = value
			}
		}
		textUsername.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }

		textPassword.textProperty().addListener { _, _, value ->
			SharedUiData.configurationProperty.get()?.also {
				SharedUiData.configurationProperty.get()?.also { config ->
					config.auth = config.auth ?: BasicAuthConfig()
					config.auth?.let { it as? BasicAuthConfig }?.password = value
				}
			}
		}
		textPassword.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }

		textJobName.textProperty().addListener { _, _, value -> SharedUiData.configurationProperty.get()?.jobName = value }
		textJobName.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }

		textParameters.textProperty().addListener { _, _, value -> SharedUiData.configurationProperty.get()?.parameters = value }
		textParameters.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }

		comboboxJobStatusColorNotExecuted.initJobStatus("not_executed")
		comboboxJobStatusColorPending.initJobStatus("pending")
		comboboxJobStatusColorInProgress.initJobStatus("in_progress")
		comboboxJobStatusColorNotBuilt.initJobStatus("not_built")
		comboboxJobStatusColorSuccess.initJobStatus("success")
		comboboxJobStatusColorUnstable.initJobStatus("unstable")
		comboboxJobStatusColorAborted.initJobStatus("aborted")
		comboboxJobStatusColorFailure.initJobStatus("failure")
		comboboxJobStatusColorUnknown.initJobStatus("unknown")

		comboboxPipelineStatusTargetStateSuccess.initPipelineStatus("success")
		comboboxPipelineStatusTargetStateFailure.initPipelineStatus("failure")

		// Test
		comboboxType.items.addAll("State", "Action", "Transit")
		comboboxType.selectionModel.select(0)
		SharedUiData.configurationProperty.addListener { _, _, configuration ->
			isUpdateInProgress = true
			checkboxActive.isSelected = configuration?.isActive == true
			//textName.text = configuration?.name ?: ""
			textName.textProperty().bindBidirectional(SharedUiData.nameProperty)
			comboboxMethod.selectionModel.select(configuration?.method ?: "GET")
			textURL.text = configuration?.url ?: ""
			textUsername.text = configuration?.auth?.let { it as? BasicAuthConfig }?.username ?: ""
			textPassword.text = configuration?.auth?.let { it as? BasicAuthConfig }?.password ?: ""
			textJobName.text = configuration?.jobName ?: ""
			textParameters.text = configuration?.parameters ?: ""

			jobStatusColorComboBoxes.forEach { (status, combobox) ->
				combobox.selectionModel.select(combobox.items.find { configuration?.jobStatus?.get(status) == it?.name })
			}
			pipelineStatusTargetStateComboBoxes.forEach { (status, combobox) ->
				combobox.selectionModel.select(combobox.items.find { configuration?.pipelineStatus?.get(status) == it?.name })
			}
			isUpdateInProgress = false
		}
	}

	private fun load() {
		comboboxName.items.clear()
		comboboxValue.items.clear()
		config.testNameHistory.also { comboboxName.items.addAll(it) }
		config.testValueHistory.also { comboboxValue.items.addAll(it) }

	}

	override fun onUpload() {
		SharedUiData.configurationProperty.get()?.also { configuration ->
			Alert(Alert.AlertType.CONFIRMATION).apply {
				initModality(Modality.APPLICATION_MODAL)
				title = "Upload \"%s\" State Machine".format(configuration.name)
				headerText = "Uploading will overwrite the deployment football's configuration and it may take a while."
				contentText = "Do you want to proceed with uploading \"%s\" state machine to the deployment football?"
						.format(configuration.name)
				buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
			}.showAndWait().ifPresent { button ->
				when (button) {
					ButtonType.YES -> {
						if (!checkboxActive.isSelected) {
							checkboxActive.isSelected = true
						} else {
							notifier.pushStateMachine()
						}
					}
				}
			}
		}
	}

	override fun onKeyPressed(keyEvent: KeyEvent) = when (keyEvent.code) {
		KeyCode.F4 -> onTest()
		else -> null
	}

	@FXML
	fun onTest() {
		val type = comboboxType.selectionModel.selectedItem?.toLowerCase()
		val name = comboboxName.value
		val value = comboboxValue.value

		if (comboboxName.items.contains(name).not()) comboboxName.items.add(0, name)
		if (comboboxValue.items.contains(value).not()) comboboxValue.items.add(0, value)

		while (comboboxName.items.size > 20) comboboxName.items.removeAt(comboboxName.items.size - 1)
		while (comboboxValue.items.size > 20) comboboxValue.items.removeAt(comboboxValue.items.size - 1)

		LOGGER.debug("Message: ${type},${name},${value}")
		//notifier?.communicator?.send("${type},${name},${value}")
		saveConfig()
	}

	private fun ComboBox<Variable?>.initJobStatus(status: String) {
		userData = status
		converter = object : StringConverter<Variable?>() {
			override fun toString(variable: Variable?) = variable
					?.getDisplayNameValue()
					?.takeIf { it.isNotEmpty() }
					?: "-- Not selected --"

			override fun fromString(string: String?) = string
					?.toVariableFromValue(SharedUiData.variables)
		}

		SharedUiData.variablesProperty.addListener { _, _, variables ->
			items = variables
					?.filtered { it?.type == VariableType.COLOR_PATTERN }
					?.sorted { o1, o2 -> compareValues(o1?.name, o2?.name) }
		}

		SharedUiData.jobStatusProperty.addListener { _, _, jobStatus ->
			selectionModel.select(jobStatus[status]?.toVariable(SharedUiData.variables))
		}

		selectionModel.selectedItemProperty().addListener(saveConfigListener)
		selectionModel.selectedItemProperty().addListener { observable, _, variable ->
			((observable as? ComboBox<*>)?.userData as? String)?.also { SharedUiData.jobStatus[it] = variable?.name }
		}
		jobStatusColorComboBoxes[status] = this
	}

	private fun ComboBox<State?>.initPipelineStatus(status: String) {
		userData = status
		converter = object : StringConverter<State?>() {
			override fun toString(state: State?) = state?.name ?: "-- Not selected --"
			override fun fromString(string: String?) = SharedUiData.stateMachine.firstOrNull { it.name == string }
		}

		SharedUiData.stateMachineProperty.addListener { _, _, states ->
			items = states.sorted { o1, o2 -> compareValues(o1?.name, o2.name) }
			selectionModel.select(states.find { it.name == SharedUiData.pipelineStatus[status] })
		}

		selectionModel.selectedItemProperty().addListener(saveConfigListener)
		selectionModel.selectedItemProperty().addListener { observable, _, state ->
			((observable as? ComboBox<*>)?.userData as? String)?.also { SharedUiData.pipelineStatus[it] = state?.name }
		}
		pipelineStatusTargetStateComboBoxes[status] = this
	}

}
