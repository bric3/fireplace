/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.jfr.support

import io.github.bric3.fireplace.flamegraph.FrameBox
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node
import java.util.function.ToDoubleFunction

/**
 * Creates an array of FlameNodes that live in the [0.0, 1.0] world space on the X axis and the depth of the stack representing
 * the Y axis.
 * A child node will be proportional to its parent's X space according to its proportion of time it took of its parent's time.
 * The root of the flame graph will always be full width.
 */
object JfrFrameNodeConverter {
    fun convert(node: Node): List<FrameBox<Node>> {
        val nodes = mutableListOf<FrameBox<Node>>()
        FrameBox.flattenAndCalculateCoordinate(
            nodes,
            node,
            Node::getChildren,
            Node::getCumulativeWeight,
            { it.children.stream().mapToDouble(Node::getCumulativeWeight).sum() },
            0.0,
            1.0,
            0
        )
        assert(nodes[0].actualNode.isRoot) { "First node should be the root node" }
        return nodes
    }

    fun convertButterfly(
        node: io.github.bric3.fireplace.jfr.tree.Node,
        nodeWeightFunction: ToDoubleFunction<io.github.bric3.fireplace.jfr.tree.Node>
    ): MutableList<FrameBox<io.github.bric3.fireplace.jfr.tree.Node>> {
        val nodes = mutableListOf<FrameBox<io.github.bric3.fireplace.jfr.tree.Node>>()
        FrameBox.flattenAndCalculateCoordinate(
            nodes,
            node,
            io.github.bric3.fireplace.jfr.tree.Node::getChildren,
            nodeWeightFunction,
            { it.children.stream().mapToDouble(nodeWeightFunction).sum() },
            0.0,
            1.0,
            0
        )
        assert(nodes[0].actualNode.isRoot) { "First node should be the root node" }
        return nodes
    }

    fun predecessorsWeight() = io.github.bric3.fireplace.jfr.tree.Node::getWeight
    fun successorsWeight() = io.github.bric3.fireplace.jfr.tree.Node::getCumulativeWeight
}