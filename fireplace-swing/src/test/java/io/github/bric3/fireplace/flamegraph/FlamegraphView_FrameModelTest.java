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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link FlamegraphView} model operations and clear functionality.
 */
@SuppressWarnings("NewClassNamingConvention")
@DisplayName("FlamegraphView - Model")
class FlamegraphView_FrameModelTest {

    private FlamegraphView<String> fg;

    @BeforeEach
    void setUp() {
        fg = new FlamegraphView<>();
    }

    @Nested
    @DisplayName("Model")
    class ModelTests {

        @Test
        void getFrameModel_default_is_empty() {
            assertThat(fg.getFrameModel()).isEqualTo(FrameModel.empty());
        }

        @Test
        void setModel_updates_frame_model() {
            var model = new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0)));

            fg.setModel(model);

            assertThat(fg.getFrameModel()).isEqualTo(model);
        }

        @Test
        void setModel_null_throws_exception() {
            assertThatThrownBy(() -> fg.setModel(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void getFrames_returns_frame_list() {
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            );
            var model = new FrameModel<>(frames);

            fg.setModel(model);

            assertThat(fg.getFrames()).isEqualTo(frames);
        }

        @Test
        void setModel_with_title_and_equality() {
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            );
            var model = new FrameModel<>(
                    "Test Flamegraph",
                    (a, b) -> Objects.equals(a.actualNode, b.actualNode),
                    frames
            );

            fg.setModel(model);

            assertThat(fg.getFrameModel()).isEqualTo(model);
            assertThat(fg.getFrameModel().title).isEqualTo("Test Flamegraph");
        }

        @Test
        void setModel_with_description() {
            var model = new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0)))
                    .withDescription("Test description");

            fg.setModel(model);

            assertThat(fg.getFrameModel().description).isEqualTo("Test description");
        }

        @Test
        void setModel_same_model_reference_twice() {
            var model = new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0)));

            fg.setModel(model);
            fg.setModel(model);

            assertThat(fg.getFrameModel()).isSameAs(model);
        }

        @Test
        void setModel_different_models_updates() {
            var model1 = new FrameModel<>(List.of(new FrameBox<>("first", 0.0, 1.0, 0)));
            var model2 = new FrameModel<>(List.of(new FrameBox<>("second", 0.0, 1.0, 0)));

            fg.setModel(model1);
            assertThat(fg.getFrames().get(0).actualNode).isEqualTo("first");

            fg.setModel(model2);
            assertThat(fg.getFrames().get(0).actualNode).isEqualTo("second");
        }
    }

    @Nested
    @DisplayName("Clear")
    class ClearTests {

        @Test
        void clear_resets_model() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            fg.clear();

            assertThat(fg.getFrameModel()).isEqualTo(FrameModel.empty());
            assertThat(fg.getFrames()).isEmpty();
        }

        @Test
        void clear_after_model_set_resets_to_empty() {
            fg.setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            assertThat(fg.getFrames()).hasSize(2);

            fg.clear();

            assertThat(fg.getFrameModel()).isEqualTo(FrameModel.empty());
            assertThat(fg.getFrames()).isEmpty();
        }

        @Test
        void clear_then_set_model_works() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("first", 0.0, 1.0, 0))));
            fg.clear();

            var newModel = new FrameModel<>(List.of(new FrameBox<>("second", 0.0, 1.0, 0)));
            fg.setModel(newModel);

            assertThat(fg.getFrames()).hasSize(1);
            assertThat(fg.getFrames().get(0).actualNode).isEqualTo("second");
        }

        @Test
        void clear_multiple_times_does_not_throw() {
            assertThatCode(() -> {
                fg.clear();
                fg.clear();
                fg.clear();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Request Repaint")
    class RequestRepaintTests {

        @Test
        void requestRepaint_with_model_does_not_throw() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            assertThatCode(() -> fg.requestRepaint())
                    .doesNotThrowAnyException();
        }

        @Test
        void requestRepaint_multiple_times_does_not_throw() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            assertThatCode(() -> {
                fg.requestRepaint();
                fg.requestRepaint();
                fg.requestRepaint();
            }).doesNotThrowAnyException();
        }
    }
}