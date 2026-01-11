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
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FlamegraphView.ZoomModel}.
 */
@DisplayName("ZoomModel")
class FlamegraphViewZoomModelTest {

    private FlamegraphView.ZoomModel<String> zoomModel;

    @BeforeEach
    void setUp() {
        zoomModel = new FlamegraphView.ZoomModel<>();
    }

    @Test
    void initial_state_has_default_values() {
        assertSoftly(softly -> {
            softly.assertThat(zoomModel.getCurrentZoomTarget()).isNull();
            softly.assertThat(zoomModel.getLastScaleFactor()).isEqualTo(1.0);
            softly.assertThat(zoomModel.getLastUserInteractionStartX()).isEqualTo(0.0);
            softly.assertThat(zoomModel.getLastUserInteractionEndX()).isEqualTo(0.0);
            softly.assertThat(zoomModel.getLastUserInteractionWidthX()).isEqualTo(0.0);
        });
    }

    @Test
    void recordLastPositionFromZoomTarget_with_null_zoom_target_resets_to_defaults() {
        // Arrange: set some non-default values first
        var canvas = mock(JPanel.class);
        var frameBox = new FrameBox<>("test", 0.2, 0.8, 0);
        var zoomTarget = new ZoomTarget<>(0, 0, 400, 100, frameBox);
        doAnswer(invocation -> {
            Rectangle rect = invocation.getArgument(0);
            rect.setBounds(0, 0, 400, 600);
            return null;
        }).when(canvas).computeVisibleRect(any(Rectangle.class));

        zoomModel.recordLastPositionFromZoomTarget(canvas, zoomTarget);

        // Act: call with null
        zoomModel.recordLastPositionFromZoomTarget(canvas, null);

        // Assert
        assertSoftly(softly -> {
            softly.assertThat(zoomModel.getCurrentZoomTarget()).isNull();
            softly.assertThat(zoomModel.getLastScaleFactor()).isEqualTo(1.0);
            softly.assertThat(zoomModel.getLastUserInteractionStartX()).isEqualTo(0.0);
            softly.assertThat(zoomModel.getLastUserInteractionEndX()).isEqualTo(1.0);
        });
    }

    @Test
    void recordLastPositionFromZoomTarget_with_null_target_frame_resets_to_defaults() {
        // Arrange
        var canvas = mock(JPanel.class);
        var zoomTargetWithNullFrame = new ZoomTarget<String>(0, 0, 400, 100, null);

        // Act
        zoomModel.recordLastPositionFromZoomTarget(canvas, zoomTargetWithNullFrame);

        // Assert
        assertSoftly(softly -> {
            softly.assertThat(zoomModel.getCurrentZoomTarget()).isSameAs(zoomTargetWithNullFrame);
            softly.assertThat(zoomModel.getLastScaleFactor()).isEqualTo(1.0);
            softly.assertThat(zoomModel.getLastUserInteractionStartX()).isEqualTo(0.0);
            softly.assertThat(zoomModel.getLastUserInteractionEndX()).isEqualTo(1.0);
        });
    }

    @Test
    void recordLastPositionFromZoomTarget_with_valid_zoom_target_extracts_frame_coordinates() {
        // Arrange
        var canvas = mock(JPanel.class);
        var frameBox = new FrameBox<>("test", 0.25, 0.75, 1);
        var zoomTarget = new ZoomTarget<>(100, 50, 400, 200, frameBox);

        doAnswer(invocation -> {
            Rectangle rect = invocation.getArgument(0);
            rect.setBounds(0, 0, 800, 600);
            return null;
        }).when(canvas).computeVisibleRect(any(Rectangle.class));

        // Act
        zoomModel.recordLastPositionFromZoomTarget(canvas, zoomTarget);

        // Assert
        // getScaleFactor = visibleWidth / (canvasWidth * frameWidthX) = 800 / (400 * 1.0) = 2.0
        assertSoftly(softly -> {
            softly.assertThat(zoomModel.getCurrentZoomTarget()).isSameAs(zoomTarget);
            softly.assertThat(zoomModel.getLastScaleFactor()).isEqualTo(2.0);
            softly.assertThat(zoomModel.getLastUserInteractionStartX()).isEqualTo(0.25);
            softly.assertThat(zoomModel.getLastUserInteractionEndX()).isEqualTo(0.75);
            softly.assertThat(zoomModel.getLastUserInteractionWidthX()).isEqualTo(0.5);
        });
    }

