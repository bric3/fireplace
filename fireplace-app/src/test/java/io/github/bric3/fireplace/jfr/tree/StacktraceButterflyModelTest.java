package io.github.bric3.fireplace.jfr.tree;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        var butterfly = new StacktraceButterflyModel(
                execSampleTreeModel,
                frame -> frame.getMethod().getMethodName().equals("work")
        );

        Node predecessorsRoot = butterfly.getPredecessorsRoot();
        Node successorsRoot = butterfly.getSuccessorsRoot();

        assertEquals(predecessorsRoot.getFrame(), successorsRoot.getFrame());
        assertMethodName(predecessorsRoot, "StupidMain.work");
    }

    @Test
    void should_produce_predecessors_from_StacktraceTreeModel() {
        // io.github.bric3.fireplace.jfr.tree.StupidMain.work()
        var butterfly = new StacktraceButterflyModel(
                execSampleTreeModel,
                frame -> frame.getMethod().getMethodName().equals("work")
        );

        Node predecessorsRoot = butterfly.getPredecessorsRoot();

        assertChildren(
                predecessorsRoot,
                main -> {
                    assertMethodName(main, "StupidMain.main");
                    assertNoChildren(main);
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
                                                        assertNoChildren(main);
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
                                            assertNoChildren(main);
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
                                            assertNoChildren(main);
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
                                assertNoChildren(main);
                            }
                    );
                },
                zap -> {
                    assertMethodName(zap, "StupidMain$Z.zap");
                    assertChildren(
                            zap,
                            main -> {
                                assertMethodName(main, "StupidMain.main");
                                assertNoChildren(main);
                            }
                    );
                }
        );
    }

    @Test
    void should_produce_successors_from_StacktraceTreeModel() {
        // io.github.bric3.fireplace.jfr.tree.StupidMain.work()
        var butterfly = new StacktraceButterflyModel(
                execSampleTreeModel,
                frame -> frame.getMethod().getMethodName().equals("work")
        );

        Node successorsRoot = butterfly.getSuccessorsRoot();
        assertEquals(49.00, successorsRoot.getWeight());
        assertEquals(1_536.00, successorsRoot.getCumulativeWeight());

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

        assertChildren(
                successorsRoot,
                nextInt -> {
                    assertMethodName(nextInt, "Random.nextInt");
                    assertEquals(8.00 + 2.00 + 5.00 + 4.00 + 3.00 + 1.00, nextInt.weight);
                    assertEquals(8.00 + 2.00 + 5.00 + 4.00 + 3.00 + 1.00, nextInt.cumulativeWeight);
                    assertChildren(
                            nextInt,
                            next -> {
                                assertMethodName(next, "Random.next");
                                assertEquals(22.00, next.weight);
                                assertEquals(22.00, next.cumulativeWeight);
                                assertChildren(
                                        next,
                                        compareAndSet -> {
                                            assertMethodName(compareAndSet, "AtomicLong.compareAndSet");
                                            assertEquals(14.00, compareAndSet.weight);
                                            assertEquals(14.00, compareAndSet.cumulativeWeight);
                                            assertNoChildren(compareAndSet);
                                        }
                                );
                            }
                    );
                },
                update -> {
                    assertMethodName(update, "MessageDigest.update");
                    assertEquals(240.00 + 239.00 + 249.00 + 248.00 + 246.00 + 242.00, update.weight);
                    assertEquals(240.00 + 239.00 + 249.00 + 248.00 + 246.00 + 242.00, update.cumulativeWeight);
                    assertChildren(
                            update,
                            engineUpdate -> {
                                assertMethodName(engineUpdate, "MessageDigest$Delegate.engineUpdate");
                                assertEquals(1461.00, engineUpdate.weight);
                                assertEquals(1461.00, engineUpdate.cumulativeWeight);
                                assertChildren(
                                        engineUpdate,
                                        DBengineUpdate -> {
                                            assertMethodName(DBengineUpdate, "DigestBase.engineUpdate");
                                            assertEquals(1399.00, DBengineUpdate.weight);
                                            assertEquals(1399.00, DBengineUpdate.cumulativeWeight);
                                            assertChildren(
                                                    DBengineUpdate,
                                                    DBengineUpdate1 -> {
                                                        assertMethodName(DBengineUpdate1, "DigestBase.engineUpdate");
                                                        assertEquals(1356.00, DBengineUpdate1.weight);
                                                        assertEquals(1356.00, DBengineUpdate1.cumulativeWeight);
                                                        assertChildren(
                                                                DBengineUpdate1,
                                                                implCompress -> {
                                                                    assertMethodName(implCompress, "Preconditions.checkFromIndexSize");
                                                                    assertEquals(21.00, implCompress.weight);
                                                                    assertEquals(21.00, implCompress.cumulativeWeight);
                                                                    assertNoChildren(implCompress);
                                                                },
                                                                implCompress -> {
                                                                    assertMethodName(implCompress, "SHA2.implCompress");
                                                                    assertEquals(853.00, implCompress.weight);
                                                                    assertEquals(853.00, implCompress.cumulativeWeight);
                                                                    assertChildrenSize(implCompress, 2);
                                                                },
                                                                implCompress0 -> {
                                                                    assertMethodName(implCompress0, "SHA2.implCompress0");
                                                                    assertEquals(3.00, implCompress0.weight);
                                                                    assertEquals(3.00, implCompress0.cumulativeWeight);
                                                                    assertNoChildren(implCompress0);
                                                                }
                                                        );
                                                    }
                                            );
                                        }
                                );
                            }
                    );
                }
        );
    }


    private static void assertNoChildren(Node main) {
        assertChildrenSize(main, 0);
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