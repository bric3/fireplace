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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FrameModel}.
 * These tests run in headless mode without requiring a display.
 */
@DisplayName("FrameModel")
class FrameModelTest {

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        void withFramesList_usesDefaultEquality() {
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            );

            var model = new FrameModel<>(frames);

            assertThat(model.title).isEmpty();
            assertThat(model.frames).isEqualTo(frames);
            assertThat(model.description).isNull();
        }

        @Test
        void withTitleAndEquality_setsAllFields() {
            var frames = List.of(new FrameBox<>("node", 0.0, 1.0, 0));
            FrameModel.FrameEquality<String> equality = (a, b) -> Objects.equals(a.actualNode, b.actualNode);

            var model = new FrameModel<>("My Title", equality, frames);

            assertThat(model.title).isEqualTo("My Title");
            assertThat(model.frames).isEqualTo(frames);
            assertThat(model.frameEquality).isEqualTo(equality);
        }

        @Test
        void nullTitle_throwsNullPointerException() {
            var frames = List.of(new FrameBox<>("node", 0.0, 1.0, 0));
            FrameModel.FrameEquality<String> equality = (a, b) -> true;

            assertThatThrownBy(() -> new FrameModel<>(null, equality, frames))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("title");
        }

        @Test
        void nullFrames_throwsNullPointerException() {
            FrameModel.FrameEquality<String> equality = (a, b) -> true;

            assertThatThrownBy(() -> new FrameModel<>("title", equality, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("frames");
        }

        @Test
        void nullEquality_throwsNullPointerException() {
            var frames = List.of(new FrameBox<>("node", 0.0, 1.0, 0));

            assertThatThrownBy(() -> new FrameModel<>("title", null, frames))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("frameEquality");
        }
    }

    @Nested
    @DisplayName("Empty Model")
    class EmptyModelTests {

        @Test
        void returnsSingletonInstance() {
            FrameModel<String> empty1 = FrameModel.empty();
            FrameModel<String> empty2 = FrameModel.empty();

            assertThat(empty1).isSameAs(empty2);
            assertThat(empty1.frames).isEmpty();
            assertThat(empty1.title).isEmpty();
        }

        @Test
        void worksWithDifferentTypes() {
            FrameModel<String> emptyString = FrameModel.empty();
            FrameModel<Integer> emptyInteger = FrameModel.empty();

            // Both reference the same singleton due to type erasure
            assertThat((Object) emptyString).isSameAs(emptyInteger);
        }

        @Test
        void framesIsEmptyList() {
            FrameModel<String> empty = FrameModel.empty();

            assertThat(empty.frames).isEqualTo(Collections.emptyList());
            assertThat(empty.frames).isEmpty();
        }
    }

    @Nested
    @DisplayName("WithDescription")
    class WithDescriptionTests {

        @Test
        void setsDescription() {
            var model = new FrameModel<>(List.of(new FrameBox<>("node", 0.0, 1.0, 0)));

            var result = model.withDescription("Test description");

            assertThat(result).isSameAs(model);
            assertThat(model.description).isEqualTo("Test description");
        }

        @Test
        void nullDescription_throwsNullPointerException() {
            var model = new FrameModel<>(List.of(new FrameBox<>("node", 0.0, 1.0, 0)));

            assertThatThrownBy(() -> model.withDescription(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("description");
        }
    }

    @Nested
    @DisplayName("Frame Equality")
    class FrameEqualityTests {

        @Test
        void defaultEquality_comparesActualNodes() {
            var frame1 = new FrameBox<>("same", 0.0, 0.5, 0);
            var frame2 = new FrameBox<>("same", 0.5, 1.0, 1);
            var frame3 = new FrameBox<>("different", 0.0, 0.5, 0);

            var model = new FrameModel<>(List.of(frame1, frame2, frame3));

            // Default equality compares actualNode using Objects.equals
            assertThat(model.frameEquality.equal(frame1, frame2)).isTrue();
            assertThat(model.frameEquality.equal(frame1, frame3)).isFalse();
        }

        @Test
        void customEquality_usesProvidedFunction() {
            var frame1 = new FrameBox<>("abc", 0.0, 0.5, 0);
            var frame2 = new FrameBox<>("def", 0.5, 1.0, 1);
            var frame3 = new FrameBox<>("ab", 0.0, 0.5, 0);

            // Custom equality: equal if same length
            FrameModel.FrameEquality<String> lengthEquality =
                    (a, b) -> a.actualNode.length() == b.actualNode.length();

            var model = new FrameModel<>("title", lengthEquality, List.of(frame1, frame2, frame3));

            assertThat(model.frameEquality.equal(frame1, frame2)).isTrue();  // both length 3
            assertThat(model.frameEquality.equal(frame1, frame3)).isFalse(); // 3 vs 2
        }
    }

    @Nested
    @DisplayName("Equals")
    class EqualsTests {

        @Test
        void sameModel_returnsTrue() {
            var frames = List.of(new FrameBox<>("node", 0.0, 1.0, 0));
            FrameModel.FrameEquality<String> equality = (a, b) -> Objects.equals(a.actualNode, b.actualNode);

            var model1 = new FrameModel<>("title", equality, frames);
            var model2 = new FrameModel<>("title", equality, frames);

            assertThat(model1).isEqualTo(model2);
        }

        @Test
        void differentTitle_returnsFalse() {
            var frames = List.of(new FrameBox<>("node", 0.0, 1.0, 0));
            FrameModel.FrameEquality<String> equality = (a, b) -> true;

            var model1 = new FrameModel<>("title1", equality, frames);
            var model2 = new FrameModel<>("title2", equality, frames);

            assertThat(model1).isNotEqualTo(model2);
        }

        @Test
        void differentFrames_returnsFalse() {
            FrameModel.FrameEquality<String> equality = (a, b) -> true;

            var model1 = new FrameModel<>("title", equality, List.of(new FrameBox<>("node1", 0.0, 1.0, 0)));
            var model2 = new FrameModel<>("title", equality, List.of(new FrameBox<>("node2", 0.0, 1.0, 0)));

            assertThat(model1).isNotEqualTo(model2);
        }

        @Test
        void differentDescription_returnsFalse() {
            var frames = List.of(new FrameBox<>("node", 0.0, 1.0, 0));
            FrameModel.FrameEquality<String> equality = (a, b) -> true;

            var model1 = new FrameModel<>("title", equality, frames).withDescription("desc1");
            var model2 = new FrameModel<>("title", equality, frames).withDescription("desc2");

            assertThat(model1).isNotEqualTo(model2);
        }

        @Test
        void withNull_returnsFalse() {
            var model = new FrameModel<>(List.of(new FrameBox<>("node", 0.0, 1.0, 0)));

            assertThat(model.equals(null)).isFalse();
        }

        @Test
        void withDifferentType_returnsFalse() {
            var model = new FrameModel<>(List.of(new FrameBox<>("node", 0.0, 1.0, 0)));

            assertThat(model.equals("not a FrameModel")).isFalse();
        }

        @Test
        void reflexive() {
            var model = new FrameModel<>(List.of(new FrameBox<>("node", 0.0, 1.0, 0)));

            assertThat(model).isEqualTo(model);
        }
    }

    @Nested
    @DisplayName("HashCode")
    class HashCodeTests {

        @Test
        void equalModels_sameHashCode() {
            var frames = List.of(new FrameBox<>("node", 0.0, 1.0, 0));
            FrameModel.FrameEquality<String> equality = (a, b) -> Objects.equals(a.actualNode, b.actualNode);

            var model1 = new FrameModel<>("title", equality, frames);
            var model2 = new FrameModel<>("title", equality, frames);

            assertThat(model1.hashCode()).isEqualTo(model2.hashCode());
        }

        @Test
        void consistent() {
            var model = new FrameModel<>(List.of(new FrameBox<>("node", 0.0, 1.0, 0)));

            int hash1 = model.hashCode();
            int hash2 = model.hashCode();

            assertThat(hash1).isEqualTo(hash2);
        }
    }

    @Nested
    @DisplayName("Frames List")
    class FramesListTests {

        @Test
        void isUnmodifiableWhenProvidedUnmodifiable() {
            var frames = List.of(new FrameBox<>("node", 0.0, 1.0, 0));
            var model = new FrameModel<>(frames);

            // List.of creates unmodifiable list
            assertThatThrownBy(() -> model.frames.add(new FrameBox<>("new", 0.0, 0.5, 0)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