    @Test
    void recordLastPositionFromUserInteraction_calculates_position_from_canvas() {
        // Arrange
        var canvas = mock(JPanel.class);
        when(canvas.getWidth()).thenReturn(1000);
        doAnswer(invocation -> {
            Rectangle rect = invocation.getArgument(0);
            rect.setBounds(200, 0, 400, 600); // x=200, visible width=400
            return null;
        }).when(canvas).computeVisibleRect(any(Rectangle.class));

        // Act
        zoomModel.recordLastPositionFromUserInteraction(canvas);

        // Assert
        // startX = 200 / 1000 = 0.2
        // endX = (200 + 400) / 1000 = 0.6
        // scaleFactor = visibleWidth / (canvasWidth * frameWidthX) = 400 / (1000 * 1.0) = 0.4
        assertSoftly(softly -> {
            softly.assertThat(zoomModel.getLastScaleFactor()).isCloseTo(0.4, within(1e-10));
            softly.assertThat(zoomModel.getLastUserInteractionStartX()).isCloseTo(0.2, within(1e-10));
            softly.assertThat(zoomModel.getLastUserInteractionEndX()).isCloseTo(0.6, within(1e-10));
            softly.assertThat(zoomModel.getLastUserInteractionWidthX()).isCloseTo(0.4, within(1e-10));
        });
    }

    @Test
    void recordLastPositionFromUserInteraction_with_no_scroll_offset() {
        // Arrange
        var canvas = mock(JPanel.class);
        when(canvas.getWidth()).thenReturn(800);
        doAnswer(invocation -> {
            Rectangle rect = invocation.getArgument(0);
            rect.setBounds(0, 0, 800, 600); // x=0, visible width matches canvas width
            return null;
        }).when(canvas).computeVisibleRect(any(Rectangle.class));

        // Act
        zoomModel.recordLastPositionFromUserInteraction(canvas);

        // Assert
        // startX = 0 / 800 = 0.0
        // endX = (0 + 800) / 800 = 1.0
        // scaleFactor = 800 / (800 * 1.0) = 1.0
        assertSoftly(softly -> {
            softly.assertThat(zoomModel.getLastScaleFactor()).isEqualTo(1.0);
            softly.assertThat(zoomModel.getLastUserInteractionStartX()).isEqualTo(0.0);
            softly.assertThat(zoomModel.getLastUserInteractionEndX()).isEqualTo(1.0);
            softly.assertThat(zoomModel.getLastUserInteractionWidthX()).isEqualTo(1.0);
        });
    }

    @Test
    void getCurrentZoomTarget_returns_last_set_zoom_target() {
        // Arrange
        var canvas = mock(JPanel.class);
        var frameBox = new FrameBox<>("test", 0.1, 0.9, 0);
        var zoomTarget = new ZoomTarget<>(0, 0, 100, 100, frameBox);
        doAnswer(invocation -> {
            Rectangle rect = invocation.getArgument(0);
            rect.setBounds(0, 0, 100, 100);
            return null;
        }).when(canvas).computeVisibleRect(any(Rectangle.class));

        // Act
        zoomModel.recordLastPositionFromZoomTarget(canvas, zoomTarget);

        // Assert
        assertThat(zoomModel.getCurrentZoomTarget()).isSameAs(zoomTarget);
    }

