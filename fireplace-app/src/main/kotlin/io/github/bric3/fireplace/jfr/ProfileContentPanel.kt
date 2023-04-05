/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.jfr

import io.github.bric3.fireplace.jfr.views.appDebug.AppSystemProperties
import io.github.bric3.fireplace.jfr.views.appDebug.AppUIManagerProperties
import io.github.bric3.fireplace.jfr.views.cpu.MethodCpuSample
import io.github.bric3.fireplace.jfr.views.events.EventBrowser
import io.github.bric3.fireplace.jfr.views.general.NativeLibraries
import io.github.bric3.fireplace.jfr.views.general.SystemProperties
import io.github.bric3.fireplace.jfr.views.memory.Allocations
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel


internal class ProfileContentPanel(private val jfrBinder: JFRLoaderBinder) : JPanel(BorderLayout()) {
    private val views = buildList {
        add(MethodCpuSample(jfrBinder))
        add(Allocations(jfrBinder))
        add(SystemProperties(jfrBinder))
        add(NativeLibraries(jfrBinder))
        add(EventBrowser(jfrBinder))

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
                    view.add(it.view)
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
