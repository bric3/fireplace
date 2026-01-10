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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ColorMapper}.
 * These tests run in headless mode without requiring a display.
 */
@DisplayName("ColorMapper")
class ColorMapperTest {

    @Nested
    @DisplayName("ofObjectHashUsing factory method")
    class OfObjectHashUsing {

        @Test
        void nullObject_returnsFirstColor() {
            Color first = Color.RED;
            Color second = Color.BLUE;
            ColorMapper<String> mapper = ColorMapper.ofObjectHashUsing(first, second);

            Color result = mapper.mapToColor(null);

            assertThat(result).isEqualTo(first);
        }

        @Test
        void sameObject_returnsSameColor() {
            ColorMapper<String> mapper = ColorMapper.ofObjectHashUsing(Color.RED, Color.GREEN, Color.BLUE);

            String obj = "test";
            Color color1 = mapper.mapToColor(obj);
            Color color2 = mapper.mapToColor(obj);

            assertThat(color1).isEqualTo(color2);
        }

        @Test
        void equalObjects_returnsSameColor() {
            ColorMapper<String> mapper = ColorMapper.ofObjectHashUsing(Color.RED, Color.GREEN, Color.BLUE);

            Color color1 = mapper.mapToColor("test");
            Color color2 = mapper.mapToColor(new String("test")); // new instance, but equal

            assertThat(color1).isEqualTo(color2);
        }

        @Test
        void differentObjects_mayReturnDifferentColors() {
            ColorMapper<Integer> mapper = ColorMapper.ofObjectHashUsing(Color.RED, Color.GREEN, Color.BLUE);

            // With different integers, we should eventually get different colors
            var colors = new HashSet<Color>();
            for (int i = 0; i < 100; i++) {
                colors.add(mapper.mapToColor(i));
            }

            // Should have at least 2 different colors (statistically almost certain with 100 items and 3 colors)
            assertThat(colors.size()).isGreaterThan(1);
        }

        @Test
        void singleColorPalette_alwaysReturnsSameColor() {
            ColorMapper<String> mapper = ColorMapper.ofObjectHashUsing(Color.ORANGE);

            assertThat(mapper.mapToColor("a")).isEqualTo(Color.ORANGE);
            assertThat(mapper.mapToColor("b")).isEqualTo(Color.ORANGE);
            assertThat(mapper.mapToColor("anything")).isEqualTo(Color.ORANGE);
        }

        @Test
        void largePalette_usesAllColors() {
            Color[] palette = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA};
            ColorMapper<Integer> mapper = ColorMapper.ofObjectHashUsing(palette);

            var usedColors = new HashSet<Color>();
            // Generate enough objects to likely hit all colors
            for (int i = 0; i < 1000; i++) {
                usedColors.add(mapper.mapToColor(i));
            }

            // Should use all colors from the palette
            assertThat(usedColors).containsExactlyInAnyOrder(palette);
        }

        @Test
        void consistentAcrossMultipleCalls() {
            Color[] palette = {Color.RED, Color.GREEN, Color.BLUE};

            // Create two separate mappers with same palette
            ColorMapper<String> mapper1 = ColorMapper.ofObjectHashUsing(palette);
            ColorMapper<String> mapper2 = ColorMapper.ofObjectHashUsing(palette);

            String obj = "consistent-test";

            assertThat(mapper1.mapToColor(obj)).isEqualTo(mapper2.mapToColor(obj));
        }

        @Test
        void handlesNegativeHashCodes() {
            ColorMapper<Integer> mapper = ColorMapper.ofObjectHashUsing(Color.RED, Color.GREEN, Color.BLUE);

            // Negative numbers will have negative hash codes
            Color result = mapper.mapToColor(-123);

            // Should not throw and should return a valid color from the palette
            assertThat(result).isIn(Color.RED, Color.GREEN, Color.BLUE);
        }

        @Test
        void distributesEvenly() {
            Color[] palette = {Color.RED, Color.GREEN, Color.BLUE};
            ColorMapper<Integer> mapper = ColorMapper.ofObjectHashUsing(palette);

            int[] counts = new int[palette.length];
            int sampleSize = 3000;

            for (int i = 0; i < sampleSize; i++) {
                Color color = mapper.mapToColor(i);
                for (int j = 0; j < palette.length; j++) {
                    if (palette[j].equals(color)) {
                        counts[j]++;
                        break;
                    }
                }
            }

            // Each color should be used roughly 1/3 of the time
            int expected = sampleSize / palette.length;
            int tolerance = expected / 2; // Allow 50% deviation

            for (int count : counts) {
                assertThat(count)
                        .as("Color distribution should be roughly even")
                        .isBetween(expected - tolerance, expected + tolerance);
            }
        }
    }

    @Nested
    @DisplayName("mapToColor method")
    class MapToColor {

        @Test
        void customObject_usesHashCode() {
            ColorMapper<CustomObject> mapper = ColorMapper.ofObjectHashUsing(Color.RED, Color.GREEN, Color.BLUE);

            // Objects with same hash code should map to same color
            CustomObject obj1 = new CustomObject(42);
            CustomObject obj2 = new CustomObject(42);

            assertThat(obj1.hashCode()).isEqualTo(obj2.hashCode());
            assertThat(mapper.mapToColor(obj1)).isEqualTo(mapper.mapToColor(obj2));
        }
    }

    @Nested
    @DisplayName("apply method (Function interface)")
    class ApplyMethod {

        @Test
        void delegatesToMapToColor() {
            ColorMapper<String> mapper = ColorMapper.ofObjectHashUsing(Color.RED, Color.BLUE);

            String obj = "test";
            Color viaApply = mapper.apply(obj);
            Color viaMapToColor = mapper.mapToColor(obj);

            assertThat(viaApply).isEqualTo(viaMapToColor);
        }

        @Test
        void nullObject_returnsFirstColor() {
            ColorMapper<String> mapper = ColorMapper.ofObjectHashUsing(Color.PINK, Color.CYAN);

            Color result = mapper.apply(null);

            assertThat(result).isEqualTo(Color.PINK);
        }
    }

    @Nested
    @DisplayName("functional interface implementation")
    class FunctionalInterface {

        @Test
        void canBeImplementedAsLambda() {
            // ColorMapper extends Function, so can be implemented as lambda
            ColorMapper<String> customMapper = s -> s == null ? Color.BLACK : Color.WHITE;

            assertThat(customMapper.mapToColor(null)).isEqualTo(Color.BLACK);
            assertThat(customMapper.mapToColor("anything")).isEqualTo(Color.WHITE);
        }

        @Test
        void applyMethodWorks() {
            ColorMapper<Integer> customMapper = i -> i != null && i > 0 ? Color.GREEN : Color.RED;

            assertThat(customMapper.apply(5)).isEqualTo(Color.GREEN);
            assertThat(customMapper.apply(-5)).isEqualTo(Color.RED);
            assertThat(customMapper.apply(null)).isEqualTo(Color.RED);
        }
    }

    // Helper class for testing custom objects
    private static class CustomObject {
        private final int id;

        CustomObject(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof CustomObject && ((CustomObject) obj).id == this.id;
        }
    }
}
