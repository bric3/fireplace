package io.github.bric3.fireplace

import io.github.bric3.fireplace.views.appDebug.AppSystemProperties
import io.github.bric3.fireplace.views.appDebug.AppUIManagerProperties
import io.github.bric3.fireplace.views.cpu.MethodCpuSample
import io.github.bric3.fireplace.views.general.NativeLibraries
import io.github.bric3.fireplace.views.general.SystemProperties
import io.github.bric3.fireplace.views.memory.Allocations
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel


internal class ContentPanel(private val jfrBinder: JFRBinder) : JPanel(BorderLayout()) {
    private val views = buildList {
        add(MethodCpuSample(jfrBinder))
        add(Allocations(jfrBinder))
        add(SystemProperties(jfrBinder))
        add(NativeLibraries(jfrBinder))

        if (AppSystemProperties.isActive()) {
            add(AppSystemProperties())
        }
        if (AppUIManagerProperties.isActive()) {
            add(AppUIManagerProperties())
        }
    }.associateByTo(LinkedHashMap()) { it.identifier }

    init {
        val view = JPanel(BorderLayout())

        val model = DefaultMutableTreeNode().apply {
            views.keys.forEach { add(DefaultMutableTreeNode(it)) }
        }
        val jTree = JTree(model).apply {
            selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
            addTreeSelectionListener { e ->
                val lastPathComponent = e.path.lastPathComponent as DefaultMutableTreeNode
                views[lastPathComponent.userObject as String]?.let {
                    view.removeAll()
                    view.add(it.getView())
                    view.revalidate()
                    view.repaint()
                }
            }
            selectionPath = TreePath(model.firstLeaf.path)
            minimumSize = preferredSize
        }

        JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jTree, view).apply {
        }.also {
            add(it, BorderLayout.CENTER)
        }
    }
}
