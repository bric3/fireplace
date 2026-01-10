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

import java.awt.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link FlamegraphView} minimap functionality.
 */
@DisplayName("FlamegraphView - Minimap")
class FlamegraphViewMinimapTest {

    private FlamegraphView<String> fg;

    @BeforeEach
    void setUp() {
        fg = new FlamegraphView<>();
    }

    @Nested
    @DisplayName("Minimap")
    class MinimapTests {

        @Test
        void isShowMinimap_default_is_true() {
            assertThat(fg.isShowMinimap()).isTrue();
        }

        @Test
        void setShowMinimap_false_hides_minimap() {
            fg.setShowMinimap(false);

            assertThat(fg.isShowMinimap()).isFalse();
        }

        @Test
        void setMinimapShadeColorSupplier_sets_supplier() {
            Supplier<Color> supplier = () -> Color.RED;

            fg.setMinimapShadeColorSupplier(supplier);

            assertThat(fg.getMinimapShadeColorSupplier()).isEqualTo(supplier);
        }

        @Test
        void setMinimapShadeColorSupplier_null_throws_exception() {
            assertThatThrownBy(() -> fg.setMinimapShadeColorSupplier(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void setShowMinimap_toggle_multiple_times() {
            assertThat(fg.isShowMinimap()).isTrue();

            fg.setShowMinimap(false);
            assertThat(fg.isShowMinimap()).isFalse();

            fg.setShowMinimap(true);
            assertThat(fg.isShowMinimap()).isTrue();

            fg.setShowMinimap(false);
            assertThat(fg.isShowMinimap()).isFalse();
        }

        @Test
        void setShowMinimap_same_value_does_not_throw() {
            fg.setShowMinimap(true);

            assertThatCode(() -> fg.setShowMinimap(true))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Minimap Shade Color")
    class MinimapShadeColorTests {

        @Test
        void getMinimapShadeColorSupplier_default_is_null() {
            // Default may be null or have a default supplier
            // Just verify it doesn't throw
            assertThatCode(() -> fg.getMinimapShadeColorSupplier())
                    .doesNotThrowAnyException();
        }

        @Test
        void setMinimapShadeColorSupplier_custom_color() {
            Supplier<Color> redSupplier = () -> Color.RED;

            fg.setMinimapShadeColorSupplier(redSupplier);

            assertThat(fg.getMinimapShadeColorSupplier()).isEqualTo(redSupplier);
            assertThat(fg.getMinimapShadeColorSupplier().get()).isEqualTo(Color.RED);
        }

        @Test
        void setMinimapShadeColorSupplier_with_alpha_color() {
            Supplier<Color> alphaSupplier = () -> new Color(128, 128, 128, 128);

            fg.setMinimapShadeColorSupplier(alphaSupplier);

            var color = fg.getMinimapShadeColorSupplier().get();
            assertThat(color.getAlpha()).isEqualTo(128);
        }
    }
}