    @Test
    void getLastScaleFactor_returns_calculated_scale_from_zoom_target() {
        // Arrange
        var canvas = mock(JPanel.class);
        var frameBox = new FrameBox<>("test", 0.0, 0.5, 0); // 50% width frame
        var zoomTarget = new ZoomTarget<>(0, 0, 200, 100, frameBox); // width=200

        doAnswer(invocation -> {
            Rectangle rect = invocation.getArgument(0);
            rect.setBounds(0, 0, 600, 400); // visible width = 600
            return null;
        }).when(canvas).computeVisibleRect(any(Rectangle.class));

        // Act
        zoomModel.recordLastPositionFromZoomTarget(canvas, zoomTarget);

        // Assert
        // scaleFactor = visibleWidth / (targetWidth * 1.0) = 600 / (200 * 1.0) = 3.0
        assertThat(zoomModel.getLastScaleFactor()).isEqualTo(3.0);
    }

    @Test
    void multiple_recordLastPositionFromZoomTarget_calls_update_state() {
        // Arrange
        var canvas = mock(JPanel.class);

        var frameBox1 = new FrameBox<>("test1", 0.0, 0.5, 0);
        var zoomTarget1 = new ZoomTarget<>(0, 0, 100, 50, frameBox1);

        var frameBox2 = new FrameBox<>("test2", 0.3, 0.7, 1);
        var zoomTarget2 = new ZoomTarget<>(50, 25, 200, 100, frameBox2);

        doAnswer(invocation -> {
            Rectangle rect = invocation.getArgument(0);
            rect.setBounds(0, 0, 400, 300);
            return null;
        }).when(canvas).computeVisibleRect(any(Rectangle.class));

        // Act - first call
        zoomModel.recordLastPositionFromZoomTarget(canvas, zoomTarget1);

        // Verify first state
        assertThat(zoomModel.getCurrentZoomTarget()).isSameAs(zoomTarget1);
        assertThat(zoomModel.getLastUserInteractionStartX()).isEqualTo(0.0);
        assertThat(zoomModel.getLastUserInteractionEndX()).isEqualTo(0.5);

        // Act - second call
        zoomModel.recordLastPositionFromZoomTarget(canvas, zoomTarget2);

        // Assert - state should be updated
        assertSoftly(softly -> {
            softly.assertThat(zoomModel.getCurrentZoomTarget()).isSameAs(zoomTarget2);
            softly.assertThat(zoomModel.getLastUserInteractionStartX()).isCloseTo(0.3, within(1e-10));
            softly.assertThat(zoomModel.getLastUserInteractionEndX()).isCloseTo(0.7, within(1e-10));
            softly.assertThat(zoomModel.getLastUserInteractionWidthX()).isCloseTo(0.4, within(1e-10));
        });
    }

    @Test
    void zoom_target_and_user_interaction_can_be_interleaved() {
        // Arrange
        var canvas = mock(JPanel.class);
        when(canvas.getWidth()).thenReturn(500);

        var frameBox = new FrameBox<>("test", 0.1, 0.6, 0);
        var zoomTarget = new ZoomTarget<>(0, 0, 250, 100, frameBox);

        doAnswer(invocation -> {
            Rectangle rect = invocation.getArgument(0);
            rect.setBounds(50, 0, 200, 300);
            return null;
        }).when(canvas).computeVisibleRect(any(Rectangle.class));

        // Act - zoom target
        zoomModel.recordLastPositionFromZoomTarget(canvas, zoomTarget);
        assertThat(zoomModel.getCurrentZoomTarget()).isSameAs(zoomTarget);

        // Act - user interaction (should preserve zoomTarget but update positions)
        zoomModel.recordLastPositionFromUserInteraction(canvas);

        // Assert - zoomTarget unchanged, but positions updated from user interaction
        assertSoftly(softly -> {
            // Note: recordLastPositionFromUserInteraction doesn't update currentZoomTarget
            softly.assertThat(zoomModel.getCurrentZoomTarget()).isSameAs(zoomTarget);
            // User interaction values: startX = 50/500 = 0.1, endX = (50+200)/500 = 0.5
            softly.assertThat(zoomModel.getLastUserInteractionStartX()).isEqualTo(0.1);
            softly.assertThat(zoomModel.getLastUserInteractionEndX()).isEqualTo(0.5);
        });
    }
}