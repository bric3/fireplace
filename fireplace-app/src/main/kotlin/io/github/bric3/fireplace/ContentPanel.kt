package io.github.bric3.fireplace

import java.awt.BorderLayout
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel


internal class ContentPanel(private val jfrBinder: JFRBinder) : JPanel(BorderLayout()) {
    private val SYSTEM_PROPERTIES = "System properties"
    private val NATIVE_LIBRARIES = "Native libraries"
    private val ALLOCATIONS = "Allocations"
    private val CPU = "CPU"

    private val views = linkedMapOf(
        CPU to FlameGraphTab().apply {
            jfrBinder.bindEvents(
                JfrAnalyzer::stackTraceCPUFun,
                this::setStacktraceTreeModel,
            )
        },
        ALLOCATIONS to FlameGraphTab().apply {
            jfrBinder.bindEvents(
                JfrAnalyzer::stackTraceAllocationFun,
                this::setStacktraceTreeModel
            )
        },
        SYSTEM_PROPERTIES to JTextArea().apply {
            isEditable = false
            dropTarget = null
            caret.isVisible = true
            caret.isSelectionVisible = true
            jfrBinder.bindEvents(
                JfrAnalyzer::jvmSystemProperties,
                this::setText
            )
        },
        NATIVE_LIBRARIES to JTextArea().apply {
            isEditable = false
            dropTarget = null
            caret.isVisible = true
            caret.isSelectionVisible = true
            jfrBinder.bindEvents(
                JfrAnalyzer::nativeLibraries,
                this::setText
            )
        },
    )

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
                    view.add(it as JComponent)
                    view.revalidate()
                    view.repaint()
                }
            }
            selectionPath = TreePath(model.firstLeaf.path)
        }

        JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jTree, view).apply {
            
        }.also {
            add(it, BorderLayout.CENTER)
        }
    }
}
