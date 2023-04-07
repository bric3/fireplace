package io.github.bric3.fireplace.jfr.tree;

import org.openjdk.jmc.flightrecorder.stacktrace.tree.AggregatableFrame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Unmodified copy of {@link org.openjdk.jmc.flightrecorder.stacktrace.tree.Node} as some
 * of the methods are not accessible from a different package.
 */
public final class Node {
    /**
     * Integer uniquely identifying this node within our data structure.
     */
    private final Integer nodeId;

    /**
     * The frame associated with this node.
     */
    private final AggregatableFrame frame;

    /**
     * The weight when being the top frame.
     */
    double weight;

    /**
     * The parent node; null when root.
     */
    Node parent;

    /**
     * The child nodes; empty when leaf.
     */
    final List<Node> children = new ArrayList<>();

    /**
     * The cumulative weight for all contributions.
     */
    double cumulativeWeight;

    public static Node newRootNode(AggregatableFrame rootFrame) {
        return new Node(null, rootFrame);
    }

    public Node(Node parent, AggregatableFrame frame) {
        this.nodeId = computeNodeId(parent, frame);
        this.parent = parent;
        this.frame = frame;
        if (frame == null) {
            throw new NullPointerException("Frame cannot be null!");
        }
    }

    private static Integer computeNodeId(Node parent, AggregatableFrame frame) {
        return Objects.hash(parent != null ? parent.getNodeId() : null, frame.hashCode());
    }

    /**
     * @return the unique identifier associated with this node.
     */
    public Integer getNodeId() {
        return nodeId;
    }

    /**
     * @return the weight of this node.
     */
    public double getWeight() {
        return weight;
    }

    /**
     * @return the cumulative weight of this node.
     */
    public double getCumulativeWeight() {
        return cumulativeWeight;
    }

    /**
     * @return the frame corresponding to this node.
     */
    public AggregatableFrame getFrame() {
        return frame;
    }

    /**
     * @return the list of child nodes, in order of appearance.
     */
    public List<Node> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * @return the parent node or null when root.
     */
    public Node getParent() {
        return parent;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    @Override
    public int hashCode() {
        // This will get a few extra collisions.
        return frame.getMethod().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Node other = (Node) obj;

        return Objects.equals(nodeId, other.nodeId) && Objects.equals(frame, other.frame) && weight == other.weight
                && cumulativeWeight == other.cumulativeWeight;
    }

    @Override
    public String toString() {
        return String.format("%s %.2f (%.2f)", frame.getHumanReadableShortString(), weight, cumulativeWeight);
    }
}
