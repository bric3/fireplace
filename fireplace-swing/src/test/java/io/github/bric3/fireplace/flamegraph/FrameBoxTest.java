/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.flamegraph;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FrameBox}.
 * These tests run in headless mode without requiring a display.
 */
@DisplayName("FrameBox")
class FrameBoxTest {

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        void constructor_validParameters_createsInstance() {
            var frame = new FrameBox<>("node", 0.0, 1.0, 0);

            assertThat(frame.actualNode).isEqualTo("node");
            assertThat(frame.startX).isEqualTo(0.0);
            assertThat(frame.endX).isEqualTo(1.0);
            assertThat(frame.stackDepth).isEqualTo(0);
        }

        @Test
        void constructor_partialWidth_createsInstance() {
            var frame = new FrameBox<>("node", 0.25, 0.75, 3);

            assertThat(frame.startX).isEqualTo(0.25);
            assertThat(frame.endX).isEqualTo(0.75);
            assertThat(frame.stackDepth).isEqualTo(3);
        }

        @Test
        void constructor_nullNode_throwsNullPointerException() {
            assertThatThrownBy(() -> new FrameBox<>(null, 0.0, 1.0, 0))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @CsvSource({
                "-0.1, 1.0",    // startX < 0
                "0.0, 1.1",     // endX > 1
                "0.6, 0.5",     // startX > endX
                "-0.5, -0.1",   // both negative
                "1.5, 2.0",     // both > 1
        })
        void constructor_invalidCoordinates_throwsIllegalArgumentException(double startX, double endX) {
            assertThatThrownBy(() -> new FrameBox<>("node", startX, endX, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid frame coordinates");
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, -100, Integer.MIN_VALUE})
        void constructor_negativeStackDepth_throwsIllegalArgumentException(int depth) {
            assertThatThrownBy(() -> new FrameBox<>("node", 0.0, 1.0, depth))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid stack depth");
        }

        @Test
        void constructor_zeroWidthFrame_isValid() {
            // A frame with startX == endX is valid (zero width)
            var frame = new FrameBox<>("node", 0.5, 0.5, 1);

            assertThat(frame.startX).isEqualTo(frame.endX);
        }
    }

    @Nested
    @DisplayName("isRoot")
    class IsRootTests {

        @Test
        void isRoot_depthZero_returnsTrue() {
            var frame = new FrameBox<>("root", 0.0, 1.0, 0);

            assertThat(frame.isRoot()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 10, 100})
        void isRoot_depthNonZero_returnsFalse(int depth) {
            var frame = new FrameBox<>("child", 0.0, 1.0, depth);

            assertThat(frame.isRoot()).isFalse();
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        void toString_containsRelevantInfo() {
            var frame = new FrameBox<>("node", 0.25, 0.75, 5);

            var str = frame.toString();

            assertThat(str)
                    .contains("startX=0.25")
                    .contains("endX=0.75")
                    .contains("depth=5");
        }
    }

    @Nested
    @DisplayName("flattenAndCalculateCoordinate")
    class FlattenAndCalculateCoordinateTests {

        @Test
        void flattenAndCalculateCoordinate_singleNode_createsOneFrame() {
            var accumulator = new ArrayList<FrameBox<TestNode>>();
            var root = new TestNode("root", 100, List.of());

            FrameBox.flattenAndCalculateCoordinate(
                    accumulator,
                    root,
                    TestNode::children,
                    TestNode::weight,
                    0.0,
                    1.0,
                    0
            );

            assertThat(accumulator).hasSize(1);
            assertThat(accumulator.get(0).actualNode).isEqualTo(root);
            assertThat(accumulator.get(0).startX).isEqualTo(0.0);
            assertThat(accumulator.get(0).endX).isEqualTo(1.0);
            assertThat(accumulator.get(0).stackDepth).isEqualTo(0);
        }

        @Test
        void flattenAndCalculateCoordinate_twoChildren_proportionalWidths() {
            var child1 = new TestNode("child1", 30, List.of());
            var child2 = new TestNode("child2", 70, List.of());
            var root = new TestNode("root", 100, List.of(child1, child2));

            var accumulator = new ArrayList<FrameBox<TestNode>>();
            FrameBox.flattenAndCalculateCoordinate(
                    accumulator,
                    root,
                    TestNode::children,
                    TestNode::weight,
                    0.0,
                    1.0,
                    0
            );

            assertThat(accumulator).hasSize(3);

            // Root spans full width
            var rootFrame = accumulator.get(0);
            assertThat(rootFrame.actualNode.name).isEqualTo("root");
            assertThat(rootFrame.startX).isEqualTo(0.0);
            assertThat(rootFrame.endX).isEqualTo(1.0);
            assertThat(rootFrame.stackDepth).isEqualTo(0);

            // Child1 spans 30% (0.0 to 0.3)
            var child1Frame = accumulator.get(1);
            assertThat(child1Frame.actualNode.name).isEqualTo("child1");
            assertThat(child1Frame.startX).isEqualTo(0.0);
            assertThat(child1Frame.endX).isCloseTo(0.3, org.assertj.core.api.Assertions.within(0.0001));
            assertThat(child1Frame.stackDepth).isEqualTo(1);

            // Child2 spans 70% (0.3 to 1.0)
            var child2Frame = accumulator.get(2);
            assertThat(child2Frame.actualNode.name).isEqualTo("child2");
            assertThat(child2Frame.startX).isCloseTo(0.3, org.assertj.core.api.Assertions.within(0.0001));
            assertThat(child2Frame.endX).isEqualTo(1.0);
            assertThat(child2Frame.stackDepth).isEqualTo(1);
        }

        @Test
        void flattenAndCalculateCoordinate_deepTree_correctDepths() {
            var leaf = new TestNode("leaf", 10, List.of());
            var child = new TestNode("child", 10, List.of(leaf));
            var root = new TestNode("root", 10, List.of(child));

            var accumulator = new ArrayList<FrameBox<TestNode>>();
            FrameBox.flattenAndCalculateCoordinate(
                    accumulator,
                    root,
                    TestNode::children,
                    TestNode::weight,
                    0.0,
                    1.0,
                    0
            );

            assertThat(accumulator).hasSize(3);
            assertThat(accumulator.get(0).stackDepth).isEqualTo(0); // root
            assertThat(accumulator.get(1).stackDepth).isEqualTo(1); // child
            assertThat(accumulator.get(2).stackDepth).isEqualTo(2); // leaf
        }

        @Test
        void flattenAndCalculateCoordinate_dfsOrder() {
            // Tree structure:
            //        root
            //       /    \
            //      A      B
            //     / \
            //    C   D
            var c = new TestNode("C", 10, List.of());
            var d = new TestNode("D", 10, List.of());
            var a = new TestNode("A", 20, List.of(c, d));
            var b = new TestNode("B", 30, List.of());
            var root = new TestNode("root", 50, List.of(a, b));

            var accumulator = new ArrayList<FrameBox<TestNode>>();
            FrameBox.flattenAndCalculateCoordinate(
                    accumulator,
                    root,
                    TestNode::children,
                    TestNode::weight,
                    0.0,
                    1.0,
                    0
            );

            // DFS order: root -> A -> C -> D -> B
            assertThat(accumulator).extracting(f -> f.actualNode.name)
                    .containsExactly("root", "A", "C", "D", "B");
        }

        @Test
        void flattenAndCalculateCoordinate_partialRange_respectsBounds() {
            var child = new TestNode("child", 10, List.of());
            var root = new TestNode("root", 10, List.of(child));

            var accumulator = new ArrayList<FrameBox<TestNode>>();
            // Only span 0.2 to 0.6
            FrameBox.flattenAndCalculateCoordinate(
                    accumulator,
                    root,
                    TestNode::children,
                    TestNode::weight,
                    0.2,
                    0.6,
                    0
            );

            assertThat(accumulator.get(0).startX).isEqualTo(0.2);
            assertThat(accumulator.get(0).endX).isEqualTo(0.6);
            assertThat(accumulator.get(1).startX).isEqualTo(0.2);
            assertThat(accumulator.get(1).endX).isEqualTo(0.6);
        }

        @Test
        void flattenAndCalculateCoordinate_withSeparateWeightFunctions() {
            var child1 = new TestNode("child1", 25, List.of());
            var child2 = new TestNode("child2", 75, List.of());
            // Total weight is different from sum of children
            var root = new TestNode("root", 200, List.of(child1, child2));

            var accumulator = new ArrayList<FrameBox<TestNode>>();
            FrameBox.flattenAndCalculateCoordinate(
                    accumulator,
                    root,
                    TestNode::children,
                    TestNode::weight,      // node weight
                    n -> 100.0,            // total weight (always 100 for testing)
                    0.0,
                    1.0,
                    0
            );

            // With total weight 100: child1 = 25/100 = 0.25, child2 = 75/100 = 0.75
            var child1Frame = accumulator.get(1);
            assertThat(child1Frame.startX).isEqualTo(0.0);
            assertThat(child1Frame.endX).isCloseTo(0.25, org.assertj.core.api.Assertions.within(0.0001));

            var child2Frame = accumulator.get(2);
            assertThat(child2Frame.startX).isCloseTo(0.25, org.assertj.core.api.Assertions.within(0.0001));
            assertThat(child2Frame.endX).isEqualTo(1.0);
        }

        @Test
        void flattenAndCalculateCoordinate_nullChildren_treatedAsEmpty() {
            var root = new TestNode("root", 100, null);

            var accumulator = new ArrayList<FrameBox<TestNode>>();
            FrameBox.flattenAndCalculateCoordinate(
                    accumulator,
                    root,
                    TestNode::children,
                    TestNode::weight,
                    0.0,
                    1.0,
                    0
            );

            assertThat(accumulator).hasSize(1);
        }

        @Test
        void flattenAndCalculateCoordinate_emptyChildren_onlyRoot() {
            var root = new TestNode("root", 100, List.of());

            var accumulator = new ArrayList<FrameBox<TestNode>>();
            FrameBox.flattenAndCalculateCoordinate(
                    accumulator,
                    root,
                    TestNode::children,
                    TestNode::weight,
                    0.0,
                    1.0,
                    0
            );

            assertThat(accumulator).hasSize(1);
        }
    }

    // Helper test node class
    static class TestNode {
        final String name;
        final double weight;
        final List<TestNode> children;

        TestNode(String name, double weight, List<TestNode> children) {
            this.name = name;
            this.weight = weight;
            this.children = children;
        }

        String name() {
            return name;
        }

        double weight() {
            return weight;
        }

        List<TestNode> children() {
            return children;
        }
    }
}
