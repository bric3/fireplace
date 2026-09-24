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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class FrameTextsProviderTest {
    @Test
    void list_factory_retains_candidates_in_order() {
        Function<FrameBox<String>, String> first = frame -> frame.actualNode;
        Function<FrameBox<String>, String> second = frame -> "short";

        var provider = FrameTextsProvider.of(List.of(first, second));

        assertThat(provider.frameToTextCandidates()).containsExactly(first, second);
    }

    @Test
    void varargs_factory_retains_candidates_in_order() {
        Function<FrameBox<String>, String> first = frame -> frame.actualNode;
        Function<FrameBox<String>, String> second = frame -> "short";

        var provider = FrameTextsProvider.of(first, second);

        assertThat(provider.frameToTextCandidates()).containsExactly(first, second);
    }

    @Test
    void factories_accept_no_candidates() {
        assertThat(FrameTextsProvider.of(List.of()).frameToTextCandidates()).isEmpty();
        assertThat(FrameTextsProvider.of().frameToTextCandidates()).isEmpty();
    }

    @Test
    void empty_provider_has_no_candidates() {
        assertThat(FrameTextsProvider.empty().frameToTextCandidates()).isEmpty();
    }

    @Test
    void default_clip_strategy_is_right() {
        var provider = FrameTextsProvider.of(frame -> "label");

        assertThat(provider.clipStrategy()).isSameAs(StringClipper.RIGHT);
    }
}
