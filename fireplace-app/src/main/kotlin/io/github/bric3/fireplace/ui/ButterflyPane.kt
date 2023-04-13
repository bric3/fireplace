/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.ui

import io.github.bric3.fireplace.Utils
import io.github.bric3.fireplace.core.ui.Colors
import io.github.bric3.fireplace.core.ui.SwingUtils
import io.github.bric3.fireplace.flamegraph.ButterflyView
import io.github.bric3.fireplace.flamegraph.ColorMapper
import io.github.bric3.fireplace.flamegraph.DimmingFrameColorProvider
import io.github.bric3.fireplace.flamegraph.FrameBox
import io.github.bric3.fireplace.flamegraph.FrameFontProvider
import io.github.bric3.fireplace.flamegraph.FrameModel
import io.github.bric3.fireplace.flamegraph.FrameTextsProvider
import io.github.bric3.fireplace.jfr.support.JfrFrameColorMode
import io.github.bric3.fireplace.jfr.support.JfrFrameColorMode.BY_PACKAGE
import io.github.bric3.fireplace.jfr.support.JfrFrameNodeConverter.convertButterfly
import io.github.bric3.fireplace.jfr.support.JfrFrameNodeConverter.predecessorsWeight
import io.github.bric3.fireplace.jfr.support.JfrFrameNodeConverter.successorsWeight
import io.github.bric3.fireplace.jfr.tree.Node
import io.github.bric3.fireplace.jfr.tree.StacktraceButterflyModel
import io.github.bric3.fireplace.ui.toolkit.BalloonToolTip
import org.openjdk.jmc.common.IMCFrame
import org.openjdk.jmc.common.util.FormatToolkit
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.function.Function
import java.util.regex.Pattern
import javax.swing.*

class ButterflyPane : JPanel(BorderLayout()) {
    private var jfrButterflyView: ButterflyView<Node>
    private var dataApplier: Consumer<ButterflyView<Node>>? = null

