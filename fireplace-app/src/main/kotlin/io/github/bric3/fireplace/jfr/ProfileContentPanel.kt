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

import io.github.bric3.fireplace.appDebug.FireplaceAppSystemProperties
import io.github.bric3.fireplace.appDebug.FireplaceAppUIManagerProperties
import io.github.bric3.fireplace.jfr.support.JFRLoaderBinder
import io.github.bric3.fireplace.ui.ViewPanel
import io.github.bric3.fireplace.ui.ViewPanel.Priority
import io.github.classgraph.ClassGraph
import java.awt.BorderLayout
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel


internal class ProfileContentPanel(private val jfrBinder: JFRLoaderBinder) : JPanel(BorderLayout()) {
    private fun listViewPanels() = ClassGraph()
        .enableAllInfo()
        .acceptPackages(javaClass.packageName)
        .scan()
        .use { scanResult ->
            scanResult.getClassesImplementing(ViewPanel::class.java)
                .standardClasses
                .filter { !it.isAbstract }
                .asSequence()
                .map { it.loadClass(ViewPanel::class.java) }
                .sortedBy { it.getAnnotation(Priority::class.java)?.value ?: 100_000 }
                .map { it.getDeclaredConstructor(JFRLoaderBinder::class.java).newInstance(jfrBinder) }
                .toList()
        }

    private val views = buildList {
        addAll(listViewPanels())

        if (FireplaceAppSystemProperties.isActive()) {
            add(FireplaceAppSystemProperties())
        }
        if (FireplaceAppUIManagerProperties.isActive()) {
            add(FireplaceAppUIManagerProperties())
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
