/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.jfr

import io.github.bric3.fireplace.core.ui.LightDarkColor
import io.github.bric3.fireplace.flamegraph.FrameBox
import org.openjdk.jmc.common.IMCFrame
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node
import java.awt.Color
import java.util.function.Function
import java.util.regex.Pattern

/**
 * A JFR specific color mapping modes.
 */
enum class JfrFrameColorMode {
    BY_PACKAGE {
        private val runtimePrefixes = Pattern.compile("(java\\.|javax\\.|sun\\.|com\\.sun\\.|com\\.oracle\\.|com\\.ibm\\.|jdk\\.)")
        public override fun getJfrNodeColor(colorMapper: Function<Any, Color>, frameNode: Node): Color {
            if (frameNode.isRoot) {
                return rootNodeColor
            }
            val frame = frameNode.frame
            if (frame.type === IMCFrame.Type.UNKNOWN) {
                return undefinedColor
            }
            val type = frame.type
            if (type == IMCFrame.Type.NATIVE || type == IMCFrame.Type.KERNEL || type == IMCFrame.Type.CPP) {
                return runtimeColor
            }
            val name = frame.method.type.getPackage().name
            if (name != null && runtimePrefixes.matcher(name).lookingAt()) {
                return runtimeColor
            }
            return colorMapper.apply(name)
        }
    },
    BY_MODULE {
        public override fun getJfrNodeColor(colorMapper: Function<Any, Color>, frameNode: Node): Color {
            return if (frameNode.isRoot) {
                rootNodeColor
            } else {
                colorMapper.apply(frameNode.frame.method.type.getPackage().module)
            }
        }
    },
    BY_FRAME_TYPE {
        public override fun getJfrNodeColor(colorMapper: Function<Any, Color>, frameNode: Node): Color {
            if (frameNode.isRoot) {
                return rootNodeColor
            }
            return when (frameNode.frame.type) {
                IMCFrame.Type.INTERPRETED -> interpretedColor
                IMCFrame.Type.INLINED -> inlinedColor
                IMCFrame.Type.JIT_COMPILED -> jitCompiledColor
                else -> undefinedColor
            }
        }
    };

    protected abstract fun getJfrNodeColor(colorMapper: Function<Any, Color>, frameNode: Node): Color
    fun colorMapperUsing(colorMapper: Function<Any, Color>): Function<FrameBox<Node>, Color> {
        return Function { frameNode -> getJfrNodeColor(colorMapper, frameNode.actualNode) }
    }

    companion object {
        var rootNodeColor = LightDarkColor(
            0xFF_77_4A_A4.toInt(),
            0xFF_56_1D_8C.toInt()
        )
        var runtimeColor = LightDarkColor(
            0xFF_D1_D4_DE.toInt(),
            0xFF_3B_39_3D.toInt()
        )
        var undefinedColor = Color(108, 163, 189)
        var jitCompiledColor = Color(21, 110, 64)
        var inlinedColor: Color = Color.pink
        var interpretedColor: Color = Color.orange
    }
}