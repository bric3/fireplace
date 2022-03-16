/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace;

import io.github.bric3.fireplace.flamegraph.FrameBox;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates an array of FlameNodes that live in the [0.0, 1.0] world space on the X axis and the depth of the stack representing
 * the Y axis.
 * A child node will be proportional to its parent's X space according to its proportion of time it took of its parent's time.
 * The root of the flame graph will always be full width.
 */
public class JfrFrameNodeConverter {
    public static List<FrameBox<Node>> convert(StacktraceTreeModel model) {
        var nodes = new ArrayList<FrameBox<Node>>();

        FrameBox.flattenAndCalculateCoordinate(
                nodes,
                model.getRoot(),
                Node::getChildren,
                Node::getCumulativeWeight,
                0.0d,
                1.0d,
                0
        );

        assert nodes.get(0).actualNode.isRoot() : "First node should be the root node";

        return nodes;
    }
}
