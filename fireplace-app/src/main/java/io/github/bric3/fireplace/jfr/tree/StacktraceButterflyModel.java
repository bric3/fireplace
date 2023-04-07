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

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.AggregatableFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class StacktraceButterflyModel {
    private final Node predecessorsRoot;
    private final Node successorsRoot;
    private final IItemCollection items;
    private final IAttribute<IQuantity> attribute;

    public StacktraceButterflyModel(StacktraceTreeModel stacktraceTreeModel, Predicate<AggregatableFrame> nodeSelector) {
        Objects.requireNonNull(stacktraceTreeModel, "StacktraceTreeModel must not be null");
        Objects.requireNonNull(nodeSelector, "Node selector must not be null");

        items = stacktraceTreeModel.getItems();
        attribute = stacktraceTreeModel.getAttribute();

        predecessorsRoot = computePredecessorsTree(stacktraceTreeModel.getRoot(), nodeSelector);
        successorsRoot = computeSuccessorsTree(stacktraceTreeModel.getRoot(), nodeSelector);

        assert predecessorsRoot.getFrame().equals(successorsRoot.getFrame()) : "Predecessors and successors root should be the same frame";
    }

    /**
     * Compute the predecessor frames, aka <em>callers</em> tree.
     *
     * @param flamegraphRoot The flamegraphRoot Node
     * @param nodeSelector   The node selector
     * @return a tree representing the predecessors of the frame
     */
    private Node computePredecessorsTree(
            org.openjdk.jmc.flightrecorder.stacktrace.tree.Node flamegraphRoot,
            Predicate<AggregatableFrame> nodeSelector
    ) {
        AtomicReference<Node> predecessorsRoot = new AtomicReference<>();
        findPredecessors(predecessorsRoot, flamegraphRoot, nodeSelector);

        return predecessorsRoot.get();
    }

    private void findPredecessors(
            AtomicReference<Node> predecessorsRootRef,
            org.openjdk.jmc.flightrecorder.stacktrace.tree.Node node,
            Predicate<AggregatableFrame> nodeSelector
    ) {
        // if there's a match add children nodes
        if (nodeSelector.test(node.getFrame())) {
            predecessorsRootRef.compareAndSet(null, Node.newRootNode(node.getFrame()));

            Node predecessorsRootNode = predecessorsRootRef.get();
            predecessorsRootNode.weight += node.getWeight();
            predecessorsRootNode.cumulativeWeight += node.getCumulativeWeight();

            // Adds and merge predecessors in the callers tree
            Node predecessorNode = predecessorsRootNode;
            for (
                    org.openjdk.jmc.flightrecorder.stacktrace.tree.Node currentNode = node.getParent();
                    currentNode != null && currentNode.getParent() != null;
                    currentNode = currentNode.getParent()
            ) {
                Node child = getOrCreateNode(predecessorNode, currentNode.getFrame());
                child.weight += currentNode.getWeight();
                child.cumulativeWeight += currentNode.getCumulativeWeight();
                predecessorNode = child;
            }
        }

        // regardless walk the current tree for matching nodes in children
        for (org.openjdk.jmc.flightrecorder.stacktrace.tree.Node child : node.getChildren()) {
            findPredecessors(predecessorsRootRef, child, nodeSelector);
        }
    }
    private Node getOrCreateNode(Node parent, AggregatableFrame frame) {
        return parent.children.stream()
                // TODO: consider a map lookup instead of linear search
                .filter(child -> child.getFrame().equals(frame)).findAny().orElseGet(() -> {
                    Node result = new Node(parent, frame);
                    parent.children.add(result);
                    return result;
                });
    }


    /**
     * Compute the successor frames, aka <em>callees</em> tree.
     *
     * @param flamegraphRoot The flamegraphRoot Node
     * @param nodeSelector   The node selector
     * @return a tree representing the successors of the frame
     */
    private Node computeSuccessorsTree(
            org.openjdk.jmc.flightrecorder.stacktrace.tree.Node flamegraphRoot,
            Predicate<AggregatableFrame> nodeSelector
    ) {
        AtomicReference<Node> successorsRootRef = new AtomicReference<>();
        findSuccessors(successorsRootRef, flamegraphRoot, nodeSelector);
        mergeChildren(successorsRootRef.get());
        return successorsRootRef.get();
    }

    private void findSuccessors(
            AtomicReference<Node> successorsRootRef,
            org.openjdk.jmc.flightrecorder.stacktrace.tree.Node node,
            Predicate<AggregatableFrame> nodeSelector
    ) {
        // if there's a match add children nodes
        if (nodeSelector.test(node.getFrame())) {
            successorsRootRef.compareAndSet(null, Node.newRootNode(node.getFrame()));

            Node successorsRootNode = successorsRootRef.get();
            successorsRootNode.weight += node.getWeight();
            successorsRootNode.cumulativeWeight += node.getCumulativeWeight();
            convertAndAddChildren(successorsRootNode, node);
        }
        // regardless look for matching nodes in children
        for (org.openjdk.jmc.flightrecorder.stacktrace.tree.Node child : node.getChildren()) {
            findSuccessors(successorsRootRef, child, nodeSelector);
        }
    }

    /**
     * Merge children with the same {@link AggregatableFrame}.
     *
     * @param node The node to process
     */
    private void mergeChildren(Node node) {
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
}