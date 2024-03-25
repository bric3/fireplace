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

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class StacktraceButterflyModelTest {
    private static StacktraceTreeModel execSampleTreeModel;

    @BeforeAll
    static void loadEvents() throws CouldNotLoadRecordingException, IOException {
        InputStream resourceStream = StacktraceButterflyModelTest.class.getClassLoader().getResourceAsStream("StupidMain.jfr");
        IItemCollection iItemIterables = JfrLoaderToolkit.loadEvents(resourceStream);

        IItemCollection execSampleEvents = iItemIterables.apply(JdkFilters.EXECUTION_SAMPLE);
        execSampleTreeModel = new StacktraceTreeModel(execSampleEvents);
    }

    @Test
    void should_have_the_same_frame_for_predecessors_and_predecessor() {
        StacktraceButterflyModel butterfly = StacktraceButterflyModel.from(
                execSampleTreeModel,
                frame -> frame.getMethod().getMethodName().equals("work")
        );

        Node predecessorsRoot = butterfly.getPredecessorsRoot().getChildren().get(0);
        Node successorsRoot = butterfly.getSuccessorsRoot().getChildren().get(0); // TODO assert size

        assertEquals(predecessorsRoot.getFrame(), successorsRoot.getFrame());
        assertMethodName(predecessorsRoot, "StupidMain.work");
    }

    @Test
    void should_fail_and_report_when_selector_matches_more_thar_one_frame() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> StacktraceButterflyModel.from(
                        execSampleTreeModel,
                        frame -> frame.getMethod().getMethodName().equals("work") || frame.getMethod().getMethodName().equals("qux")
                ),
                "Selector should match only one frame, but matched: [StupidMain.work(), StupidMain$C.qux()]"
        );
    }

    @Test
    void should_fail_and_report_when_StacktraceTreeModel_has_inverted_stacks() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> StacktraceButterflyModel.from(
                        new StacktraceTreeModel(
                                execSampleTreeModel.getItems(),
                                /* DEFAULT_FRAME_SEPARATOR */ new FrameSeparator(FrameSeparator.FrameCategorization.METHOD, false),
                                /* inverted stack traces */ true
                        ),
                        frame -> frame.getMethod().getMethodName().equals("work") || frame.getMethod().getMethodName().equals("qux")
                ),
                "Selector should match only one frame, but matched: [StupidMain.work(), StupidMain$C.qux()]"
        );
    }

    @Test
    void should_produce_predecessors_from_StacktraceTreeModel() {
        StacktraceButterflyModel butterfly = StacktraceButterflyModel.from(
                execSampleTreeModel,
                frame -> frame.getMethod().getMethodName().equals("work")
        );

        assertEquivalentTree(
                butterfly.getPredecessorsRoot(),
                node("StupidMain.work", 1536.0, 1536.0,
                        node("StupidMain.main", 261.0, 1536.0),
                        node("StupidMain$C.qux", 500.0, 500.0,
                                node("StupidMain$B.bar", 246.0, 510.0,
                                        node("StupidMain$A.foo", 246.0, 771.0,
                                                node("StupidMain.main", 246.0, 1536.0)
                                        )
                                ),
                                node("StupidMain$Z.zap", 254.0, 504.0,
                                        node("StupidMain.main", 254.0, 1536.0)
                                )
                        ),
                        node("StupidMain$B.bar", 264.0, 510.0,
                                node("StupidMain$A.foo", 264.0, 771.0,
                                        node("StupidMain.main", 264.0, 1536.0)
                                )
                        ),
                        node("StupidMain$A.foo", 261.0, 771.0,
                                node("StupidMain.main", 261.0, 1536.0)
                        ),
                        node("StupidMain$Z.zap", 250.0, 504.0,
                                node("StupidMain.main", 250.0, 1536.0)
                        )
                )
        );
    }

    @Test
    void should_produce_successors_from_StacktraceTreeModel() {
        // io.github.bric3.fireplace.jfr.tree.StupidMain.work()
        StacktraceButterflyModel butterfly = StacktraceButterflyModel.from(
                execSampleTreeModel,
                frame -> frame.getMethod().getMethodName().equals("work")
        );

        assertEquivalentTree(
                butterfly.getSuccessorsRoot(),
                node("StupidMain.work", 49.0, 1536.0,
                        node("Random.nextInt",
                                23.0 /*8.0 + 2.0 + 5.0 + 4.0 + 3.0 + 1.0*/,
                                23.0 /*8.0 + 2.0 + 5.0 + 4.0 + 3.0 + 1.0*/,
                                node("Random.next", 22.0, 22.0,
                                        node("AtomicLong.compareAndSet", 14.0, 14.0)
                                )
                        ),
                        node("MessageDigest.update",
                                1464.0 /* 240.0 + 239.0 + 249.0 + 248.0 + 246.0 + 242.0 */,
                                1464.0 /* 240.0 + 239.0 + 249.0 + 248.0 + 246.0 + 242.0 */,
                                node("MessageDigest$Delegate.engineUpdate", 1461.0, 1461.0,
                                        node("DigestBase.engineUpdate", 1399.0, 1399.0,
                                                node("DigestBase.engineUpdate",
                                                        1356.0,
                                                        1356.0,
                                                        node("Preconditions.checkFromIndexSize", 21.0, 21.0),
                                                        node("SHA2.implCompress", 853.0, 853.0, count(2)),
                                                        node("SHA2.implCompress0", 3.0, 3.0)
                                                )
                                        )
                                )
                        )
                )
        );
    }

    @SafeVarargs
    private void assertEquivalentTree(Node successorsRoot, Consumer<Node>... node) {
        assertChildren(successorsRoot, node);
    }

    @SafeVarargs
    @NotNull
    private static Consumer<Node> node(
            String typeAndMethodName,
            double weight,
            double cumulativeWeight,
            Consumer<Node>... childrenAssertions
    ) {
        return node -> {
            assertMethodName(node, typeAndMethodName);
            assertEquals(/* expected */weight, node.weight);
            assertEquals(/* expected */cumulativeWeight, node.cumulativeWeight);
            assertChildren(node, childrenAssertions);
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Consumer<Node>[] count(int expected) {
        Consumer[] consumers = new Consumer[expected];
        Arrays.fill(consumers, (Consumer<Node>) node -> {});
        return consumers;
    }

    @SafeVarargs
    private static void assertChildren(Node node, Consumer<Node>... childrenAssertions) {
        for (int i = 0; i < childrenAssertions.length; i++) {
            childrenAssertions[i].accept(node.getChildren().get(i));
        }
        assertEquals(childrenAssertions.length, node.getChildren().size());
    }

    private static void assertMethodName(Node node, String typeAndMethod) {
        int dotPos = typeAndMethod.indexOf('.');

        if (dotPos < 0) {
            throw new IllegalArgumentException("typeAndMethod should look like 'Type.method' or 'Type$Inner.method'");
        }
        assertEquals(
                typeAndMethod,
                node.getFrame().getMethod().getType().getTypeName() + "." + node.getFrame().getMethod().getMethodName()
        );
    }

    private static void print(Node node) {
        System.out.println(treeToString("", node, true, new StringBuilder()));
    }

    private static CharSequence treeToString(String prefix, Node node, boolean isTail, StringBuilder outputBuilder) {
        outputBuilder.append(prefix).append(isTail ? "└── " : "├── ").append(node).append("\n");
        for (int i = 0; i < node.children.size() - 1; i++) {

            treeToString(prefix + (isTail ? "    " : "│   "), node.children.get(i), false, outputBuilder);
        }
        if (node.children.size() > 0) {
            treeToString(prefix + (isTail ? "    " : "│   "), node.children.get(node.children.size() - 1), true, outputBuilder);
        }
        return outputBuilder;
    }
}