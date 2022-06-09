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

import java.util.List;
import java.util.function.Function;

/**
 * Backward compatibility class, use {@link FrameTextsProvider} instead.
 * @param <T> The type of frame node
 */
@Deprecated(forRemoval = true)
public interface NodeDisplayStringProvider<T> extends FrameTextsProvider<T> {
    @Deprecated(forRemoval = true)
    static <T> NodeDisplayStringProvider<T> of(List<Function<FrameBox<T>, String>> frameToTextCandidates) {
        return () -> frameToTextCandidates;
    }

    @Deprecated(forRemoval = true)
    @SafeVarargs
    static <T> NodeDisplayStringProvider<T> of(Function<FrameBox<T>, String>... frameToTextCandidates) {
        return of(List.of(frameToTextCandidates));
    }
}
