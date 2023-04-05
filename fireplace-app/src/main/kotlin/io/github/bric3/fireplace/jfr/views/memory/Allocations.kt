/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.jfr.views.memory

import io.github.bric3.fireplace.jfr.JFRLoaderBinder
import io.github.bric3.fireplace.jfr.JfrAnalyzer
import io.github.bric3.fireplace.ui.ThreadFlamegraphView

class Allocations(jfrBinder: JFRLoaderBinder) : ThreadFlamegraphView(jfrBinder) {
    override val identifier = "Allocations"

    override val eventSelector = JfrAnalyzer::allocInOutTlab
}