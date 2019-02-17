package com.poterion.monitor.notifiers.deploymentcase.ui

import com.poterion.monitor.notifiers.deploymentcase.control.setStateMachine
import com.poterion.monitor.notifiers.deploymentcase.data.Placeholder
import com.poterion.monitor.notifiers.deploymentcase.data.State
import com.poterion.monitor.notifiers.deploymentcase.data.StateMachineItem
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.stage.Stage


class StateCompareWindowController {

	companion object {
		fun popup(stateMachine1: List<State>, stateMachine2: List<State>) {

			val loader = FXMLLoader(StateCompareWindowController::class.java.getResource("state-compare-window.fxml"))
			val root = loader.load<Parent>()
			val controller = loader.getController<StateCompareWindowController>()
			controller.stateMachine1 = stateMachine1
			controller.stateMachine2 = stateMachine2

			Stage().apply {
				//initModality(Modality.APPLICATION_MODAL)
				//content.add(loader.load())
				scene = Scene(root, 1200.0, 800.0)
				controller.load()
				show()
			}
		}
	}

	//@FXML private lateinit var resources: ResourceBundle
	//@FXML private lateinit var location: URL
	@FXML private lateinit var treeViewLeft: TreeView<StateMachineItem>
	@FXML private lateinit var treeViewRight: TreeView<StateMachineItem>

	private var stateMachine1: List<State> = emptyList()
	private var stateMachine2: List<State> = emptyList()
	private var selectedIndex = 0

	internal fun load() {
		treeViewLeft.root = TreeItem<StateMachineItem>(Placeholder())
		treeViewRight.root = TreeItem<StateMachineItem>(Placeholder())

		treeViewLeft.selectionModel.selectedIndexProperty().addListener { _, _, value -> updateSelection(value.toInt()) }
		treeViewRight.selectionModel.selectedIndexProperty().addListener { _, _, value -> updateSelection(value.toInt()) }

		treeViewLeft.setStateMachine(stateMachine1)
		treeViewRight.setStateMachine(stateMachine2)
	}

	private fun updateSelection(index: Int) {
		treeViewLeft.selectionModel.takeIf { it.selectedIndex != index }?.also { it.select(index) }
		treeViewRight.selectionModel.takeIf { it.selectedIndex != index }?.also { it.select(index) }
	}
}
