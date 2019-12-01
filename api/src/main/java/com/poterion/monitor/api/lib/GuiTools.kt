package com.poterion.monitor.api.lib

import com.sun.javafx.scene.control.skin.TableViewSkin
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

private val LOGGER = LoggerFactory.getLogger("GuiTools")

private var columnToFitMethod: Method? = null

fun TableView<*>.autoFitTable() {
	if (columnToFitMethod == null) {
		try {
			columnToFitMethod = TableViewSkin::class.java.getDeclaredMethod("resizeColumnToFitContent", TableColumn::class.java, Int::class.javaPrimitiveType)
			columnToFitMethod?.isAccessible = true
		} catch (e: NoSuchMethodException) {
			e.printStackTrace()
		}
	}

	skin?.also { tableViewSkin ->
		for (column in columns) {
			try {
				columnToFitMethod?.invoke(tableViewSkin, column, -1)
			} catch (e: IllegalAccessException) {
				LOGGER.error(e.message, e)
			} catch (e: InvocationTargetException) {
				LOGGER.error(e.message, e)
			}
		}
	}
}

