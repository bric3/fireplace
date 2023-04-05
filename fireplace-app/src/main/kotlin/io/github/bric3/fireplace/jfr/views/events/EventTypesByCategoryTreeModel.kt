/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.jfr.views.events

import io.github.bric3.fireplace.jfr.TypeCategory
import io.github.bric3.fireplace.jfr.TypeCategoryExtractor
import io.github.bric3.fireplace.core.ui.Colors
import org.openjdk.jmc.common.item.IItemCollection
import org.openjdk.jmc.common.item.IType
import java.text.DecimalFormat
import java.util.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode

internal class EventTypesByCategoryTreeModel : DefaultTreeModel(
    SortedTreeNode(Comparator.comparing(CategoryOrType::category))
) {
    var typeToCategory: Map<IType<*>, TypeCategory> = mapOf()

    internal class CategoryOrType(val category: String) {
        internal var eventCount = 0L


        override fun toString(): String {
            // poor 's man renderer
            return if (eventCount > 0) {
                "<html>$category <span style='color:" +
                        Colors.toHex(Colors.blue) +
                        ";'>(${DF.format(eventCount)})</span></html>"
            } else {
                category
            }
        }

        companion object {
            private val DF = DecimalFormat("#,##0")
        }
    }

    private fun fetchOrAdd(categorisation: List<String>): SortedTreeNode<CategoryOrType> {
        @Suppress("UNCHECKED_CAST") // We know the type of the root
        var current = getRoot() as SortedTreeNode<CategoryOrType>
        for (category in categorisation) {
            current = fetchOrAdd(category, current)
        }
        return current
    }

    private fun fetchOrAdd(category: String, node: MutableTreeNode): SortedTreeNode<CategoryOrType> {
        for (i in 0 until node.childCount) {
            @Suppress("UNCHECKED_CAST") // Only inserting nodes of type CategoryOrType
            val child = node.getChildAt(i) as SortedTreeNode<CategoryOrType>
            val content = child.userObject
            if (category == content.category) {
                return child
            }
        }
        val content = CategoryOrType(category)
        return SortedTreeNode(Comparator.comparing(CategoryOrType::category), content).also {
            node.insert(it, node.childCount)
        }
    }

    fun populateTree(events: IItemCollection) {
        typeToCategory = TypeCategoryExtractor.extract(events)
        typeToCategory.forEach { (eventType, eventTypeDetails) ->
            if (eventTypeDetails.count == 0L) {
                return@forEach
            }
            val parentCategory = fetchOrAdd(eventTypeDetails.categories)
            fetchOrAdd(eventType.name, parentCategory).also {
                it.userObject.eventCount = eventTypeDetails.count
            }
        }
        // fire change event
        nodeStructureChanged(root)
    }
}

private class SortedTreeNode<T> : DefaultMutableTreeNode {
    private val comparator: Comparator<MutableTreeNode>?

    constructor(comparator: Comparator<T>? = null) : super() {
        this.comparator = makeComparator(comparator)
    }

    constructor(comparator: Comparator<T>?, userObject: T) : super(userObject) {
        this.comparator = makeComparator(comparator)
    }

    constructor(comparator: Comparator<T>?, userObject: T, allowsChildren: Boolean) : super(
        userObject,
        allowsChildren
    ) {
        this.comparator = makeComparator(comparator)
    }

    override fun insert(newChild: MutableTreeNode, childIndex: Int) {
        super.insert(newChild, childIndex)

        comparator?.let {
            // This code only inserts `DefaultMutableTreeNode`
            @Suppress("UNCHECKED_CAST")
            Collections.sort(children as Vector<MutableTreeNode>, it)
        }
    }

    private fun makeComparator(comparator: Comparator<T>?): Comparator<MutableTreeNode>? {
        comparator ?: return null
        // So MutableTreeNode is part of the `insert` signature, but
        // `getUserObject` is not part of the `MutableTreeNode` interface.
        return Comparator.comparing(
            { tn: MutableTreeNode ->
                @Suppress("UNCHECKED_CAST")
                (tn as DefaultMutableTreeNode).userObject as T
            },
            comparator
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun getUserObject(): T {
        return super.getUserObject() as T
    }
}

