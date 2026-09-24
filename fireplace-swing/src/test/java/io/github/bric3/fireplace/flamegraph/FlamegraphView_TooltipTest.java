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
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link FlamegraphView} tooltip configuration.
 */
@DisplayName("FlamegraphView - Tooltip")
@org.junit.jupiter.api.extension.ExtendWith(SwingEdtExtension.class)
class FlamegraphView_TooltipTest {

    private FlamegraphView<String> fg;

    @BeforeEach
    void setUp() {
        fg = new FlamegraphView<>();
    }

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

}
