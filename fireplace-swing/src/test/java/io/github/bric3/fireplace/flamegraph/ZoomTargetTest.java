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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.awt.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ZoomTarget}.
 * These tests run in headless mode without requiring a display.
 */
@DisplayName("ZoomTarget")
class ZoomTargetTest {

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        void constructor_withCoordinates_setsBounds() {
            var frame = new FrameBox<>("node", 0.0, 1.0, 0);
            var zoomTarget = new ZoomTarget<>(10, 20, 100, 50, frame);

            assertThat(zoomTarget.getX()).isEqualTo(10.0);
            assertThat(zoomTarget.getY()).isEqualTo(20.0);
            assertThat(zoomTarget.getWidth()).isEqualTo(100.0);
            assertThat(zoomTarget.getHeight()).isEqualTo(50.0);
            assertThat(zoomTarget.targetFrame).isEqualTo(frame);
        }

        @Test
        void constructor_withRectangle_copiesBounds() {
            var frame = new FrameBox<>("node", 0.0, 1.0, 0);
            var bounds = new Rectangle(15, 25, 200, 75);
            var zoomTarget = new ZoomTarget<>(bounds, frame);

            bounds.setBounds(999, 888, 777, 666);

            assertThat(zoomTarget.targetFrame).isSameAs(frame);
            assertThat(zoomTarget.getX()).isEqualTo(15.0);
            assertThat(zoomTarget.getY()).isEqualTo(25.0);
            assertThat(zoomTarget.getWidth()).isEqualTo(200.0);
            assertThat(zoomTarget.getHeight()).isEqualTo(75.0);
        }

        @Test
        void constructor_withNullFrame_isAllowed() {
            var zoomTarget = new ZoomTarget<String>(10, 20, 100, 50, null);

            assertThat(zoomTarget.targetFrame).isNull();
        }

        @Test
        void constructor_rectangleWithNullFrame_isAllowed() {
            var zoomTarget = new ZoomTarget<String>(new Rectangle(0, 0, 100, 100), null);

            assertThat(zoomTarget.targetFrame).isNull();
        }

        @Test
        void constructor_negativeValues_areAllowed() {
            var zoomTarget = new ZoomTarget<String>(-10, -20, 100, 50, null);

            assertThat(zoomTarget.getX()).isEqualTo(-10.0);
            assertThat(zoomTarget.getY()).isEqualTo(-20.0);
        }

