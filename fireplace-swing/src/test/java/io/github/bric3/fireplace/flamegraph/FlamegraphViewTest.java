package io.github.bric3.fireplace.flamegraph;

import io.github.bric3.fireplace.flamegraph.FlamegraphView.Mode;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class FlamegraphViewTest {

    @Test
    void basicApi() {
        var fg = new FlamegraphView<String>();

        assertSoftly(softly -> {
            var component = fg.component;
            softly.assertThat(FlamegraphView.<String>from(component)).contains(fg);
            softly.assertThat(FlamegraphView.<String>from(new JPanel())).isEmpty();
        });
        // non configured
        assertSoftly(softly -> {
            softly.assertThat(fg.getFrameModel()).isEqualTo(FrameModel.empty());
            softly.assertThat(fg.getFrames()).isEmpty();
            softly.assertThat(fg.isFrameGapEnabled()).isFalse();

            softly.assertThat(fg.getMode()).isEqualTo(Mode.ICICLEGRAPH);
            softly.assertThat(fg.isShowMinimap()).isTrue();
            softly.assertThat(fg.isShowHoveredSiblings()).isFalse();

            softly.assertThat(fg.getFrameColorProvider()).isEqualTo(null);
            softly.assertThat(fg.getFrameFontProvider()).isEqualTo(null);
            softly.assertThat(fg.getFrameTextsProvider()).isEqualTo(null);
        });

        // after configuration
        assertSoftly(softly -> {
            var frameTextsProvider = FrameTextsProvider.<String>empty();
            var frameColorProvider = FrameColorProvider.<String>defaultColorProvider(box -> Color.BLACK);
            var frameFontProvider = FrameFontProvider.<String>defaultFontProvider();
            fg.setRenderConfiguration(
                    frameTextsProvider,
                    frameColorProvider,
                    frameFontProvider
            );
            softly.assertThat(fg.getFrameTextsProvider()).isEqualTo(frameTextsProvider);
            softly.assertThat(fg.getFrameColorProvider()).isEqualTo(frameColorProvider);
            softly.assertThat(fg.getFrameFontProvider()).isEqualTo(frameFontProvider);



            var frameTextsProvider2 = FrameTextsProvider.<String>empty();
            fg.setFrameTextsProvider(frameTextsProvider2);
            softly.assertThat(fg.getFrameTextsProvider()).isEqualTo(frameTextsProvider2);

            var frameColorProvider2 = FrameColorProvider.<String>defaultColorProvider(box -> Color.BLACK);
            fg.setFrameColorProvider(frameColorProvider2);
            softly.assertThat(fg.getFrameColorProvider()).isEqualTo(frameColorProvider2);

            var frameFontProvider2 = FrameFontProvider.<String>defaultFontProvider();
            fg.setFrameFontProvider(frameFontProvider2);
            softly.assertThat(fg.getFrameFontProvider()).isEqualTo(frameFontProvider2);


            var frameModel = new FrameModel<String>(
                    "title",
                    (a, b) -> Objects.equals(a.actualNode, b.actualNode),
                    List.of(new FrameBox<>("frame1", 0.0, 0.5, 1))
            );
            fg.setModel(frameModel);
            softly.assertThat(fg.getFrameModel()).isEqualTo(frameModel);
            softly.assertThat(fg.getFrames()).isEqualTo(frameModel.frames);


            // non configured
            fg.setFrameGapEnabled(true);
            softly.assertThat(fg.isFrameGapEnabled()).isTrue();

            fg.setMode(Mode.FLAMEGRAPH);
            softly.assertThat(fg.getMode()).isEqualTo(Mode.FLAMEGRAPH);

            fg.setShowMinimap(false);
            softly.assertThat(fg.isShowMinimap()).isFalse();

            fg.setShowHoveredSiblings(true);
            softly.assertThat(fg.isShowHoveredSiblings()).isTrue();
        });
    }
}