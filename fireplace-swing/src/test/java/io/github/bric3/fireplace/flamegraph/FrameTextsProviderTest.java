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

import io.github.bric3.fireplace.core.ui.StringClipper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FrameTextsProvider}.
 * These tests run in headless mode without requiring a display.
 */
@DisplayName("FrameTextsProvider")
class FrameTextsProviderTest {

    @Nested
    @DisplayName("of factory method")
    class OfFactoryMethod {

        @Test
        void withList_createsProvider() {
            Function<FrameBox<String>, String> func = frame -> frame.actualNode;
            List<Function<FrameBox<String>, String>> funcs = List.of(func);

            FrameTextsProvider<String> provider = FrameTextsProvider.of(funcs);

            assertThat(provider.frameToTextCandidates()).containsExactly(func);
        }

        @Test
        void withVarargs_createsProvider() {
            Function<FrameBox<String>, String> func1 = frame -> frame.actualNode;
            Function<FrameBox<String>, String> func2 = frame -> "prefix-" + frame.actualNode;

            FrameTextsProvider<String> provider = FrameTextsProvider.of(func1, func2);

            assertThat(provider.frameToTextCandidates()).hasSize(2);
        }

        @Test
        void singleFunction_createsProvider() {
            Function<FrameBox<String>, String> func = frame -> frame.actualNode.toUpperCase();

            FrameTextsProvider<String> provider = FrameTextsProvider.of(func);

            assertThat(provider.frameToTextCandidates()).hasSize(1);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 0);
            String result = provider.frameToTextCandidates().get(0).apply(frame);
            assertThat(result).isEqualTo("TEST");
        }

        @Test
        void withEmptyList_returnsEmptyProvider() {
            FrameTextsProvider<String> provider = FrameTextsProvider.of(List.of());

            assertThat(provider.frameToTextCandidates()).isEmpty();
        }

        @Test
        void preservesOrderOfFunctions() {
            Function<FrameBox<String>, String> first = frame -> "1";
            Function<FrameBox<String>, String> second = frame -> "2";
            Function<FrameBox<String>, String> third = frame -> "3";

            FrameTextsProvider<String> provider = FrameTextsProvider.of(first, second, third);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 0);
            List<Function<FrameBox<String>, String>> candidates = provider.frameToTextCandidates();

            assertThat(candidates.get(0).apply(frame)).isEqualTo("1");
            assertThat(candidates.get(1).apply(frame)).isEqualTo("2");
            assertThat(candidates.get(2).apply(frame)).isEqualTo("3");
        }
    }

    @Nested
    @DisplayName("empty factory method")
    class EmptyFactoryMethod {

        @Test
        void returnsEmptyProvider() {
            FrameTextsProvider<String> provider = FrameTextsProvider.empty();

            assertThat(provider.frameToTextCandidates()).isEmpty();
        }

        @Test
        void multipleCallsReturnSameStructure() {
            FrameTextsProvider<String> provider1 = FrameTextsProvider.empty();
            FrameTextsProvider<String> provider2 = FrameTextsProvider.empty();

            // Both should return empty lists
            assertThat(provider1.frameToTextCandidates()).isEmpty();
            assertThat(provider2.frameToTextCandidates()).isEmpty();
        }
    }

    @Nested
    @DisplayName("clipStrategy method")
    class ClipStrategyMethod {

        @Test
        void defaultsToRight() {
            FrameTextsProvider<String> provider = FrameTextsProvider.of(frame -> frame.actualNode);

            assertThat(provider.clipStrategy()).isEqualTo(StringClipper.RIGHT);
        }

        @Test
        void canBeOverridden() {
            FrameTextsProvider<String> provider = new FrameTextsProvider<>() {
                @Override
                public List<Function<FrameBox<String>, String>> frameToTextCandidates() {
                    return List.of(frame -> frame.actualNode);
                }

                @Override
                public StringClipper clipStrategy() {
                    return StringClipper.NONE;
                }
            };

            assertThat(provider.clipStrategy()).isEqualTo(StringClipper.NONE);
        }
    }

    @Nested
    @DisplayName("frameToTextCandidates method")
    class FrameToTextCandidatesMethod {

        @Test
        void functionsAreApplicable() {
            Function<FrameBox<String>, String> nameFunc = frame -> frame.actualNode;
            Function<FrameBox<String>, String> shortFunc = frame -> frame.actualNode.substring(0, Math.min(3, frame.actualNode.length()));
            Function<FrameBox<String>, String> depthFunc = frame -> "depth:" + frame.stackDepth;

            FrameTextsProvider<String> provider = FrameTextsProvider.of(nameFunc, shortFunc, depthFunc);
            FrameBox<String> frame = new FrameBox<>("HelloWorld", 0.25, 0.75, 5);

            List<Function<FrameBox<String>, String>> candidates = provider.frameToTextCandidates();

            assertThat(candidates.get(0).apply(frame)).isEqualTo("HelloWorld");
            assertThat(candidates.get(1).apply(frame)).isEqualTo("Hel");
            assertThat(candidates.get(2).apply(frame)).isEqualTo("depth:5");
        }
    }

    @Nested
    @DisplayName("functional interface implementation")
    class FunctionalInterface {

        @Test
        void canBeImplementedAsLambda() {
            // FrameTextsProvider is a functional interface
            FrameTextsProvider<String> provider = () -> List.of(
                    frame -> frame.actualNode,
                    frame -> frame.actualNode.substring(0, 1)
            );

            assertThat(provider.frameToTextCandidates()).hasSize(2);
            assertThat(provider.clipStrategy()).isEqualTo(StringClipper.RIGHT); // default
        }
    }

    @Nested
    @DisplayName("complex node types")
    class ComplexNodeTypes {

        @Test
        void providerWithComplexNodeType() {
            // Test with a more complex node type
            FrameTextsProvider<MethodInfo> provider = FrameTextsProvider.of(
                    frame -> frame.actualNode.className + "." + frame.actualNode.methodName,
                    frame -> frame.actualNode.methodName,
                    frame -> frame.actualNode.className.substring(frame.actualNode.className.lastIndexOf('.') + 1) + "." + frame.actualNode.methodName
            );

            MethodInfo method = new MethodInfo("com.example.MyClass", "doSomething");
            FrameBox<MethodInfo> frame = new FrameBox<>(method, 0.0, 1.0, 0);

            List<Function<FrameBox<MethodInfo>, String>> candidates = provider.frameToTextCandidates();

            assertThat(candidates.get(0).apply(frame)).isEqualTo("com.example.MyClass.doSomething");
            assertThat(candidates.get(1).apply(frame)).isEqualTo("doSomething");
            assertThat(candidates.get(2).apply(frame)).isEqualTo("MyClass.doSomething");
        }
    }

    // Helper class for testing
    static class MethodInfo {
        final String className;
        final String methodName;

        MethodInfo(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }
    }
}
