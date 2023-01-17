/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.views.cpu

import io.github.bric3.fireplace.JFRBinder
import io.github.bric3.fireplace.JfrAnalyzer
import io.github.bric3.fireplace.ui.ThreadFlamegraphView

class MethodCpuSample(jfrBinder: JFRBinder) : ThreadFlamegraphView(jfrBinder) {
    override val identifier = "CPU"
    override val eventSelector = JfrAnalyzer::executionSamples
}