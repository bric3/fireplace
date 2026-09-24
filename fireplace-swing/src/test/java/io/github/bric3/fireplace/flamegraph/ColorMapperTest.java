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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

class ColorMapperTest {
    @Test
    void null_maps_to_first_palette_color() {
        var mapper = ColorMapper.ofObjectHashUsing(Color.PINK, Color.CYAN);

        assertThat(mapper.mapToColor(null)).isEqualTo(Color.PINK);
    }

    @ParameterizedTest(name = "hash {0} selects palette entry {1}")
    @CsvSource({"0, 0", "1, 1", "2, 2", "3, 0", "4, 1", "-1, 1", "-2, 2", "-3, 0", "2147483647, 1"})
    void hash_selects_expected_palette_entry(int hash, int paletteIndex) {
        Color[] palette = {Color.RED, Color.GREEN, Color.BLUE};
        ColorMapper<HashedValue> mapper = ColorMapper.ofObjectHashUsing(palette);

        assertThat(mapper.mapToColor(new HashedValue(hash))).isEqualTo(palette[paletteIndex]);
    }

    @Test
    void same_hash_keeps_its_color_across_calls_and_mapper_instances() {
        ColorMapper<HashedValue> first = ColorMapper.ofObjectHashUsing(Color.RED, Color.GREEN, Color.BLUE);
        ColorMapper<HashedValue> second = ColorMapper.ofObjectHashUsing(Color.RED, Color.GREEN, Color.BLUE);
        var value = new HashedValue(4);

        assertThat(first.mapToColor(value)).isEqualTo(Color.GREEN);
        assertThat(first.mapToColor(new HashedValue(2))).isEqualTo(Color.BLUE);
        assertThat(first.mapToColor(value)).isEqualTo(Color.GREEN);
        assertThat(second.mapToColor(new HashedValue(4))).isEqualTo(Color.GREEN);
    }

    @Test
    void single_color_palette_maps_every_hash_and_null_to_that_color() {
        ColorMapper<Integer> mapper = ColorMapper.ofObjectHashUsing(Color.ORANGE);

        assertThat(mapper.mapToColor(null)).isEqualTo(Color.ORANGE);
        assertThat(mapper.mapToColor(1)).isEqualTo(Color.ORANGE);
        assertThat(mapper.mapToColor(-1)).isEqualTo(Color.ORANGE);
    }

    @Test
    void apply_delegates_to_mapToColor() {
        ColorMapper<Integer> mapper = i -> i != null && i > 0 ? Color.GREEN : Color.RED;

        assertThat(mapper.apply(5)).isEqualTo(Color.GREEN);
        assertThat(mapper.apply(-5)).isEqualTo(Color.RED);
        assertThat(mapper.apply(null)).isEqualTo(Color.RED);
    }

    private static final class HashedValue {
        private final int hash;

        private HashedValue(int hash) {
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
