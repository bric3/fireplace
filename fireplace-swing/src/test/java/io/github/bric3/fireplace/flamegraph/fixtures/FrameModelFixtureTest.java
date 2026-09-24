/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.flamegraph.fixtures;

import io.github.bric3.fireplace.flamegraph.FrameModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class FrameModelFixtureTest {
    @Test
    void model_descriptions_do_not_leak_between_scenarios() {
        List<Supplier<FrameModel<String>>> factories = List.of(
                FrameModelFixture::rootWithSiblings, () -> FrameModelFixture.deepChain(5));
        for (var factory : factories) {
            var first = factory.get().withDescription("changed by an earlier scenario");
            var next = factory.get();

            assertThat(next).isNotSameAs(first);
            assertThat(next.description).isNull();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 40})
    void chain_depth_counts_descendants_and_keeps_them_inside_their_parent(int depth) {
        var frames = FrameModelFixture.deepChain(depth).frames;
        assertThat(frames).hasSize(depth + 1);
        assertThat(frames.get(0).isRoot()).isTrue();
        for (int level = 1; level < frames.size(); level++) {
            var parent = frames.get(level - 1);
            var child = frames.get(level);
            assertThat(child.stackDepth).isEqualTo(parent.stackDepth + 1);
            assertThat(child.startX).isGreaterThanOrEqualTo(parent.startX);
            assertThat(child.endX).isLessThanOrEqualTo(parent.endX).isGreaterThan(child.startX);
        }
    }
}
