/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.jfr.tree;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.MCFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.AggregatableFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;

public class StacktraceButterflyModel {
    private final Node predecessorsRoot;
    private final Node successorsRoot;
    private final IItemCollection items;
    private final IAttribute<IQuantity> attribute;
    private final FrameSeparator frameSeparator;

    /**
     * A special marker object that indicates a hand-crafted frame at the root of the tree.
     * <p>
     * We need to create this frame as a parent to all branches of the tree we want to represent.
     */
    private static final IMCFrame ROOT_FRAME = new MCFrame(null, null, null, IMCFrame.Type.UNKNOWN);

    private StacktraceButterflyModel(Node predecessorsRoot, Node successorsRoot, IItemCollection items, IAttribute<IQuantity> attribute, FrameSeparator frameSeparator) {
        this.predecessorsRoot = predecessorsRoot;
        this.successorsRoot = successorsRoot;
        this.items = items;
        this.attribute = attribute;
        this.frameSeparator = frameSeparator;
    }

    /**
     * Create a butterfly model, that is focused on a specific frame.
     * This model allows to model predecessors and successors of the frame as merged trees.
     *
     * @param stacktraceTreeModel The {@link StacktraceTreeModel}
     * @param frameSelector       The frame selector, this selector should only select a single frame otherwise the result is undefined.
     * @return A stacktrace Butterfly model.
     */
    public static StacktraceButterflyModel from(
            StacktraceTreeModel stacktraceTreeModel,
            Predicate<AggregatableFrame> frameSelector
    ) {
        Objects.requireNonNull(stacktraceTreeModel, "StacktraceTreeModel must not be null");
        Objects.requireNonNull(frameSelector, "Frame selector must not be null");

        // TODO modify StacktraceTreeModel to expose 'frameSeparator', 'invertedStacks'
        if (StacktraceTreeModelAccessor.isInvertedStacks(stacktraceTreeModel)) {
            throw new IllegalArgumentException("Inverted stacks are not supported");
        }
        IItemCollection items = stacktraceTreeModel.getItems();
        IAttribute<IQuantity> attribute = stacktraceTreeModel.getAttribute();
        FrameSeparator frameSeparator = StacktraceTreeModelAccessor.getFrameSeparator(stacktraceTreeModel);

        Node predecessorsRoot = computePredecessorsTree(stacktraceTreeModel.getRoot(), frameSeparator, frameSelector);
        Node successorsRoot = computeSuccessorsTree(stacktraceTreeModel.getRoot(), frameSeparator, frameSelector);

        assert predecessorsRoot.getChildren().size() == 1 && successorsRoot.getChildren().size() == 1 :
                "Predecessors and successors root should have a single child";
        assert predecessorsRoot.getChildren().get(0).getFrame().equals(successorsRoot.getChildren().get(0).getFrame()) :
                "Predecessors and successors root should be the same frame";

        return new StacktraceButterflyModel(predecessorsRoot, successorsRoot, items, attribute, frameSeparator);
    }

    /**
     * Compute the predecessor frames, aka <em>callers</em> tree.
     *
     * @param flamegraphRoot The flamegraphRoot Node
     * @param frameSeparator The frame separator
     * @param nodeSelector   The node selector
     * @return a tree representing the predecessors of the frame
     */
    private static Node computePredecessorsTree(
            org.openjdk.jmc.flightrecorder.stacktrace.tree.Node flamegraphRoot,
            FrameSeparator frameSeparator,
            Predicate<AggregatableFrame> nodeSelector
    ) {
        AggregatableFrame rootFrame = new AggregatableFrame(frameSeparator, ROOT_FRAME);
        Node predecessorsRoot = Node.newRootNode(rootFrame);

        findPredecessors(predecessorsRoot, flamegraphRoot, nodeSelector);

        return predecessorsRoot;
    }

    private static void findPredecessors(
            Node predecessorsRoot,
            org.openjdk.jmc.flightrecorder.stacktrace.tree.Node node,
            Predicate<AggregatableFrame> nodeSelector
    ) {
        // if there's a match add children nodes
        if (nodeSelector.test(node.getFrame())) {
            Node predecessorsRootNode = getOrCreateFocusedMethodNode(predecessorsRoot, node.getFrame());

            predecessorsRootNode.weight += node.getWeight();
            predecessorsRootNode.cumulativeWeight += node.getCumulativeWeight();

            // Adds and merge predecessors in the callers tree
            Node predecessorNode = predecessorsRootNode;
            for (
                    org.openjdk.jmc.flightrecorder.stacktrace.tree.Node currentNode = node.getParent();
                    currentNode != null && currentNode.getParent() != null;
                    currentNode = currentNode.getParent()
            ) {
                if (currentNode.getParent().isRoot()) {
                    continue;
                }

                Node child = getOrCreateNode(predecessorNode, currentNode.getFrame());
                child.weight += currentNode.getWeight();
                child.cumulativeWeight += currentNode.getCumulativeWeight();
                predecessorNode = child;
            }
        }

        // regardless walk the current tree for matching nodes in children
        for (org.openjdk.jmc.flightrecorder.stacktrace.tree.Node child : node.getChildren()) {
            findPredecessors(predecessorsRoot, child, nodeSelector);
        }
    }

