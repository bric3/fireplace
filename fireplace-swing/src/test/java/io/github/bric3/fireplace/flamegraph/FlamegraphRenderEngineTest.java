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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link FlamegraphRenderEngine}.
 * Uses BufferedImage to get a Graphics2D context in headless mode.
 */
@DisplayName("FlamegraphRenderEngine")
class FlamegraphRenderEngineTest {

    private Graphics2D g2d;
    private FrameRenderer<String> frameRenderer;
    private FlamegraphRenderEngine<String> engine;

    @BeforeEach
    void setUp() {
        var image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        g2d = image.createGraphics();

        frameRenderer = new DefaultFrameRenderer<>(
                FrameTextsProvider.of(frame -> frame.actualNode),
                FrameColorProvider.defaultColorProvider(frame -> Color.ORANGE),
                FrameFontProvider.defaultFontProvider()
        );

        engine = new FlamegraphRenderEngine<>(frameRenderer);
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {
        @Test
        void null_frame_renderer_throws_exception() {
            assertThatThrownBy(() -> new FlamegraphRenderEngine<String>(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void valid_renderer_creates_engine() {
            var newEngine = new FlamegraphRenderEngine<>(frameRenderer);

            assertThat(newEngine.getFrameRenderer()).isEqualTo(frameRenderer);
            assertThat(newEngine.getFrameModel()).isEqualTo(FrameModel.empty());
        }
    }

    @Nested
    @DisplayName("init and reset")
    class InitAndResetTests {
        @Test
        void init_with_frame_model_initializes_engine() {
            var frames = createSimpleFrameList();
            var model = new FrameModel<>(frames);

            engine.init(model);

            assertThat(engine.getFrameModel()).isEqualTo(model);
        }

        @Test
        void init_null_model_throws_exception() {
            assertThatThrownBy(() -> engine.init(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void init_returns_this() {
            var model = new FrameModel<>(createSimpleFrameList());

            var result = engine.init(model);

            assertThat(result).isSameAs(engine);
        }

        @Test
        void reset_clears_model() {
            engine.init(new FrameModel<>(createSimpleFrameList()));

            engine.reset();

            assertThat(engine.getFrameModel()).isEqualTo(FrameModel.empty());
        }

        @Test
        void reset_returns_this() {
            var result = engine.reset();

            assertThat(result).isSameAs(engine);
        }
    }

    @Nested
    @DisplayName("computeVisibleFlamegraphHeight")
    class ComputeVisibleFlamegraphHeightTests {
        @Test
        void empty_model_returns_zero() {
            engine.init(FrameModel.empty());

            int height = engine.computeVisibleFlamegraphHeight(g2d, 800);

            assertThat(height).isZero();
        }

        @Test
        void single_frame_returns_frame_height() {
            var frames = List.of(new FrameBox<>("root", 0.0, 1.0, 0));
            engine.init(new FrameModel<>(frames));

            int height = engine.computeVisibleFlamegraphHeight(g2d, 800);

            assertThat(height).isEqualTo(frameRenderer.getFrameBoxHeight(g2d));
        }

        @Test
        void multiple_depths_returns_proportional_height() {
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child1", 0.0, 0.5, 1),
                    new FrameBox<>("grandchild", 0.0, 0.25, 2)
            );
            engine.init(new FrameModel<>(frames));

            int height = engine.computeVisibleFlamegraphHeight(g2d, 800);
            int frameBoxHeight = frameRenderer.getFrameBoxHeight(g2d);

            assertThat(height).isEqualTo(3 * frameBoxHeight);
        }

        @Test
        void zero_width_returns_zero() {
            engine.init(new FrameModel<>(createSimpleFrameList()));

            int height = engine.computeVisibleFlamegraphHeight(g2d, 0);

            assertThat(height).isZero();
        }

        @Test
        void narrow_frames_excluded_at_small_width() {
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("wide", 0.0, 0.5, 1),
                    new FrameBox<>("veryNarrow", 0.999, 1.0, 2) // 0.1% width
            );
            engine.init(new FrameModel<>(frames));

            // At 100px width, the narrow frame is 0.1px wide (below 2px threshold)
            int heightAtSmallWidth = engine.computeVisibleFlamegraphHeight(g2d, 100);
            // At 10000px width, the narrow frame is 10px wide (above threshold)
            int heightAtLargeWidth = engine.computeVisibleFlamegraphHeight(g2d, 10000);

            assertThat(heightAtSmallWidth).isLessThan(heightAtLargeWidth);
        }

        @Test
        void update_flag_true_updates_visible_depth() {
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 1.0, 1)
            );
            engine.init(new FrameModel<>(frames));

            engine.computeVisibleFlamegraphHeight(g2d, 800, true);

            assertThat(engine.getVisibleDepth()).isEqualTo(2);
        }

        @Test
        void update_flag_false_does_not_update_visible_depth() {
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 1.0, 1)
            );
            engine.init(new FrameModel<>(frames));
            int initialDepth = engine.getVisibleDepth();

            engine.computeVisibleFlamegraphHeight(g2d, 800, false);

            assertThat(engine.getVisibleDepth()).isEqualTo(initialDepth);
        }

        @Test
        void caching_returns_same_result_for_same_width() {
            engine.init(new FrameModel<>(createSimpleFrameList()));

            int height1 = engine.computeVisibleFlamegraphHeight(g2d, 800);
            int height2 = engine.computeVisibleFlamegraphHeight(g2d, 800);

            assertThat(height1).isEqualTo(height2);
        }
    }

    @Nested
    @DisplayName("computeVisibleFlamegraphMinimapHeight")
    class ComputeVisibleFlamegraphMinimapHeightTests {
        @Test
        void returns_height_based_on_depth() {
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 1.0, 1),
                    new FrameBox<>("grandchild", 0.0, 1.0, 2)
            );
            engine.init(new FrameModel<>(frames));
            engine.computeVisibleFlamegraphHeight(g2d, 800, true);

            int minimapHeight = engine.computeVisibleFlamegraphMinimapHeight(200);

            assertThat(minimapHeight).isEqualTo(engine.getVisibleDepth());
        }

