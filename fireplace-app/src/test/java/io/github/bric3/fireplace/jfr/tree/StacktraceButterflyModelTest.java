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
        // io.github.bric3.fireplace.jfr.tree.StupidMain.work()
        StacktraceButterflyModel butterfly = StacktraceButterflyModel.from(
                execSampleTreeModel,
                frame -> frame.getMethod().getMethodName().equals("work")
        );

        Node predecessorsRoot = butterfly.getPredecessorsRoot().getChildren().get(0);

        assertChildren(
                predecessorsRoot,
                main -> {
                    assertMethodName(main, "StupidMain.main");
                    assertChildren(main);
                },
                qux -> {
                    assertMethodName(qux, "StupidMain$C.qux");
                    assertChildren(
                            qux,
                            bar -> {
                                assertMethodName(bar, "StupidMain$B.bar");
                                assertChildren(
                                        bar,
                                        foo -> {
                                            assertMethodName(foo, "StupidMain$A.foo");
                                            assertChildren(
                                                    foo,
                                                    main -> {
                                                        assertMethodName(main, "StupidMain.main");
                                                        assertChildren(main);
                                                    }
                                            );
                                        }
                                );
                            },
                            zap -> {
                                assertMethodName(zap, "StupidMain$Z.zap");
                                assertChildren(
                                        zap,
                                        main -> {
                                            assertMethodName(main, "StupidMain.main");
                                            assertChildren(main);
                                        }
                                );
                            }
                    );
                },
                bar -> {
                    assertMethodName(bar, "StupidMain$B.bar");
                    assertChildren(
                            bar,
                            foo -> {
                                assertMethodName(foo, "StupidMain$A.foo");
                                assertChildren(
                                        foo,
                                        main -> {
                                            assertMethodName(main, "StupidMain.main");
                                            assertChildren(main);
                                        }
                                );
                            }
                    );
                },
                foo -> {
                    assertMethodName(foo, "StupidMain$A.foo");
                    assertChildren(
                            foo,
                            main -> {
                                assertMethodName(main, "StupidMain.main");
                                assertChildren(main);
                            }
                    );
                },
                zap -> {
                    assertMethodName(zap, "StupidMain$Z.zap");
                    assertChildren(
                            zap,
                            main -> {
                                assertMethodName(main, "StupidMain.main");
                                assertChildren(main);
                            }
                    );
                }
        );
    }

    @Test
    void should_produce_successors_from_StacktraceTreeModel() {
        // io.github.bric3.fireplace.jfr.tree.StupidMain.work()
        StacktraceButterflyModel butterfly = StacktraceButterflyModel.from(
                execSampleTreeModel,
                frame -> frame.getMethod().getMethodName().equals("work")
        );

        // [
        //   Random.nextInt() 8.00 (8.00),
        //   MessageDigest.update(byte) 240.00 (240.00),
        //   MessageDigest.update(byte) 239.00 (239.00),
        //   Random.nextInt() 2.00 (2.00),
        //   MessageDigest.update(byte) 249.00 (249.00),
        //   Random.nextInt() 5.00 (5.00),
        //   MessageDigest.update(byte) 248.00 (248.00),
        //   Random.nextInt() 4.00 (4.00),
        //   MessageDigest.update(byte) 246.00 (246.00),
        //   Random.nextInt() 3.00 (3.00),
        //   MessageDigest.update(byte) 242.00 (242.00),
        //   Random.nextInt() 1.00 (1.00)
        // ]

        assertEquivalentTree(
                butterfly.getSuccessorsRoot(),
                node("StupidMain.work", 49.00, 1536.00,
                        node("Random.nextInt",
                                8.00 + 2.00 + 5.00 + 4.00 + 3.00 + 1.00,
                                8.00 + 2.00 + 5.00 + 4.00 + 3.00 + 1.00,
                                node("Random.next", 22.0, 22.0,
                                        node("AtomicLong.compareAndSet", 14.0, 14.0)
                                )
                        ),
                        node("MessageDigest.update",
                                240.00 + 239.00 + 249.00 + 248.00 + 246.00 + 242.00,
                                240.00 + 239.00 + 249.00 + 248.00 + 246.00 + 242.00,
                                node("MessageDigest$Delegate.engineUpdate", 1461.00, 1461.00,
                                        node("DigestBase.engineUpdate", 1399.00, 1399.00,
                                                node("DigestBase.engineUpdate",
                                                        1356.00,
                                                        1356.00,
                                                        node("Preconditions.checkFromIndexSize", 21.00, 21.00),
                                                        node("SHA2.implCompress", 853.00, 853.00, count(2)),
                                                        node("SHA2.implCompress0", 3.00, 3.00)
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

    private static void assertChildrenSize(Node node, int expected) {
        assertEquals(expected, node.getChildren().size());
    }

    private static void assertMethodName(Node node, String typeAndMethodName) {
        int dotPos = typeAndMethodName.indexOf('.');

        if (dotPos < 0) {
            throw new IllegalArgumentException("typeAndMethodName should look like 'Type.method'");
        }
        String className = typeAndMethodName.substring(0, dotPos);
        assertEquals(className, node.getFrame().getMethod().getType().getTypeName());
        assertEquals(typeAndMethodName.substring(dotPos + 1), node.getFrame().getMethod().getMethodName());
    }
}