    /**
     * Compute the successor frames, aka <em>callees</em> tree.
     *
     * @param flamegraphRoot The flamegraphRoot Node
     * @param frameSeparator The frame separator
     * @param nodeSelector   The node selector
     * @return a tree representing the successors of the frame
     */
    private static Node computeSuccessorsTree(
            org.openjdk.jmc.flightrecorder.stacktrace.tree.Node flamegraphRoot,
            FrameSeparator frameSeparator, Predicate<AggregatableFrame> nodeSelector
    ) {
        AggregatableFrame rootFrame = new AggregatableFrame(frameSeparator, ROOT_FRAME);
        Node root = Node.newRootNode(rootFrame);
        findSuccessors(root, flamegraphRoot, nodeSelector);
        mergeChildren(root);
        return root;
    }

    private static void findSuccessors(
            Node successorsRoot,
            org.openjdk.jmc.flightrecorder.stacktrace.tree.Node node,
            Predicate<AggregatableFrame> nodeSelector
    ) {
        // if there's a match add children nodes
        if (nodeSelector.test(node.getFrame())) {
            Node successorsRootNode = getOrCreateFocusedMethodNode(successorsRoot, node.getFrame());
            successorsRootNode.weight += node.getWeight();
            successorsRootNode.cumulativeWeight += node.getCumulativeWeight();
            convertAndAddChildren(successorsRootNode, node);
        }
        // regardless look for matching nodes in children
        for (org.openjdk.jmc.flightrecorder.stacktrace.tree.Node child : node.getChildren()) {
            findSuccessors(successorsRoot, child, nodeSelector);
        }
    }

    private static Node getOrCreateFocusedMethodNode(Node parent, AggregatableFrame frame) {
        if (!parent.children.isEmpty() && !parent.children.get(0).getFrame().equals(frame)) {
            throw new IllegalArgumentException("frameSelector matched more than one frame");
        }
        return getOrCreateNode(parent, frame);
    }

    private static Node getOrCreateNode(Node parent, AggregatableFrame frame) {
        return parent.children.stream()
                // TODO: consider a map lookup instead of linear search
                .filter(child -> child.getFrame().equals(frame))
                .findAny()
                .orElseGet(() -> {
                    Node result = new Node(parent, frame);
                    parent.children.add(result);
                    return result;
                });
    }

    /**
     * Merge children with the same {@link AggregatableFrame}.
     *
     * @param node The node to process
     */
    private static void mergeChildren(Node node) {
        Map<AggregatableFrame, Node> childrenMap = new HashMap<>();
        for (Node child : node.getChildren()) {
            childrenMap.merge(
                    child.getFrame(),
                    child,
                    (accumulatorNode, nodeToMerge) -> {
                        accumulatorNode.cumulativeWeight += child.cumulativeWeight;
                        accumulatorNode.weight += child.weight;
                        accumulatorNode.children.addAll(nodeToMerge.getChildren());
                        return accumulatorNode;
                    }
            );
        }
        for (Node child : childrenMap.values()) {
            mergeChildren(child);
        }
        node.children.clear();
        node.children.addAll(childrenMap.values());
    }

    private static void convertAndAddChildren(Node successorsRootNode, org.openjdk.jmc.flightrecorder.stacktrace.tree.Node node) {
        List<Node> list = new ArrayList<>();
        for (org.openjdk.jmc.flightrecorder.stacktrace.tree.Node child : node.getChildren()) {
            Node jmcTreeNode = toJmcTreeNode(successorsRootNode, child);
            list.add(jmcTreeNode);
        }
        successorsRootNode.children.addAll(list);
    }

    private static Node toJmcTreeNode(Node convertedParent, org.openjdk.jmc.flightrecorder.stacktrace.tree.Node node) {
        Node converted = new Node(
                convertedParent,
                node.getFrame()
        );
        converted.cumulativeWeight = node.getCumulativeWeight();
        converted.weight = node.getCumulativeWeight();

        convertAndAddChildren(converted, node);
        return converted;
    }

    public Node getSuccessorsRoot() {
        return successorsRoot;
    }

    public Node getPredecessorsRoot() {
        return predecessorsRoot;
    }

    public AggregatableFrame getFocusedMethod() {
        return successorsRoot.getFrame();
    }

    public IItemCollection getItems() {
        return items;
    }

    public IAttribute<IQuantity> getAttribute() {
        return attribute;
    }

    public FrameSeparator getFrameSeparator() {
        return frameSeparator;
    }

    private static abstract class StacktraceTreeModelAccessor {
        private final static Field frameSeparatorField;
        private final static Field invertedStacksField;

        static {
            try {
                frameSeparatorField = StacktraceTreeModel.class.getDeclaredField("frameSeparator");
                frameSeparatorField.setAccessible(true);
                invertedStacksField = StacktraceTreeModel.class.getDeclaredField("invertedStacks");
                invertedStacksField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Fields have changed in 'StacktraceTreeModel'", e);
            }
        }

        static FrameSeparator getFrameSeparator(StacktraceTreeModel model) {
            try {
                return (FrameSeparator) frameSeparatorField.get(model);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("StacktraceTreeModel.frameSeparator field cannot be accessed", e);
            }
        }

        static boolean isInvertedStacks(StacktraceTreeModel model) {
            try {
                return (boolean) invertedStacksField.get(model);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("StacktraceTreeModel.invertedStacks field cannot be accessed", e);
            }
        }
    }
}