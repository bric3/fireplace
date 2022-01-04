package com.github.bric3;

public class FlameNode<T> {
    final T jfrNode;
    final double startX;
    final double endX;
    final int stackDepth;

    public FlameNode(T jfrNode, double startX, double endX, int stackDepth) {
        this.jfrNode = jfrNode;
        this.startX = startX;
        this.endX = endX;
        this.stackDepth = stackDepth;
    }

    @Override
    public String toString() {
        return "FlameNode{" +
               "startX=" + startX +
               ", endX=" + endX +
               ", depth=" + stackDepth +
               '}';
    }
}
