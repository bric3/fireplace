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

import io.github.bric3.fireplace.flamegraph.FlamegraphView.FlamegraphCanvas;
import io.github.bric3.fireplace.flamegraph.FlamegraphView.ZoomAction;
import org.pushingpixels.radiance.animation.api.Timeline;
import org.pushingpixels.radiance.animation.api.ease.Sine;
import org.pushingpixels.radiance.animation.api.swing.EventDispatchThreadTimelineCallbackAdapter;

import javax.swing.*;
import java.awt.*;

/**
 * A zoom action that incorporates animation using <a href="https://github.com/kirill-grouchnikov/radiance/blob/sunshine/docs/animation/animation.md">Radiance Animation</a>
 */
public class ZoomAnimation implements ZoomAction {

    private static final long ZOOM_ANIMATION_DURATION = 400L;

    /**
     * A key for a system property that can be used to disable zoom animations.  Used to set the
     * initial state of the `animateZoomTransitions` flag.
     */
    private static final String ZOOM_ANIMATION_DISABLED_KEY = "fireplace.zoom.animation.disabled";

    /**
     * A flag controlling whether zoom transitions are animated.  Defaults to true unless a
     * system property is set to disable it (`-Dfireplace.zoom.animation.disabled=true`).
     */
    private boolean animateZoomTransitions = !Boolean.getBoolean(ZOOM_ANIMATION_DISABLED_KEY);

    public <T> void install(final FlamegraphView<T> flameGraph) {
        flameGraph.overrideZoomAction(this);
    }

    /**
     * Returns the flag that controls whether zoom transitions are animated.  The default
     * value is {@code true} unless the System property {@code fireplace.zoom.animation.disabled}
     * is set to {@code true} (this provides a way to switch off the feature if required).
     *
     * @return A boolean.
     */
    public boolean isAnimateZoomTransitions() {
        return animateZoomTransitions;
    }

    /**
     * Sets the flag that controls whether zoom transitions are animated.
     *
     * @param animateZoomTransitions the new flag value.
     */
    public void setAnimateZoomTransitions(boolean animateZoomTransitions) {
        this.animateZoomTransitions = animateZoomTransitions;
    }

    @Override
    public <T> boolean zoom(JViewport viewPort, FlamegraphCanvas<T> canvas, ZoomTarget zoomTarget) {
        System.getLogger(FlamegraphCanvas.class.getName()).log(System.Logger.Level.DEBUG, () -> "zoom to " + zoomTarget);
        if (!isAnimateZoomTransitions()) {
            return false;
        }
        int startW = canvas.getWidth();
        int startH = canvas.getHeight();
        double deltaW = zoomTarget.width - startW;
        double deltaH = zoomTarget.height - startH;

        int startX = canvas.getLocation().x;
        int startY = canvas.getLocation().y;
        double deltaX = zoomTarget.x - startX;
        double deltaY = zoomTarget.y - startY;


        Timeline.builder()
                .setDuration(ZOOM_ANIMATION_DURATION)
                .setEase(new Sine())
                .addCallback(new EventDispatchThreadTimelineCallbackAdapter() {
                    @Override
                    public void onTimelineStateChanged(Timeline.TimelineState oldState, Timeline.TimelineState newState, float durationFraction, float timelinePosition) {
                        if (newState.equals(Timeline.TimelineState.DONE)) {
                            // throw in a final update to the target position, because the last pulse
                            // might not have reached exactly timelinePosition = 1.0...
                            canvas.zoom(zoomTarget);
                        }
                    }

                    @Override
                    public void onTimelinePulse(float durationFraction, float timelinePosition) {
                        canvas.zoom(new ZoomTarget(
                                startX + (int) (timelinePosition * deltaX),
                                startY + (int) (timelinePosition * deltaY),
                                (int) (startW + timelinePosition * deltaW),
                                (int) (startH + timelinePosition * deltaH)
                        ));
                    }
                })
                .build()
                .playSkipping(3L);
        return true;
    }
}
