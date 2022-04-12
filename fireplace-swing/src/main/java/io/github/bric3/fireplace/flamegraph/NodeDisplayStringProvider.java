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

import java.util.List;
import java.util.function.Function;

@FunctionalInterface
public interface NodeDisplayStringProvider<T> {

    List<Function<FrameBox<T>, String>> frameToTextCandidates();

    static <T> NodeDisplayStringProvider<T> of(List<Function<FrameBox<T>, String>> frameToTextCandidates) {
        return () -> frameToTextCandidates;
    }

    @SafeVarargs
    static <T> NodeDisplayStringProvider<T> of(Function<FrameBox<T>, String>... frameToTextCandidates) {
        return of(List.of(frameToTextCandidates));
    }

    static <T> NodeDisplayStringProvider<T> empty() {
        return of(List.of());
    }

    default StringClipper clipStrategy() {
        return StringClipper.RIGHT;
    }
}