        @Test
        void constructor_zeroWidthAndHeight_areAllowed() {
            var zoomTarget = new ZoomTarget<String>(10, 20, 0, 0, null);

            assertThat(zoomTarget.getWidth()).isZero();
            assertThat(zoomTarget.getHeight()).isZero();
        }
    }

    @Nested
    @DisplayName("getTargetBounds")
    class GetTargetBoundsTests {

        @Test
        void getTargetBounds_returnsDefensiveCopy() {
            var zoomTarget = new ZoomTarget<String>(10, 20, 100, 50, null);

            Rectangle bounds1 = zoomTarget.getTargetBounds();
            bounds1.setBounds(999, 888, 777, 666);

            Rectangle bounds2 = zoomTarget.getTargetBounds();
            assertThat(bounds2).isNotSameAs(bounds1).isEqualTo(new Rectangle(10, 20, 100, 50));
        }

        @Test
        void getTargetBounds_withRect_populatesAndReturnsProvided() {
            var zoomTarget = new ZoomTarget<String>(10, 20, 100, 50, null);
            var rect = new Rectangle();

            Rectangle result = zoomTarget.getTargetBounds(rect);

            assertThat(result).isSameAs(rect);
            assertThat(rect.x).isEqualTo(10);
            assertThat(rect.y).isEqualTo(20);
            assertThat(rect.width).isEqualTo(100);
            assertThat(rect.height).isEqualTo(50);
        }

        @Test
        void getTargetBounds_withRect_canBeReused() {
            var zoomTarget1 = new ZoomTarget<String>(10, 20, 100, 50, null);
            var zoomTarget2 = new ZoomTarget<String>(30, 40, 200, 100, null);
            var rect = new Rectangle();

            zoomTarget1.getTargetBounds(rect);
            assertThat(rect.x).isEqualTo(10);

            zoomTarget2.getTargetBounds(rect);
            assertThat(rect.x).isEqualTo(30);
            assertThat(rect.y).isEqualTo(40);
            assertThat(rect.width).isEqualTo(200);
            assertThat(rect.height).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCodeTests {

        @ParameterizedTest
        @CsvSource({"11, 20, 100, 50", "10, 21, 100, 50", "10, 20, 101, 50", "10, 20, 100, 51"})
        void equals_differentBounds_returnsFalse(int x, int y, int width, int height) {
            var frame = new FrameBox<>("node", 0.0, 1.0, 0);
            var zoomTarget1 = new ZoomTarget<>(10, 20, 100, 50, frame);
            var zoomTarget2 = new ZoomTarget<>(x, y, width, height, frame);

            assertThat(zoomTarget1).isNotEqualTo(zoomTarget2);
        }

        @Test
        void equals_differentFrame_returnsFalse() {
            var frame1 = new FrameBox<>("node1", 0.0, 1.0, 0);
            var frame2 = new FrameBox<>("node2", 0.0, 1.0, 0);
            var zoomTarget1 = new ZoomTarget<>(10, 20, 100, 50, frame1);
            var zoomTarget2 = new ZoomTarget<>(10, 20, 100, 50, frame2);

            assertThat(zoomTarget1).isNotEqualTo(zoomTarget2);
        }

        @Test
        void equals_bothNullFrames_returnsTrue() {
            var zoomTarget1 = new ZoomTarget<String>(10, 20, 100, 50, null);
            var zoomTarget2 = new ZoomTarget<String>(10, 20, 100, 50, null);

            assertThat(zoomTarget1).isEqualTo(zoomTarget2);
        }

        @Test
        void equals_oneNullFrame_returnsFalse() {
            var frame = new FrameBox<>("node", 0.0, 1.0, 0);
            var zoomTarget1 = new ZoomTarget<>(10, 20, 100, 50, frame);
            var zoomTarget2 = new ZoomTarget<String>(10, 20, 100, 50, null);

            assertThat(zoomTarget1).isNotEqualTo(zoomTarget2);
        }

        @Test
        void equals_withNull_returnsFalse() {
            var zoomTarget = new ZoomTarget<String>(10, 20, 100, 50, null);

            assertThat(zoomTarget.equals(null)).isFalse();
        }

        @Test
        void equals_withDifferentType_returnsFalse() {
            var zoomTarget = new ZoomTarget<String>(10, 20, 100, 50, null);

            assertThat(zoomTarget.equals("not a ZoomTarget")).isFalse();
        }

        @Test
        void equals_reflexive() {
            var zoomTarget = new ZoomTarget<String>(10, 20, 100, 50, null);

            assertThat(zoomTarget.equals(zoomTarget)).isTrue();
        }

        @Test
        void equals_symmetric() {
            var frame = new FrameBox<>("node", 0.0, 1.0, 0);
            var zoomTarget1 = new ZoomTarget<>(10, 20, 100, 50, frame);
            var zoomTarget2 = new ZoomTarget<>(10, 20, 100, 50, frame);

            assertThat(zoomTarget1.equals(zoomTarget2)).isTrue();
            assertThat(zoomTarget2.equals(zoomTarget1)).isTrue();
        }

        @Test
        void hashCode_equalObjects_sameHashCode() {
            var frame = new FrameBox<>("node", 0.0, 1.0, 0);
            var zoomTarget1 = new ZoomTarget<>(10, 20, 100, 50, frame);
            var zoomTarget2 = new ZoomTarget<>(10, 20, 100, 50, frame);

            assertThat(zoomTarget1.hashCode()).isEqualTo(zoomTarget2.hashCode());
        }

        @Test
        void hashCode_consistent() {
            var zoomTarget = new ZoomTarget<String>(10, 20, 100, 50, null);

            int hash1 = zoomTarget.hashCode();
            int hash2 = zoomTarget.hashCode();

            assertThat(hash1).isEqualTo(hash2);
        }
    }
}
