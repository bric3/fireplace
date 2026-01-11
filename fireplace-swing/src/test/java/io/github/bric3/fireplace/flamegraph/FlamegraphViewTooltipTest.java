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

import javax.swing.*;
import java.awt.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link FlamegraphView} tooltip configuration.
 */
@DisplayName("FlamegraphView - Tooltip")
class FlamegraphViewTooltipTest {

    private FlamegraphView<String> fg;

    @BeforeEach
    void setUp() {
        fg = new FlamegraphView<>();
    }

    @Nested
    @DisplayName("Tooltip")
    class TooltipTests {

        @Test
        void setTooltipTextFunction_sets_function() {
            BiFunction<FrameModel<String>, FrameBox<String>, String> func =
                    (model, frame) -> frame.actualNode;

            fg.setTooltipTextFunction(func);

            assertThat(fg.getTooltipTextFunction()).isEqualTo(func);
        }

        @Test
        void setTooltipTextFunction_null_throws_exception() {
            assertThatThrownBy(() -> fg.setTooltipTextFunction(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void getTooltipTextFunction_default_is_null() {
            assertThat(fg.getTooltipTextFunction()).isNull();
        }

        @Test
        void setTooltipComponentSupplier_sets_supplier() {
            Supplier<JToolTip> supplier = JToolTip::new;

            fg.setTooltipComponentSupplier(supplier);

            assertThat(fg.getTooltipComponentSupplier()).isEqualTo(supplier);
        }

        @Test
        void setTooltipComponentSupplier_null_throws_exception() {
            assertThatThrownBy(() -> fg.setTooltipComponentSupplier(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void setTooltipTextFunction_custom_function_is_set() {
            BiFunction<FrameModel<String>, FrameBox<String>, String> tooltipFunc =
                    (model, frame) -> "Tooltip: " + frame.actualNode;

            fg.setTooltipTextFunction(tooltipFunc);

            assertThat(fg.getTooltipTextFunction()).isEqualTo(tooltipFunc);
        }

        @Test
        void setTooltipComponentSupplier_custom_supplier_is_set() {
            Supplier<JToolTip> tooltipSupplier = () -> {
                var tip = new JToolTip();
                tip.setBackground(Color.YELLOW);
                return tip;
            };

            fg.setTooltipComponentSupplier(tooltipSupplier);

            assertThat(fg.getTooltipComponentSupplier()).isEqualTo(tooltipSupplier);
        }
    }
}