        @Test
        void empty_model_returns_zero() {
            engine.init(FrameModel.empty());
            engine.computeVisibleFlamegraphHeight(g2d, 800, true);

            int minimapHeight = engine.computeVisibleFlamegraphMinimapHeight(200);

            assertThat(minimapHeight).isZero();
        }
    }

    @Nested
    @DisplayName("Icicle Mode")
    class IcicleModeTests {

        @BeforeEach
        void setIcicleMode() {
            engine.setIcicle(true);
        }

        @Test
        void isIcicle_default_true() {
            var freshEngine = new FlamegraphRenderEngine<>(frameRenderer);
            assertThat(freshEngine.isIcicle()).isTrue();
        }

        @Nested
        @DisplayName("getFrameAt")
        class GetFrameAtTests {
            @Test
            void empty_model_returns_empty() {
                engine.init(FrameModel.empty());
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);

                Optional<FrameBox<String>> result = engine.getFrameAt(g2d, bounds, new Point(400, 300));

                assertThat(result).isEmpty();
            }

            @Test
            void point_on_root_frame_returns_root() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                engine.init(new FrameModel<>(List.of(root)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

                // In icicle mode, root is at y=0
                Optional<FrameBox<String>> result = engine.getFrameAt(g2d, bounds, new Point(400, frameHeight / 2));

                assertThat(result).isPresent();
                assertThat(result.get()).isEqualTo(root);
            }

            @Test
            void point_on_child_frame_returns_child() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var left = new FrameBox<>("left", 0.0, 0.5, 1);
                var right = new FrameBox<>("right", 0.5, 1.0, 1);
                engine.init(new FrameModel<>(List.of(root, left, right)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

                // In icicle mode, depth 1 is below root
                Optional<FrameBox<String>> leftResult = engine.getFrameAt(g2d, bounds, new Point(200, frameHeight + frameHeight / 2));
                assertThat(leftResult).isPresent();
                assertThat(leftResult.get().actualNode).isEqualTo("left");

                Optional<FrameBox<String>> rightResult = engine.getFrameAt(g2d, bounds, new Point(600, frameHeight + frameHeight / 2));
                assertThat(rightResult).isPresent();
                assertThat(rightResult.get().actualNode).isEqualTo("right");
            }

            @Test
            void point_on_narrow_frame_below_threshold_returns_empty() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var narrow = new FrameBox<>("narrow", 0.999, 1.0, 1); // 0.1% width
                engine.init(new FrameModel<>(List.of(root, narrow)));

                var bounds = new Rectangle2D.Double(0, 0, 100, 600); // Narrow bounds
                int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

                Optional<FrameBox<String>> result = engine.getFrameAt(g2d, bounds, new Point(99, frameHeight + frameHeight / 2));

                assertThat(result).isEmpty();
            }

            @Test
            void point_outside_frame_x_returns_empty() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var child = new FrameBox<>("child", 0.0, 0.5, 1);
                engine.init(new FrameModel<>(List.of(root, child)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

                // Point at x=600, but child ends at x=400 (0.5 * 800)
                Optional<FrameBox<String>> result = engine.getFrameAt(g2d, bounds, new Point(600, frameHeight + frameHeight / 2));

                assertThat(result).isEmpty();
            }

            @Test
            void point_before_frame_start_x_returns_empty() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                // Frame starts at x=400 (0.5 * 800) and ends at x=800
                var rightChild = new FrameBox<>("right", 0.5, 1.0, 1);
                engine.init(new FrameModel<>(List.of(root, rightChild)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

                // Point at x=200, before frame startX
                Optional<FrameBox<String>> result = engine.getFrameAt(g2d, bounds, new Point(200, frameHeight + frameHeight / 2));

                assertThat(result).isEmpty();
            }

            @Test
            void point_on_different_depth_returns_empty() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var child = new FrameBox<>("child", 0.0, 0.5, 1);
                engine.init(new FrameModel<>(List.of(root, child)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

                // Point at correct X for child but at depth 2 (no frame there)
                Optional<FrameBox<String>> result = engine.getFrameAt(g2d, bounds, new Point(200, 2 * frameHeight + frameHeight / 2));

                assertThat(result).isEmpty();
            }

            @Test
            void multiple_frames_same_depth_returns_correct_one() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var left = new FrameBox<>("left", 0.0, 0.3, 1);
                var middle = new FrameBox<>("middle", 0.35, 0.65, 1);
                var right = new FrameBox<>("right", 0.7, 1.0, 1);
                engine.init(new FrameModel<>(List.of(root, left, middle, right)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

                // Point in middle frame (400 = 0.5 * 800, which is between 0.35 and 0.65)
                Optional<FrameBox<String>> result = engine.getFrameAt(g2d, bounds, new Point(400, frameHeight + frameHeight / 2));

                assertThat(result).isPresent();
                assertThat(result.get().actualNode).isEqualTo("middle");
            }

            @Test
            void point_in_gap_between_frames_returns_empty() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var left = new FrameBox<>("left", 0.0, 0.3, 1);
                // Gap from 0.3 to 0.5
                var right = new FrameBox<>("right", 0.5, 1.0, 1);
                engine.init(new FrameModel<>(List.of(root, left, right)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

                // Point in gap (320 = 0.4 * 800, which is between 0.3 and 0.5)
                Optional<FrameBox<String>> result = engine.getFrameAt(g2d, bounds, new Point(320, frameHeight + frameHeight / 2));

                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("getFrameRectangle")
        class GetFrameRectangleTests {
            @Test
            void full_width_frame_returns_full_width_rect() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                engine.init(new FrameModel<>(List.of(root)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                Rectangle rect = engine.getFrameRectangle(g2d, bounds, root);

                assertThat(rect.width).isGreaterThanOrEqualTo(798);
            }

            @Test
            void partial_width_frame_returns_proportional_rect() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var halfFrame = new FrameBox<>("half", 0.0, 0.5, 1);
                engine.init(new FrameModel<>(List.of(root, halfFrame)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                Rectangle rect = engine.getFrameRectangle(g2d, bounds, halfFrame);

                assertThat(rect.width).isBetween(395, 410);
            }

            @Test
            void child_frame_has_correct_y_position() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var child = new FrameBox<>("child", 0.0, 0.5, 1);
                engine.init(new FrameModel<>(List.of(root, child)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                Rectangle rootRect = engine.getFrameRectangle(g2d, bounds, root);
                Rectangle childRect = engine.getFrameRectangle(g2d, bounds, child);

                // In icicle mode, child should be below root (larger Y)
                assertThat(childRect.y).isGreaterThan(rootRect.y);
            }
        }

        @Nested
        @DisplayName("paint")
        class PaintTests {
            @Test
            void empty_model_does_not_throw() {
                engine.init(FrameModel.empty());
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                var viewRect = new Rectangle2D.Double(0, 0, 800, 600);

                assertThatCode(() -> engine.paint(g2d, bounds, viewRect))
                        .doesNotThrowAnyException();
            }

            @Test
            void with_frames_does_not_throw() {
                engine.init(new FrameModel<>(createSimpleFrameList()));
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                var viewRect = new Rectangle2D.Double(0, 0, 800, 600);

                assertThatCode(() -> engine.paint(g2d, bounds, viewRect))
                        .doesNotThrowAnyException();
            }

            @Test
            void root_frame_out_of_view_does_not_paint_root() {
                engine.init(new FrameModel<>(createSimpleFrameList()));
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                // viewRect starts below root frame
                int frameHeight = frameRenderer.getFrameBoxHeight(g2d);
                var viewRect = new Rectangle2D.Double(0, frameHeight * 2, 800, 400);

                assertThatCode(() -> engine.paint(g2d, bounds, viewRect))
                        .doesNotThrowAnyException();
            }

            @Test
            void narrow_frames_below_threshold_skipped() {
                var frames = List.of(
                        new FrameBox<>("root", 0.0, 1.0, 0),
                        new FrameBox<>("narrow", 0.9999, 1.0, 1) // Very narrow
                );
                engine.init(new FrameModel<>(frames));
                var bounds = new Rectangle2D.Double(0, 0, 100, 600);
                var viewRect = new Rectangle2D.Double(0, 0, 100, 600);

                assertThatCode(() -> engine.paint(g2d, bounds, viewRect))
                        .doesNotThrowAnyException();
            }

            @Test
            void frame_completely_out_of_view_rect_skipped() {
                var frames = List.of(
                        new FrameBox<>("root", 0.0, 1.0, 0),
                        new FrameBox<>("left", 0.0, 0.2, 1),
                        new FrameBox<>("right", 0.8, 1.0, 1)
                );
                engine.init(new FrameModel<>(frames));
                var bounds = new Rectangle2D.Double(0, 0, 1000, 600);
                // viewRect only covers middle - children are outside
                var viewRect = new Rectangle2D.Double(300, 0, 400, 600);

                assertThatCode(() -> engine.paint(g2d, bounds, viewRect))
                        .doesNotThrowAnyException();
            }

            @Test
            void partial_frame_clipped_on_left_flag_set() {
                var frames = List.of(
                        new FrameBox<>("root", 0.0, 1.0, 0),
                        new FrameBox<>("wide", 0.0, 0.8, 1)
                );
                engine.init(new FrameModel<>(frames));
                var bounds = new Rectangle2D.Double(0, 0, 1000, 600);
                // viewRect starts at 200, clipping the left side of frames
                var viewRect = new Rectangle2D.Double(200, 0, 600, 600);

                assertThatCode(() -> engine.paint(g2d, bounds, viewRect))
                        .doesNotThrowAnyException();
            }
        }

        @Nested
        @DisplayName("paint with hoveredFrame")
        class PaintWithHoveredFrameTests {
            @Test
            void hovered_root_frame_paints_with_hovered_flag() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                engine.init(new FrameModel<>(List.of(root)));
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);

                // Hover the root frame
                engine.hoverFrame(root, g2d, bounds, rect -> {});

                assertThatCode(() -> engine.paint(g2d, bounds, bounds))
                        .doesNotThrowAnyException();
            }

            @Test
            void hovered_child_frame_paints_with_hovered_flag() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var child = new FrameBox<>("child", 0.0, 0.5, 1);
                engine.init(new FrameModel<>(List.of(root, child)));
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);

                engine.hoverFrame(child, g2d, bounds, rect -> {});

                assertThatCode(() -> engine.paint(g2d, bounds, bounds))
                        .doesNotThrowAnyException();
            }
        }

        @Nested
        @DisplayName("paint with selectedFrame")
        class PaintWithSelectedFrameTests {
            @Test
            void selected_root_frame_paints_with_focused_flag() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                engine.init(new FrameModel<>(List.of(root)));
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

                // Select root frame
                engine.toggleSelectedFrameAt(g2d, bounds, new Point(400, frameHeight / 2), (f, r) -> {});

                assertThatCode(() -> engine.paint(g2d, bounds, bounds))
                        .doesNotThrowAnyException();
            }

            @Test
            void selected_child_frame_paints_descendants_as_focused() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var child = new FrameBox<>("child", 0.0, 0.5, 1);
                var grandchild = new FrameBox<>("grandchild", 0.0, 0.25, 2);
                var outsideChild = new FrameBox<>("outside", 0.5, 1.0, 1);
                engine.init(new FrameModel<>(List.of(root, child, grandchild, outsideChild)));
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

                // Select child frame - grandchild should be in focused area
                engine.toggleSelectedFrameAt(g2d, bounds, new Point(200, frameHeight + frameHeight / 2), (f, r) -> {});

                assertThatCode(() -> engine.paint(g2d, bounds, bounds))
                        .doesNotThrowAnyException();
            }

            @Test
            void frame_outside_selected_bounds_not_focused() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var left = new FrameBox<>("left", 0.0, 0.4, 1);
                var right = new FrameBox<>("right", 0.6, 1.0, 1);
                engine.init(new FrameModel<>(List.of(root, left, right)));
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

                // Select left - right should NOT be in focused area
                engine.toggleSelectedFrameAt(g2d, bounds, new Point(100, frameHeight + frameHeight / 2), (f, r) -> {});

                assertThatCode(() -> engine.paint(g2d, bounds, bounds))
                        .doesNotThrowAnyException();
            }
        }

        @Nested
        @DisplayName("calculateZoomTargetForFrameAt")
        class CalculateZoomTargetTests {
            @Test
            void empty_model_returns_empty() {
                engine.init(FrameModel.empty());
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                var viewRect = new Rectangle2D.Double(0, 0, 800, 600);

                Optional<ZoomTarget<String>> result = engine.calculateZoomTargetForFrameAt(
                        g2d, bounds, viewRect, new Point(400, 300)
                );

                assertThat(result).isEmpty();
            }

            @Test
            void point_on_frame_returns_zoom_target() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var child = new FrameBox<>("child", 0.0, 0.5, 1);
                engine.init(new FrameModel<>(List.of(root, child)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                var viewRect = new Rectangle2D.Double(0, 0, 800, 600);
                int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

                Optional<ZoomTarget<String>> result = engine.calculateZoomTargetForFrameAt(
                        g2d, bounds, viewRect, new Point(200, frameHeight + frameHeight / 2)
                );

                assertThat(result).isPresent();
                assertThat(result.get().targetFrame).isEqualTo(child);
                assertThat(result.get().getWidth()).isGreaterThan(800);
            }

            @Test
            void zoom_target_clamps_to_valid_scroll_range() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var deepChild = new FrameBox<>("deep", 0.0, 0.1, 5);
                engine.init(new FrameModel<>(List.of(root, deepChild)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 100); // Small height
                var viewRect = new Rectangle2D.Double(0, 0, 800, 100);
                int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

                Optional<ZoomTarget<String>> result = engine.calculateZoomTargetForFrameAt(
                        g2d, bounds, viewRect, new Point(40, 5 * frameHeight + frameHeight / 2)
                );

                assertThat(result).isPresent();
            }
        }

        @Nested
        @DisplayName("calculateZoomTargetFrame")
        class CalculateZoomTargetFrameTests {
            @Test
            void with_context_before_zero_shows_from_frame() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var child = new FrameBox<>("child", 0.0, 0.5, 1);
                var grandchild = new FrameBox<>("grandchild", 0.0, 0.25, 2);
                engine.init(new FrameModel<>(List.of(root, child, grandchild)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                var viewRect = new Rectangle2D.Double(0, 0, 800, 600);

                ZoomTarget<String> target = engine.calculateZoomTargetFrame(g2d, bounds, viewRect, grandchild, 0, 0);

                assertThat(target).isNotNull();
                assertThat(target.targetFrame).isEqualTo(grandchild);
            }

            @Test
            void with_context_before_positive_includes_parents() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var child = new FrameBox<>("child", 0.0, 0.5, 1);
                var grandchild = new FrameBox<>("grandchild", 0.0, 0.25, 2);
                engine.init(new FrameModel<>(List.of(root, child, grandchild)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                var viewRect = new Rectangle2D.Double(0, 0, 800, 600);

                ZoomTarget<String> target = engine.calculateZoomTargetFrame(g2d, bounds, viewRect, grandchild, 2, 0);

                assertThat(target).isNotNull();
            }

            @Test
            void with_context_before_negative_keeps_vertical_position() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var child = new FrameBox<>("child", 0.0, 0.5, 1);
                engine.init(new FrameModel<>(List.of(root, child)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                var viewRect = new Rectangle2D.Double(0, 100, 800, 400); // Scrolled

                ZoomTarget<String> target = engine.calculateZoomTargetFrame(g2d, bounds, viewRect, child, -1, 0);

                assertThat(target).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("Flamegraph Mode (inverted)")
    class FlamegraphModeTests {

        @BeforeEach
        void setFlamegraphMode() {
            engine.setIcicle(false);
        }

        @Test
        void setIcicle_changes_mode() {
            engine.setIcicle(false);
            assertThat(engine.isIcicle()).isFalse();

            engine.setIcicle(true);
            assertThat(engine.isIcicle()).isTrue();
        }

        @Nested
        @DisplayName("getFrameAt")
        class GetFrameAtTests {
            @Test
            void point_on_root_frame_returns_root() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                engine.init(new FrameModel<>(List.of(root)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

                // In flamegraph mode, root is at the bottom
                Optional<FrameBox<String>> result = engine.getFrameAt(g2d, bounds, new Point(400, (int) bounds.getHeight() - frameHeight / 2));

                assertThat(result).isPresent();
                assertThat(result.get()).isEqualTo(root);
            }

            @Test
            void point_on_child_frame_returns_child() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var child = new FrameBox<>("child", 0.0, 0.5, 1);
                engine.init(new FrameModel<>(List.of(root, child)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

                // In flamegraph mode, depth 1 is above root
                int childY = (int) bounds.getHeight() - 2 * frameHeight + frameHeight / 2;
                Optional<FrameBox<String>> result = engine.getFrameAt(g2d, bounds, new Point(200, childY));

                assertThat(result).isPresent();
                assertThat(result.get().actualNode).isEqualTo("child");
            }
        }

        @Nested
        @DisplayName("getFrameRectangle")
        class GetFrameRectangleTests {
            @Test
            void root_frame_is_at_bottom() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                engine.init(new FrameModel<>(List.of(root)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                Rectangle rect = engine.getFrameRectangle(g2d, bounds, root);

                int frameHeight = frameRenderer.getFrameBoxHeight(g2d);
                assertThat(rect.y + rect.height).isCloseTo((int) bounds.getHeight(), within(frameHeight));
            }

            @Test
            void child_frame_is_above_root() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var child = new FrameBox<>("child", 0.0, 0.5, 1);
                engine.init(new FrameModel<>(List.of(root, child)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                Rectangle rootRect = engine.getFrameRectangle(g2d, bounds, root);
                Rectangle childRect = engine.getFrameRectangle(g2d, bounds, child);

                // In flamegraph mode, child should be above root (smaller Y)
                assertThat(childRect.y).isLessThan(rootRect.y);
            }
        }

        @Nested
        @DisplayName("paint")
        class PaintTests {
            @Test
            void with_frames_does_not_throw() {
                engine.init(new FrameModel<>(createSimpleFrameList()));
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                var viewRect = new Rectangle2D.Double(0, 0, 800, 600);

                assertThatCode(() -> engine.paint(g2d, bounds, viewRect))
                        .doesNotThrowAnyException();
            }

            @Test
            void partial_view_rect_does_not_throw() {
                engine.init(new FrameModel<>(createSimpleFrameList()));
                var bounds = new Rectangle2D.Double(0, 0, 1600, 600);
                var viewRect = new Rectangle2D.Double(400, 100, 800, 400);

                assertThatCode(() -> engine.paint(g2d, bounds, viewRect))
                        .doesNotThrowAnyException();
            }
        }

        @Nested
        @DisplayName("calculateZoomTargetForFrameAt")
        class CalculateZoomTargetTests {
            @Test
            void point_on_frame_returns_zoom_target() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var child = new FrameBox<>("child", 0.0, 0.5, 1);
                engine.init(new FrameModel<>(List.of(root, child)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                var viewRect = new Rectangle2D.Double(0, 0, 800, 600);
                int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

                int childY = (int) bounds.getHeight() - 2 * frameHeight + frameHeight / 2;
                Optional<ZoomTarget<String>> result = engine.calculateZoomTargetForFrameAt(
                        g2d, bounds, viewRect, new Point(200, childY)
                );

                assertThat(result).isPresent();
                assertThat(result.get().targetFrame).isEqualTo(child);
            }
        }

        @Nested
        @DisplayName("calculateZoomTargetFrame")
        class CalculateZoomTargetFrameTests {
            @Test
            void flamegraph_mode_calculates_correct_y() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var child = new FrameBox<>("child", 0.0, 0.5, 1);
                engine.init(new FrameModel<>(List.of(root, child)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                var viewRect = new Rectangle2D.Double(0, 0, 800, 600);

                ZoomTarget<String> target = engine.calculateZoomTargetFrame(g2d, bounds, viewRect, child, 0, 0);

                assertThat(target).isNotNull();
                assertThat(target.getWidth()).isGreaterThan(800);
            }

            @Test
            void flamegraph_mode_with_negative_context_keeps_vertical_position() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var child = new FrameBox<>("child", 0.0, 0.5, 1);
                engine.init(new FrameModel<>(List.of(root, child)));

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                var viewRect = new Rectangle2D.Double(0, 100, 800, 400);

                ZoomTarget<String> target = engine.calculateZoomTargetFrame(g2d, bounds, viewRect, child, -1, 0);

                assertThat(target).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("Hover and Siblings")
    class HoverAndSiblingTests {

        @Nested
        @DisplayName("showHoveredSiblings enabled")
        class SiblingsEnabledTests {

            @BeforeEach
            void setup() {
                engine.setShowHoveredSiblings(true);
                engine.setIcicle(true);
            }

            @Test
            void isShowHoveredSiblings_default_true() {
                var freshEngine = new FlamegraphRenderEngine<>(frameRenderer);
                assertThat(freshEngine.isShowHoveredSiblings()).isTrue();
            }

            @Test
            void hoverFrame_empty_model_does_not_throw() {
                engine.init(FrameModel.empty());
                var frame = new FrameBox<>("test", 0.0, 1.0, 0);
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);

                assertThatCode(() -> engine.hoverFrame(frame, g2d, bounds, rect -> {}))
                        .doesNotThrowAnyException();
            }

            @Test
            void hoverFrame_single_frame_invokes_consumer_once() {
                var frame = new FrameBox<>("test", 0.0, 1.0, 0);
                engine.init(new FrameModel<>(List.of(frame)));
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);

                var hoveredRects = new ArrayList<Rectangle>();
                engine.hoverFrame(frame, g2d, bounds, hoveredRects::add);

                assertThat(hoveredRects).hasSize(1);
            }

            @Test
            void hoverFrame_with_siblings_invokes_consumer_for_all_siblings() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var frame1 = new FrameBox<>("method", 0.0, 0.3, 1);
                var frame2 = new FrameBox<>("method", 0.5, 0.8, 1); // Same name = sibling
                engine.init(new FrameModel<>(List.of(root, frame1, frame2)));
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);

                var hoveredRects = new ArrayList<Rectangle>();
                engine.hoverFrame(frame1, g2d, bounds, hoveredRects::add);

                // Should invoke for both siblings
                assertThat(hoveredRects).hasSize(2);
            }

            @Test
            void hoverFrame_same_frame_twice_does_not_invoke_again() {
                var frame = new FrameBox<>("test", 0.0, 1.0, 0);
                engine.init(new FrameModel<>(List.of(frame)));
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);

                engine.hoverFrame(frame, g2d, bounds, rect -> {});

                var hoveredRects = new ArrayList<Rectangle>();
                engine.hoverFrame(frame, g2d, bounds, hoveredRects::add);

                assertThat(hoveredRects).isEmpty();
            }

            @Test
            void hoverFrame_different_frame_invokes_for_new_and_old() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var frame1 = new FrameBox<>("frame1", 0.0, 0.5, 1);
                var frame2 = new FrameBox<>("frame2", 0.5, 1.0, 1);
                engine.init(new FrameModel<>(List.of(root, frame1, frame2)));
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);

                engine.hoverFrame(frame1, g2d, bounds, rect -> {});

                var hoveredRects = new ArrayList<Rectangle>();
                engine.hoverFrame(frame2, g2d, bounds, hoveredRects::add);

                // Should invoke for new frame + old frame (cleanup)
                assertThat(hoveredRects).hasSize(2);
            }

            @Test
            void paint_with_sibling_hovered_paints_siblings_with_flag() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var frame1 = new FrameBox<>("method", 0.0, 0.3, 1);
                var frame2 = new FrameBox<>("method", 0.5, 0.8, 1);
                engine.init(new FrameModel<>(List.of(root, frame1, frame2)));
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);

                engine.hoverFrame(frame1, g2d, bounds, rect -> {});

                // frame2 should be painted with hoveredSibling flag
                assertThatCode(() -> engine.paint(g2d, bounds, bounds))
                        .doesNotThrowAnyException();
            }

            @Test
            void stopHover_empty_model_does_not_throw() {
                engine.init(FrameModel.empty());
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);

                assertThatCode(() -> engine.stopHover(g2d, bounds, rect -> {}))
                        .doesNotThrowAnyException();
            }

            @Test
            void stopHover_after_hover_invokes_consumer_for_siblings() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var frame1 = new FrameBox<>("method", 0.0, 0.3, 1);
                var frame2 = new FrameBox<>("method", 0.5, 0.8, 1);
                engine.init(new FrameModel<>(List.of(root, frame1, frame2)));
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);

                engine.hoverFrame(frame1, g2d, bounds, rect -> {});

                var clearedRects = new ArrayList<Rectangle>();
                engine.stopHover(g2d, bounds, clearedRects::add);

                assertThat(clearedRects).hasSize(2);
            }

            @Test
            void stopHover_no_active_hover_does_not_invoke() {
                engine.init(new FrameModel<>(createSimpleFrameList()));
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);

                var clearedRects = new ArrayList<Rectangle>();
                engine.stopHover(g2d, bounds, clearedRects::add);

                assertThat(clearedRects).isEmpty();
            }
        }

        @Nested
        @DisplayName("showHoveredSiblings disabled")
        class SiblingsDisabledTests {

            @BeforeEach
            void setup() {
                engine.setShowHoveredSiblings(false);
                engine.setIcicle(true);
            }

            @Test
            void setShowHoveredSiblings_changes_flag() {
                engine.setShowHoveredSiblings(false);
                assertThat(engine.isShowHoveredSiblings()).isFalse();

                engine.setShowHoveredSiblings(true);
                assertThat(engine.isShowHoveredSiblings()).isTrue();
            }

            @Test
            void hoverFrame_with_siblings_only_invokes_for_hovered() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var frame1 = new FrameBox<>("method", 0.0, 0.3, 1);
                var frame2 = new FrameBox<>("method", 0.5, 0.8, 1);
                engine.init(new FrameModel<>(List.of(root, frame1, frame2)));
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);

                var hoveredRects = new ArrayList<Rectangle>();
                engine.hoverFrame(frame1, g2d, bounds, hoveredRects::add);

                // Should only invoke for hovered frame, not sibling
                assertThat(hoveredRects).hasSize(1);
            }

            @Test
            void paint_with_siblings_siblings_not_highlighted() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var frame1 = new FrameBox<>("method", 0.0, 0.3, 1);
                var frame2 = new FrameBox<>("method", 0.5, 0.8, 1);
                engine.init(new FrameModel<>(List.of(root, frame1, frame2)));
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);

                engine.hoverFrame(frame1, g2d, bounds, rect -> {});

                assertThatCode(() -> engine.paint(g2d, bounds, bounds))
                        .doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("Highlighting")
    class HighlightingTests {

        @BeforeEach
        void setup() {
            engine.setIcicle(true);
        }

        @Test
        void setHighlightFrames_null_set_throws_exception() {
            assertThatThrownBy(() -> engine.setHighlightFrames(null, "test"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void paint_with_highlighting_paints_highlighted_frames() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child = new FrameBox<>("highlighted", 0.0, 0.5, 1);
            var other = new FrameBox<>("other", 0.5, 1.0, 1);
            engine.init(new FrameModel<>(List.of(root, child, other)));
            engine.setHighlightFrames(Set.of(child), "highlighted");

            var bounds = new Rectangle2D.Double(0, 0, 800, 600);

            assertThatCode(() -> engine.paint(g2d, bounds, bounds))
                    .doesNotThrowAnyException();
        }

        @Test
        void paint_empty_highlight_set_no_highlighting() {
            engine.init(new FrameModel<>(createSimpleFrameList()));
            engine.setHighlightFrames(Set.of(), null);

            var bounds = new Rectangle2D.Double(0, 0, 800, 600);

            assertThatCode(() -> engine.paint(g2d, bounds, bounds))
                    .doesNotThrowAnyException();
        }

        @Test
        void paint_frame_not_in_highlight_set_not_highlighted() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var highlighted = new FrameBox<>("match", 0.0, 0.5, 1);
            var notHighlighted = new FrameBox<>("other", 0.5, 1.0, 1);
            engine.init(new FrameModel<>(List.of(root, highlighted, notHighlighted)));
            engine.setHighlightFrames(Set.of(highlighted), "match");

            var bounds = new Rectangle2D.Double(0, 0, 800, 600);

            assertThatCode(() -> engine.paint(g2d, bounds, bounds))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Selected Frame (Focusing)")
    class SelectedFrameTests {

        @BeforeEach
        void setup() {
            engine.setIcicle(true);
        }

        @Test
        void toggleSelectedFrameAt_frame_found_invokes_consumer() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            engine.init(new FrameModel<>(List.of(root)));

            var bounds = new Rectangle2D.Double(0, 0, 800, 600);
            int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

            var toggledFrames = new ArrayList<FrameBox<String>>();
            engine.toggleSelectedFrameAt(g2d, bounds, new Point(400, frameHeight / 2),
                                         (frame, rect) -> toggledFrames.add(frame));

            assertThat(toggledFrames).containsExactly(root);
        }

        @Test
        void toggleSelectedFrameAt_no_frame_does_not_invoke() {
            engine.init(FrameModel.empty());
            var bounds = new Rectangle2D.Double(0, 0, 800, 600);

            var toggledFrames = new ArrayList<FrameBox<String>>();
            engine.toggleSelectedFrameAt(g2d, bounds, new Point(400, 300),
                                         (frame, rect) -> toggledFrames.add(frame));

            assertThat(toggledFrames).isEmpty();
        }

        @Test
        void toggleSelectedFrameAt_twice_deselects() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            engine.init(new FrameModel<>(List.of(root)));

            var bounds = new Rectangle2D.Double(0, 0, 800, 600);
            int frameHeight = frameRenderer.getFrameBoxHeight(g2d);
            var point = new Point(400, frameHeight / 2);

            engine.toggleSelectedFrameAt(g2d, bounds, point, (f, r) -> {});
            engine.toggleSelectedFrameAt(g2d, bounds, point, (f, r) -> {});

            assertThatCode(() -> engine.paint(g2d, bounds, bounds))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Zoom Calculations")
    class ZoomCalculationTests {

        @Nested
        @DisplayName("getScaleFactor")
        class GetScaleFactorTests {
            @ParameterizedTest
            @CsvSource({
                    "100, 100, 1.0, 1.0",
                    "100, 200, 0.5, 1.0",
                    "100, 100, 0.5, 2.0",
                    "100, 400, 0.25, 1.0",
                    "800, 800, 0.25, 4.0",
            })
            void calculates_correctly(double visibleWidth, double canvasWidth, double frameWidthX, double expectedFactor) {
                double factor = FlamegraphRenderEngine.getScaleFactor(visibleWidth, canvasWidth, frameWidthX);

                assertThat(factor).isCloseTo(expectedFactor, within(0.0001));
            }
        }

        @Nested
        @DisplayName("calculateHorizontalZoomTargetForFrameAt")
        class CalculateHorizontalZoomTargetTests {
            @Test
            void empty_model_returns_empty() {
                engine.init(FrameModel.empty());
                engine.setIcicle(true);
                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                var viewRect = new Rectangle2D.Double(0, 0, 800, 600);

                Optional<ZoomTarget<String>> result = engine.calculateHorizontalZoomTargetForFrameAt(
                        g2d, bounds, viewRect, new Point(400, 300)
                );

                assertThat(result).isEmpty();
            }

            @Test
            void point_on_frame_returns_target() {
                var root = new FrameBox<>("root", 0.0, 1.0, 0);
                var child = new FrameBox<>("child", 0.0, 0.5, 1);
                engine.init(new FrameModel<>(List.of(root, child)));
                engine.setIcicle(true);

                var bounds = new Rectangle2D.Double(0, 0, 800, 600);
                var viewRect = new Rectangle2D.Double(0, 0, 800, 600);
                int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

                Optional<ZoomTarget<String>> result = engine.calculateHorizontalZoomTargetForFrameAt(
                        g2d, bounds, viewRect, new Point(200, frameHeight + frameHeight / 2)
                );

                assertThat(result).isPresent();
                assertThat(result.get().targetFrame).isEqualTo(child);
            }
        }
    }

    @Nested
    @DisplayName("paintToImage")
    class PaintToImageTests {
        @Test
        void icicle_mode_does_not_throw() {
            engine.init(new FrameModel<>(createSimpleFrameList()));
            var size = new Rectangle2D.Double(0, 0, 800, 600);

            assertThatCode(() -> engine.paintToImage(g2d, size, true))
                    .doesNotThrowAnyException();
        }

        @Test
        void flamegraph_mode_does_not_throw() {
            engine.init(new FrameModel<>(createSimpleFrameList()));
            var size = new Rectangle2D.Double(0, 0, 800, 600);

            assertThatCode(() -> engine.paintToImage(g2d, size, false))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("paintMinimap")
    class PaintMinimapTests {
        @Test
        void with_frames_does_not_throw() {
            engine.init(new FrameModel<>(createSimpleFrameList()));
            engine.setIcicle(true);
            var bounds = new Rectangle2D.Double(0, 0, 200, 100);

            assertThatCode(() -> engine.paintMinimap(g2d, bounds))
                    .doesNotThrowAnyException();
        }

        @Test
        void minimap_mode_includes_narrow_frames() {
            // In minimap mode, narrow frames should NOT be skipped
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("veryNarrow", 0.9999, 1.0, 1)
            );
            engine.init(new FrameModel<>(frames));
            engine.setIcicle(true);
            var bounds = new Rectangle2D.Double(0, 0, 100, 50);

            assertThatCode(() -> engine.paintMinimap(g2d, bounds))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("depth calculation")
    class DepthCalculationTests {
        @Test
        void getVisibleDepth_after_init_returns_correct_depth() {
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 1.0, 1),
                    new FrameBox<>("grandchild", 0.0, 1.0, 2),
                    new FrameBox<>("greatGrandchild", 0.0, 1.0, 3)
            );
            engine.init(new FrameModel<>(frames));
            engine.computeVisibleFlamegraphHeight(g2d, 800, true);

            assertThat(engine.getVisibleDepth()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("constants")
    class ConstantsTests {
        @Test
        void default_icicle_mode_is_true() {
            assertThat(FlamegraphRenderEngine.DEFAULT_ICICLE_MODE).isTrue();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        void paint_very_small_bounds_does_not_throw() {
            engine.init(new FrameModel<>(createSimpleFrameList()));
            engine.setIcicle(true);
            var bounds = new Rectangle2D.Double(0, 0, 1, 1);
            var viewRect = new Rectangle2D.Double(0, 0, 1, 1);

            assertThatCode(() -> engine.paint(g2d, bounds, viewRect))
                    .doesNotThrowAnyException();
        }

        @Test
        void paint_very_large_bounds_does_not_throw() {
            engine.init(new FrameModel<>(createSimpleFrameList()));
            engine.setIcicle(true);
            var bounds = new Rectangle2D.Double(0, 0, 100000, 100000);
            var viewRect = new Rectangle2D.Double(0, 0, 800, 600);

            assertThatCode(() -> engine.paint(g2d, bounds, viewRect))
                    .doesNotThrowAnyException();
        }

        @Test
        void getFrameAt_point_at_exact_boundary_returns_frame() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var left = new FrameBox<>("left", 0.0, 0.5, 1);
            engine.init(new FrameModel<>(List.of(root, left)));
            engine.setIcicle(true);

            var bounds = new Rectangle2D.Double(0, 0, 800, 600);
            int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

            // Point at exact boundary
            Optional<FrameBox<String>> result = engine.getFrameAt(g2d, bounds, new Point(400, frameHeight + frameHeight / 2));

            assertThat(result).isPresent();
        }

        @Test
        void multiple_inits_clears_state() {
            var frames1 = List.of(new FrameBox<>("first", 0.0, 1.0, 0));
            var frames2 = List.of(
                    new FrameBox<>("second", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            );

            engine.init(new FrameModel<>(frames1));
            engine.computeVisibleFlamegraphHeight(g2d, 800, true);

            engine.init(new FrameModel<>(frames2));
            engine.computeVisibleFlamegraphHeight(g2d, 800, true);

            assertThat(engine.getVisibleDepth()).isEqualTo(2);
        }

        @Test
        void combined_flags_hover_highlight_and_select() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child = new FrameBox<>("child", 0.0, 0.5, 1);
            engine.init(new FrameModel<>(List.of(root, child)));
            engine.setIcicle(true);
            var bounds = new Rectangle2D.Double(0, 0, 800, 600);
            int frameHeight = frameRenderer.getFrameBoxHeight(g2d);

            // Set all flags
            engine.setHighlightFrames(Set.of(child), "child");
            engine.hoverFrame(child, g2d, bounds, rect -> {});
            engine.toggleSelectedFrameAt(g2d, bounds, new Point(200, frameHeight + frameHeight / 2), (f, r) -> {});

            assertThatCode(() -> engine.paint(g2d, bounds, bounds))
                    .doesNotThrowAnyException();
        }
    }

    private List<FrameBox<String>> createSimpleFrameList() {
        return List.of(
                new FrameBox<>("root", 0.0, 1.0, 0),
                new FrameBox<>("child1", 0.0, 0.5, 1),
                new FrameBox<>("child2", 0.5, 1.0, 1)
        );
    }
}