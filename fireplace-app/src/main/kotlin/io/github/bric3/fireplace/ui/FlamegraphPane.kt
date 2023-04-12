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
import io.github.bric3.fireplace.core.ui.LightDarkColor
import io.github.bric3.fireplace.core.ui.SwingUtils
import io.github.bric3.fireplace.flamegraph.*
import io.github.bric3.fireplace.flamegraph.animation.ZoomAnimation
import io.github.bric3.fireplace.jfr.support.JfrFrameColorMode
import io.github.bric3.fireplace.jfr.support.JfrFrameColorMode.BY_PACKAGE
import io.github.bric3.fireplace.jfr.support.JfrFrameNodeConverter
import io.github.bric3.fireplace.ui.toolkit.BalloonToolTip
import org.openjdk.jmc.common.util.FormatToolkit
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors.joining
import java.util.stream.Collectors.toCollection
import javax.swing.*
import javax.swing.Timer

class FlamegraphPane : JPanel(BorderLayout()) {
    private var jfrFlamegraphView: FlamegraphView<Node>
    private var dataApplier: Consumer<FlamegraphView<Node>>? = null

    init {
        jfrFlamegraphView = getJfrFlamegraphView().apply {
            isShowMinimap = defaultShowMinimap
            configureCanvas { component: JComponent -> registerToolTips(component) }
            putClientProperty(FlamegraphView.SHOW_STATS, true)
            setTooltipComponentSupplier { BalloonToolTip() }
        }
        val minimapShade = LightDarkColor(
            Colors.translucent_white_80,
            Colors.translucent_black_40
        ).also {
            jfrFlamegraphView.setMinimapShadeColorSupplier { it }
        }
        val zoomAnimation = ZoomAnimation().also { it.install(jfrFlamegraphView) }
        val colorPaletteJComboBox = JComboBox(Colors.Palette.values()).apply {
            selectedItem = defaultColorPalette
        }
        val colorModeJComboBox = JComboBox(JfrFrameColorMode.values()).apply {
            selectedItem = defaultFrameColorMode
        }

        val updateColorSettingsListener = ActionListener {
            val frameBoxColorFunction = (colorModeJComboBox.selectedItem as JfrFrameColorMode)
                .colorMapperUsing(
                    ColorMapper.ofObjectHashUsing(
                        *(colorPaletteJComboBox.selectedItem as Colors.Palette).colors()
                    )
                )
            jfrFlamegraphView.frameColorProvider = DimmingFrameColorProvider(frameBoxColorFunction)
            jfrFlamegraphView.requestRepaint()
        }.also {
            colorPaletteJComboBox.addActionListener(it)
            colorModeJComboBox.addActionListener(it)
        }

        val icicleModeToggle = JCheckBox("Icicle").apply {
            addActionListener {
                jfrFlamegraphView.mode =
                    if (isSelected)
                        FlamegraphView.Mode.ICICLEGRAPH
                    else
                        FlamegraphView.Mode.FLAMEGRAPH
                jfrFlamegraphView.requestRepaint()
            }
            isSelected = defaultIcicleMode
        }

        val minimapToggle = JCheckBox("Minimap").apply {
            addActionListener { jfrFlamegraphView.setShowMinimap(isSelected) }
            isSelected = defaultShowMinimap
        }
        val animateToggle = JCheckBox("Animate").apply {
            addActionListener { _: ActionEvent? ->
                zoomAnimation.isAnimateZoomTransitions = isSelected
            }
            isSelected = true
        }
        val wrapper = JPanel(BorderLayout()).apply {
            add(jfrFlamegraphView.component.apply {
                border = null
            })
        }
        val resetZoom = JButton("1:1").apply {
            addActionListener { jfrFlamegraphView.resetZoom() }
        }
        val searchField = JTextField("").apply {
            addActionListener {
                val searched = text
                if (searched.isEmpty()) {
                    jfrFlamegraphView.highlightFrames(emptySet(), searched)
                    return@addActionListener
                }

                CompletableFuture.runAsync {
                    try {
                        val matches = jfrFlamegraphView.frames
                            .stream()
                            .filter { frame ->
                                val method = frame.actualNode.frame.method
                                (method.methodName.contains(searched)
                                        || method.type.typeName.contains(searched)
                                        || method.type.getPackage().name != null
                                        && method.type.getPackage().name.contains(searched))
                                        || method.type.getPackage().module != null
                                        && method.type.getPackage().module.name.contains(searched)
                                        || method.formalDescriptor.replace('/', '.').contains(searched)
                            }
                            .collect(toCollection { Collections.newSetFromMap(IdentityHashMap()) })
                        jfrFlamegraphView.highlightFrames(matches, searched)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }
        val controlPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(colorPaletteJComboBox)
            add(colorModeJComboBox)
            add(icicleModeToggle)
            add(animateToggle)
            add(minimapToggle)
            Utils.ifDebugging {
                add(
                    refreshToggle(
                        icicleModeToggle,
                        minimapToggle,
                        minimapShade,
                        zoomAnimation,
                        updateColorSettingsListener,
                        wrapper
                    )
                )
            }
            add(resetZoom)
            add(searchField)
        }
        add(controlPanel, BorderLayout.NORTH)
        add(wrapper, BorderLayout.CENTER)
    }

    private fun refreshToggle(
        icicleModeToggle: JCheckBox,
        minimapToggle: JCheckBox,
        minimapShade: LightDarkColor,
        zoomAnimation: ZoomAnimation,
        updateColorSettingsListener: ActionListener,
        wrapper: JPanel
    ): JToggleButton {
        val timer = Timer(2000) {
            jfrFlamegraphView = getJfrFlamegraphView().apply {
                isShowMinimap = defaultShowMinimap
                mode = if (defaultIcicleMode) FlamegraphView.Mode.ICICLEGRAPH else FlamegraphView.Mode.FLAMEGRAPH
                icicleModeToggle.isSelected = defaultIcicleMode
                minimapToggle.isSelected = defaultShowMinimap
                configureCanvas { component: JComponent -> registerToolTips(component) }
                putClientProperty(FlamegraphView.SHOW_STATS, true)
                setMinimapShadeColorSupplier { minimapShade }
                zoomAnimation.install(this)
                dataApplier?.accept(this)
            }
            updateColorSettingsListener.actionPerformed(null)
            wrapper.run {
                removeAll()
                add(jfrFlamegraphView.component.apply {
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

    fun setStacktraceTreeModelAsync(stacktraceTreeModel: StacktraceTreeModel) {
        CompletableFuture.runAsync {
            dataApplier = dataApplier(stacktraceTreeModel).also {
                it.accept(jfrFlamegraphView)
            }
        }
    }

    private fun dataApplier(stacktraceTreeModel: StacktraceTreeModel): Consumer<FlamegraphView<Node>> {
        val flatFrameList = JfrFrameNodeConverter.convert(stacktraceTreeModel)
        val title = stacktraceTreeModel.items
            .stream()
            .map { itemsIterable -> itemsIterable.type.identifier }
            .collect(joining(", ", "all (", ")"))

        return Consumer { flameGraph ->
            SwingUtils.invokeLater {
                flameGraph.setModel(
                    FrameModel(
                        title,
                        { a, b -> a.actualNode.frame == b.actualNode.frame },
                        flatFrameList
                    ).withDescription(title)
                )
            }
        }
    }

    companion object {
        private val defaultColorPalette = Colors.Palette.DATADOG
        private val defaultFrameColorMode = BY_PACKAGE
        private const val defaultPaintFrameBorder = true
        private const val defaultShowMinimap = true
        private const val defaultIcicleMode = true
        private fun getJfrFlamegraphView(): FlamegraphView<Node> {
            val flamegraphView = FlamegraphView<Node>()
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
                DimmingFrameColorProvider(defaultFrameColorMode.colorMapperUsing(ColorMapper.ofObjectHashUsing(*defaultColorPalette.colors()))),
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
                    append("<br><hr>")
                    append(frame.actualNode.cumulativeWeight)
                    append(" ")
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