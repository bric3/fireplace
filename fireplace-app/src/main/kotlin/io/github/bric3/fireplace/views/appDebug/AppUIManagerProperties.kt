/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.views.appDebug

import io.github.bric3.fireplace.Utils
import io.github.bric3.fireplace.simpleReadOnlyTable
import io.github.bric3.fireplace.ui.ViewPanel
import javax.swing.UIManager

class AppUIManagerProperties : ViewPanel {
    override val identifier: String = "App UIManager properties"

    override val view by lazy {
        // UIManager.getLookAndFeelDefaults()
        //     .entries
        //     .stream()
        //     .filter { (_, value): Entry<Any?, Any?> -> value is Color }
        //     .map { (key, value): Entry<Any, Any> ->
        //         "$key: " + String.format(
        //             "#%06X",
        //             0xFFFFFF and (value as Color).rgb
        //         )
        //     }
        //     .sorted()
        //     .map {  }

        simpleReadOnlyTable(
            UIManager.getLookAndFeelDefaults()
                .entries
                .map { arrayOf(it.key, it.value) }
                .toTypedArray(),
            arrayOf("Key", "Value")
        )
    }

    companion object {
        fun isActive(): Boolean = Utils.isFireplaceDebug
    }
}
