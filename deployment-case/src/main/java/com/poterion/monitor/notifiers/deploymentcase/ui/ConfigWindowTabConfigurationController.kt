package com.poterion.monitor.notifiers.deploymentcase.ui


import com.poterion.monitor.notifiers.deploymentcase.api.ConfigurationContributer
import com.poterion.monitor.notifiers.deploymentcase.api.ConfigurationWindowActionListener
import com.poterion.monitor.notifiers.deploymentcase.control.DeploymentCaseNotifier
import com.poterion.monitor.notifiers.deploymentcase.data.*
import com.poterion.monitor.notifiers.deploymentcase.getDisplayNameValue
import com.poterion.monitor.notifiers.deploymentcase.toVariable
import javafx.beans.Observable
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.util.StringConverter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ConfigWindowTabConfigurationController : ConfigurationContributer, ConfigurationWindowActionListener {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(ConfigWindowTabConfigurationController::class.java)

        internal fun getRoot(config: DeploymentCaseConfig,
                             notifier: DeploymentCaseNotifier,
                             saveConfig: () -> Unit,
                             setRootPaneEnabled: (Boolean) -> Unit): Pair<ConfigWindowTabConfigurationController, Parent> =
                FXMLLoader(ConfigWindowTabConfigurationController::class.java.getResource("config-window-tab-configuration.fxml"))
                        .let { it.load<Parent>() to it.getController<ConfigWindowTabConfigurationController>() }
                        .let { (root, ctrl) ->
                            ctrl.config = config
                            ctrl.notifier = notifier
                            ctrl.saveConfig = saveConfig
                            ctrl.setRootPaneEnabled = setRootPaneEnabled
                            ctrl.load()
                            ctrl to root
                        }

    }

    @FXML
    private lateinit var checkboxActive: CheckBox
    @FXML
    private lateinit var textName: TextField
    @FXML
    private lateinit var comboboxMethod: ComboBox<String>
    @FXML
    private lateinit var textURL: TextField
    @FXML
    private lateinit var textUsername: TextField
    @FXML
    private lateinit var textPassword: PasswordField
    @FXML
    private lateinit var textJobName: TextField
    @FXML
    private lateinit var textParameters: TextArea

    @FXML
    private lateinit var comboboxJobStatusColorNotExecuted: ComboBox<Variable?>
    @FXML
    private lateinit var comboboxJobStatusColorPending: ComboBox<Variable?>
    @FXML
    private lateinit var comboboxJobStatusColorInProgress: ComboBox<Variable?>
    @FXML
    private lateinit var comboboxJobStatusColorNotBuilt: ComboBox<Variable?>
    @FXML
    private lateinit var comboboxJobStatusColorSuccess: ComboBox<Variable?>
    @FXML
    private lateinit var comboboxJobStatusColorUnstable: ComboBox<Variable?>
    @FXML
    private lateinit var comboboxJobStatusColorAborted: ComboBox<Variable?>
    @FXML
    private lateinit var comboboxJobStatusColorFailure: ComboBox<Variable?>
    @FXML
    private lateinit var comboboxJobStatusColorUnknown: ComboBox<Variable?>

    @FXML
    private lateinit var comboboxPipelineStatusTargetStateSuccess: ComboBox<State?>
    @FXML
    private lateinit var comboboxPipelineStatusTargetStateFailure: ComboBox<State?>

    @FXML
    private lateinit var comboboxType: ComboBox<String>
    @FXML
    private lateinit var comboboxName: ComboBox<String>
    @FXML
    private lateinit var comboboxValue: ComboBox<String>

    private lateinit var config: DeploymentCaseConfig
    private lateinit var notifier: DeploymentCaseNotifier
    private lateinit var saveConfig: () -> Unit
    private lateinit var setRootPaneEnabled: (Boolean) -> Unit

    private val jobStatusColorComboBoxes = mutableMapOf<String, ComboBox<Variable?>>()
    private val pipelineStatusTargetStateComboBoxes = mutableMapOf<String, ComboBox<State?>>()
    private val saveConfigListener = { _: Observable, _: Any?, _: Any? -> if (!isUpdateInProgress) saveConfig() }
    private var isUpdateInProgress = false

    @FXML
    fun initialize() {
        // Config
        checkboxActive.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
        checkboxActive.setOnAction { saveConfig() }
        checkboxActive.selectedProperty().addListener { _, _, selected ->
            // FIXME
//            val selectedConfig = listConfigurations.selectionModel.selectedItem
//            if (selected) {
//                listConfigurations.items
//                        .filter { it != listConfigurations.selectionModel.selectedItem }
//                        .forEach { it.isActive = false }
//                selectedConfig?.also { notifier.synchronizeStateMachine() }
//            }
//            selectedConfig?.isActive = selected
        }
        textName.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }

        comboboxMethod.items.addAll("GET", "POST")
        comboboxMethod.selectionModel.select(0)
        comboboxMethod.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }

        textURL.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
        textUsername.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
        textPassword.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
        textJobName.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
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
    }

    private fun load() {
        comboboxName.items.clear()
        comboboxValue.items.clear()
        config.testNameHistory.also { comboboxName.items.addAll(it) }
        config.testValueHistory.also { comboboxValue.items.addAll(it) }

    }

    override fun notifyNewConfiguration(configuration: Configuration) {
        checkboxActive.isSelected = configuration.isActive
        textName.text = configuration.name
        comboboxMethod.selectionModel.select(configuration.method)
        textURL.text = configuration.url
        textUsername.text = configuration.auth?.username
        textPassword.text = configuration.auth?.password
        textJobName.text = configuration.jobName
        textParameters.text = configuration.parameters
        refreshJobStatusColors(configuration)
        refreshPipelineStatusTargetStates(configuration)
    }

    override fun updateConfiguration(config: DeploymentCaseConfig, configuration: Configuration?) {
        config.testNameHistory = comboboxName.items
        config.testValueHistory = comboboxValue.items

        configuration?.isActive = checkboxActive.isSelected
        configuration?.name = textName.text
        configuration?.method = comboboxMethod.selectionModel.selectedItem
        configuration?.url = textURL.text
        configuration?.auth?.username = textUsername.text
        configuration?.auth?.password = textPassword.text
        configuration?.jobName = textJobName.text
        configuration?.parameters = textParameters.text
        configuration?.jobStatus = jobStatusColorComboBoxes
                .map { (status, combobox) -> status to combobox.selectionModel.selectedItem }
                .filter { (_, color) -> color != null }
                .map { (status, color) -> status to color!! }
                .toMap()
        configuration?.pipelineStatus = pipelineStatusTargetStateComboBoxes
                .map { (status, combobox) -> status to combobox.selectionModel.selectedItem }
                .map { (status, state) -> status to state?.name }
                .filter { (_, stateName) -> stateName != null }
                .map { (status, stateName) -> status to stateName!! }
                .toMap()

        refreshJobStatusColors(configuration)
        refreshPipelineStatusTargetStates(configuration)
    }

    override fun onUpload() {
        Alert(Alert.AlertType.CONFIRMATION).apply {
            title = "Upload State Machine"
            headerText = "Uploading will overwrite the deployment football's configuration and it may take a while."
            contentText = "Do you want to proceed?"
            buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
        }.showAndWait().ifPresent { button ->
            when (button) {
                ButtonType.YES -> {
                    setRootPaneEnabled(false)
                    if (!checkboxActive.isSelected) {
                        checkboxActive.isSelected = true
                    } else {
                        notifier.pushStateMachine()
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

    private fun refreshJobStatusColors(configuration: Configuration?) {
        isUpdateInProgress = true
        jobStatusColorComboBoxes.forEach { status, combobox ->
            combobox.items.clear()
            combobox.items.add(null)
            combobox.items.addAll(SharedUiData.variables.filtered { it.type == VariableType.COLOR_PATTERN }.sorted { o1, o2 -> o1.name.compareTo(o2.name) })
            combobox.selectionModel.select(configuration?.jobStatus?.get(status))
        }
        isUpdateInProgress = false
    }

    private fun refreshPipelineStatusTargetStates(configuration: Configuration?) {
        isUpdateInProgress = true
        pipelineStatusTargetStateComboBoxes.forEach { status, combobox ->
            combobox.items.clear()
            combobox.items.add(null)
            combobox.items.addAll(SharedUiData.stateMachine)
            combobox.selectionModel.select(combobox.items.find { configuration?.pipelineStatus?.get(status) == it?.name })
        }
        isUpdateInProgress = false
    }

    private fun ComboBox<Variable?>.initJobStatus(status: String) {
        userData = status
        converter = object : StringConverter<Variable?>() {
            override fun toString(device: Variable?) = device?.getDisplayNameValue() ?: "-- Not selected --"
            override fun fromString(string: String?) = string?.toVariable(SharedUiData.variables)
        }
        selectionModel.selectedItemProperty().addListener(saveConfigListener)
        selectionModel.selectedItemProperty().addListener { observable, _, newValue ->
            ((observable as? ComboBox<*>)?.userData as? String)?.also { SharedUiData.jobStatusColors[it] = newValue }
        }
        jobStatusColorComboBoxes[status] = this
    }

    private fun ComboBox<State?>.initPipelineStatus(status: String) {
        userData = status
        converter = object : StringConverter<State?>() {
            override fun toString(state: State?) = state?.name ?: "-- Not selected --"
            override fun fromString(string: String?) = SharedUiData.stateMachine.firstOrNull { it.name == string }
        }
        selectionModel.selectedItemProperty().addListener(saveConfigListener)
        selectionModel.selectedItemProperty().addListener { observable, _, newValue ->
            ((observable as? ComboBox<*>)?.userData as? String)?.also { SharedUiData.pipelineStatusTargetStates[it] = newValue }
        }
        pipelineStatusTargetStateComboBoxes[status] = this
    }

}
