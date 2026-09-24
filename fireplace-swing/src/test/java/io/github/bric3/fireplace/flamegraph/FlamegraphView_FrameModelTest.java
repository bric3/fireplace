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

import io.github.bric3.fireplace.core.ui.fixtures.SwingEdtExtension;
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
@org.junit.jupiter.api.extension.ExtendWith(SwingEdtExtension.class)
class FlamegraphView_FrameModelTest {

    private FlamegraphView<String> fg;

    @BeforeEach
    void setUp() {
        fg = new FlamegraphView<>();
    }

    @Test
    void replacing_a_model_preserves_mode_and_minimap_configuration() {
        fg.setMode(FlamegraphView.Mode.FLAMEGRAPH);
        fg.setShowMinimap(false);
        fg.setModel(new FrameModel<>(List.of(new FrameBox<>("first", 0, 1, 0))));
        var replacement = new FrameModel<>(List.of(new FrameBox<>("second", 0, 1, 0)));
        fg.setModel(replacement);
        assertThat(fg.getFrameModel()).isSameAs(replacement);
        assertThat(fg.getMode()).isEqualTo(FlamegraphView.Mode.FLAMEGRAPH);
        assertThat(fg.isShowMinimap()).isFalse();
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
        void setModel_retains_metadata_and_custom_equality() {
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            );
            FrameModel.FrameEquality<String> equality = (a, b) -> Objects.equals(a.actualNode, b.actualNode);
            var model = new FrameModel<>("Test Flamegraph", equality, frames).withDescription("Test description");

            fg.setModel(model);

            assertThat(fg.getFrameModel().title).isEqualTo("Test Flamegraph");
            assertThat(fg.getFrameModel().frameEquality).isSameAs(equality);
            assertThat(fg.getFrames()).containsExactlyElementsOf(frames);
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
        void clear_resets_to_empty_and_accepts_a_replacement_model() {
            fg.setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            assertThat(fg.getFrames()).hasSize(2);

            fg.clear();

            assertThat(fg.getFrameModel()).isEqualTo(FrameModel.empty());
            assertThat(fg.getFrames()).isEmpty();

            var newModel = new FrameModel<>(List.of(new FrameBox<>("second", 0.0, 1.0, 0)));
            fg.setModel(newModel);

            assertThat(fg.getFrameModel()).isSameAs(newModel);
            assertThat(fg.getFrames()).containsExactlyElementsOf(newModel.frames);
        }

        @Test
        void clear_multiple_times_leaves_an_empty_model() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0, 1, 0))));
            fg.clear();
            fg.clear();
            assertThat(fg.getFrameModel()).isEqualTo(FrameModel.empty());
            assertThat(fg.getFrames()).isEmpty();
        }
    }

}