    init {
        jfrButterflyView = getJfrFlamegraphView().apply {
            // isShowMinimap = defaultShowMinimap
            configureCanvas { component: JComponent -> registerToolTips(component) }
            // putClientProperty(FlamegraphView.SHOW_STATS, true)
            setTooltipComponentSupplier { BalloonToolTip() }
        }
        // val minimapShade = LightDarkColor(
        //     Colors.translucent_white_80,
        //     Colors.translucent_black_40
        // ).also {
        //     // jfrFlamegraphView.setMinimapShadeColorSupplier { it }
        // }
        // val zoomAnimation = ZoomAnimation().also { it.install(jfrFlamegraphView) }
        val colorPaletteJComboBox = JComboBox(Colors.Palette.values()).apply {
            selectedItem = defaultColorPalette
        }

        val updateColorSettingsListener = ActionListener {
            val frameBoxColorFunction = colorMapping(
                ColorMapper.ofObjectHashUsing(
                    *(colorPaletteJComboBox.selectedItem as Colors.Palette).colors()
                )
            )
            jfrButterflyView.frameColorProvider = DimmingFrameColorProvider(frameBoxColorFunction)
            jfrButterflyView.requestRepaint()
        }.also {
            colorPaletteJComboBox.addActionListener(it)
        }

        // val minimapToggle = JCheckBox("Minimap").apply {
        //     addActionListener { jfrFlamegraphView.setShowMinimap(isSelected) }
        //     isSelected = defaultShowMinimap
        // }
        // val animateToggle = JCheckBox("Animate").apply {
        //     addActionListener { _: ActionEvent? ->
        //         zoomAnimation.isAnimateZoomTransitions = isSelected
        //     }
        //     isSelected = true
        // }
        val wrapper = JPanel(BorderLayout()).apply {
            add(jfrButterflyView.apply {
                border = null
            })
        }
        // val resetZoom = JButton("1:1").apply {
        //     addActionListener { jfrFlamegraphView.resetZoom() }
        // }
        val controlPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(colorPaletteJComboBox)
            Utils.ifDebugging {
                add(
                    refreshToggle(
                        updateColorSettingsListener,
                        wrapper
                    )
                )
            }
            // add(resetZoom)
        }
        add(controlPanel, BorderLayout.NORTH)
        add(wrapper, BorderLayout.CENTER)
    }

    private fun refreshToggle(
        updateColorSettingsListener: ActionListener,
        wrapper: JPanel
    ): JToggleButton {
        val timer = Timer(2000) {
            jfrButterflyView = getJfrFlamegraphView().apply {
                // isShowMinimap = defaultShowMinimap
                // minimapToggle.isSelected = defaultShowMinimap
                configureCanvas { component: JComponent -> registerToolTips(component) }
                // setMinimapShadeColorSupplier { minimapShade }
                // zoomAnimation.install(this)
                dataApplier?.accept(this)
            }
            updateColorSettingsListener.actionPerformed(null)
            wrapper.run {
                removeAll()
                add(jfrButterflyView.apply {
                    border = null
                })
                revalidate()
                repaint(1000)
            }
        }.apply {
            initialDelay = 0
            isRepeats = true
        }
        val refreshToggle = JToggleButton("Refresh").apply {
            addActionListener {
                if (timer.isRunning) {
                    timer.stop()
                } else {
                    timer.start()
                }
            }
        }
        return refreshToggle
    }

    fun setStacktraceButterflyModelAsync(stacktraceButterflyModel: StacktraceButterflyModel) {
        CompletableFuture.runAsync {
            dataApplier = dataApplier(stacktraceButterflyModel).also {
                it.accept(jfrButterflyView)
            }
        }
    }

    private fun dataApplier(stacktraceButterflyModel: StacktraceButterflyModel): Consumer<ButterflyView<Node>> {
        val predecessorsFlatFrameList =
            convertButterfly(stacktraceButterflyModel.predecessorsRoot, predecessorsWeight())
        val successorsFlatFrameList = convertButterfly(stacktraceButterflyModel.successorsRoot, successorsWeight())
        val title = stacktraceButterflyModel.focusedMethod.method.methodName
        return Consumer { flameGraph ->
            val frameEquality: (a: FrameBox<Node>, b: FrameBox<Node>) -> Boolean =
                { a, b -> a.actualNode.frame == b.actualNode.frame }
            SwingUtils.invokeLater {
                flameGraph.setModel(
                    FrameModel(
                        title,
                        frameEquality,
                        predecessorsFlatFrameList
                    ).withDescription(title),
                    FrameModel(
                        title,
                        frameEquality,
                        successorsFlatFrameList
                    ).withDescription(title),
                )
            }
        }
    }

    companion object {
        private val defaultColorPalette = Colors.Palette.DATADOG
        private val defaultFrameColorMode = BY_PACKAGE

        /**
         * Stupid method mimicking [JfrFrameColorMode.BY_PACKAGE.getJfrNodeColor] to accommodate
         * [io.github.bric3.fireplace.jfr.tree.Node] duplicata.
         */
        fun colorMapping(colorMapper: ColorMapper<Any?>): Function<FrameBox<Node>, Color> {
            val runtimePrefixes =
                Pattern.compile("(java\\.|javax\\.|sun\\.|com\\.sun\\.|com\\.oracle\\.|com\\.ibm\\.|jdk\\.)")

            fun byPackage(
                colorMapper: ColorMapper<Any?>,
                frameNode: Node
            ): Color {
                if (frameNode.isRoot) {
                    return JfrFrameColorMode.rootNodeColor
                }
                val frame = frameNode.frame
                if (frame.type === IMCFrame.Type.UNKNOWN) {
                    return JfrFrameColorMode.undefinedColor
                }
                val type = frame.type
                if (type == IMCFrame.Type.NATIVE || type == IMCFrame.Type.KERNEL || type == IMCFrame.Type.CPP) {
                    return JfrFrameColorMode.runtimeColor
                }
                val name = frame.method.type.getPackage().name
                if (name != null && runtimePrefixes.matcher(name).lookingAt()) {
                    return JfrFrameColorMode.runtimeColor
                }
                return colorMapper.apply(name)
            }
            return Function { frameBox: FrameBox<Node> ->
                byPackage(colorMapper, frameBox.actualNode)
            }
        }

        private fun getJfrFlamegraphView(): ButterflyView<Node> {
            val flamegraphView =
                ButterflyView<Node>()
            flamegraphView.setRenderConfiguration(
                FrameTextsProvider.of(
                    Function { frame -> if (frame.isRoot) "root" else frame.actualNode.frame.humanReadableShortString },
                    Function { frame ->
                        if (frame.isRoot) "" else FormatToolkit.getHumanReadable(
                            frame.actualNode.frame.method,
                            false,
                            false,
                            false,
                            false,
                            true,
                            false
                        )
                    },
                    Function { frame -> if (frame.isRoot) "" else frame.actualNode.frame.method.methodName }
                ),
                DimmingFrameColorProvider(colorMapping(
                    ColorMapper.ofObjectHashUsing(*defaultColorPalette.colors())
                )),
                FrameFontProvider.defaultFontProvider()
            )
            flamegraphView.setTooltipTextFunction { frameModel, frame ->
                if (frame.isRoot) {
                    return@setTooltipTextFunction frameModel.description
                }
                val method = frame.actualNode.frame.method
                val desc = FormatToolkit.getHumanReadable(
                    method,
                    false,
                    false,
                    true,
                    true,
                    true,
                    false,
                    false
                )
                buildString {
                    append("<html><b>")
                    append(frame.actualNode.frame.humanReadableShortString)
                    append("</b><br>")
                    append(desc)
                    append("<br><hr>Cumulative weight:")
                    append(frame.actualNode.cumulativeWeight)
                    append(" Weight:")
                    append(frame.actualNode.weight)
                    append("<br>BCI: ")
                    append(frame.actualNode.frame.bci ?: "N/A")
                    append(" Line number: ")
                    append(frame.actualNode.frame.frameLineNumber ?: "N/A")
                    append("<br></html>")
                }
            }
            return flamegraphView
        }

        private fun registerToolTips(component: JComponent) {
            val defaultInitialDelay = ToolTipManager.sharedInstance().initialDelay
            val defaultDismissDelay = ToolTipManager.sharedInstance().dismissDelay
            val defaultReshowDelay = ToolTipManager.sharedInstance().reshowDelay
            component.addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(me: MouseEvent) {
                    ToolTipManager.sharedInstance().initialDelay = 1000
                    ToolTipManager.sharedInstance().dismissDelay = 20000
                    ToolTipManager.sharedInstance().reshowDelay = 1000
                }

                override fun mouseExited(me: MouseEvent) {
                    ToolTipManager.sharedInstance().initialDelay = defaultInitialDelay
                    ToolTipManager.sharedInstance().dismissDelay = defaultDismissDelay
                    ToolTipManager.sharedInstance().reshowDelay = defaultReshowDelay
                }
            })
            ToolTipManager.sharedInstance().registerComponent(component)
        }
    }
}