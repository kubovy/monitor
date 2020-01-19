package com.poterion.monitor.notifiers.deploymentcase.ui

import com.poterion.monitor.api.lib.toImageView
import com.poterion.monitor.notifiers.deploymentcase.control.setStateMachine
import com.poterion.monitor.notifiers.deploymentcase.data.*
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.TreeCell
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.stage.Stage
import javafx.util.Callback
import kotlin.math.max

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
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

	@FXML private lateinit var treeViewLeft: TreeView<StateMachineItem>
	@FXML private lateinit var treeViewRight: TreeView<StateMachineItem>

	private var stateMachine1: List<State> = emptyList()
	private var stateMachine2: List<State> = emptyList()
	private var differences: MutableList<StateMachineItem> = mutableListOf()
	private var childrenDifferences: MutableList<StateMachineItem> = mutableListOf()
	private var extra: MutableList<StateMachineItem> = mutableListOf()

	internal fun load() {
		treeViewLeft.root = TreeItem<StateMachineItem>(Placeholder())
		treeViewRight.root = TreeItem<StateMachineItem>(Placeholder())

		val cellFactory = Callback<TreeView<StateMachineItem>, TreeCell<StateMachineItem>> { _ ->
			object : TreeCell<StateMachineItem>() {
				override fun updateItem(item: StateMachineItem?, empty: Boolean) {
					super.updateItem(item, empty)
					if (empty) {
						text = null
						graphic = null
					} else {
						text = item?.getTitle(SharedUiData.devices, SharedUiData.variables)
						graphic = item?.icon?.toImageView()
						style = when {
							treeView.selectionModel.selectedItem?.value == item -> null
							item is Evaluation -> null
							item is Placeholder -> null
							differences.contains(item) -> "-fx-background-color: #FFCCCC"
							extra.contains(item) -> "-fx-background-color: #FFCC99"
							childrenDifferences.contains(item) -> "-fx-background-color: #CCCCFF"
							else -> "-fx-background-color: #CCFFCC"
						}
					}
				}
			}
		}

		treeViewLeft.cellFactory = cellFactory
		treeViewRight.cellFactory = cellFactory

		treeViewLeft.selectionModel.selectedItemProperty().addListener { _, _, value -> updateSelection(treeViewRight, value) }
		treeViewRight.selectionModel.selectedItemProperty().addListener { _, _, value -> updateSelection(treeViewLeft, value) }

		treeViewLeft.setStateMachine(stateMachine1)
		treeViewRight.setStateMachine(stateMachine2)

		compare(treeViewLeft.root, treeViewRight.root)
		treeViewLeft.refresh()
		treeViewRight.refresh()
	}

	private fun compare(itemA: TreeItem<StateMachineItem>, itemB: TreeItem<StateMachineItem>): Boolean {
		var binarySame = itemA.value.isBinarySame(itemB.value, SharedUiData.devices, SharedUiData.variables)
		if (!binarySame) {
			differences.add(itemA.value)
			differences.add(itemB.value)
		}

		binarySame = (0 until max(itemA.children.size, itemB.children.size))
				.map {
					if (it < itemA.children.size && it < itemB.children.size) {
						compare(itemA.children[it], itemB.children[it])
					} else if (it < itemA.children.size) {
						extra.add(itemA.children[it].value)
						false
					} else if (it < itemB.children.size) {
						extra.add(itemB.children[it].value)
						false
					} else true
				}
				.takeIf { it.isNotEmpty() }
				?.reduce { acc, b -> acc && b }
				?: true
		//if (binarySame) itemA.isExpanded = false
		if (!binarySame) {
			childrenDifferences.add(itemA.value)
			childrenDifferences.add(itemB.value)
		}

		return binarySame
	}

	private fun updateSelection(treeView: TreeView<StateMachineItem>, item: TreeItem<StateMachineItem>) {
		val indices = mutableListOf<Int>()
		var child = item
		while (child.parent != null) {
			indices.add(child.parent.children.indexOf(child))
			child = child.parent
		}

		var otherItem = treeView.root
		indices.reversed().forEach { otherItem = otherItem.children[it] ?: otherItem }
		treeView.selectionModel.select(otherItem)
		//treeView.selectionModel.selectedIndex.takeIf { it >= 0 }?.also { treeView.scrollTo(it) }
		treeViewLeft.refresh()
		treeViewRight.refresh()
	}
}
