package io.github.bric3.fireplace.ui.toolkit

import java.awt.Color
import javax.swing.*

object UIUtil {

    // Some properties come from FlatLaf
    // https://www.formdev.com/flatlaf/components/borders/
    // https://www.formdev.com/flatlaf/components/
    object Colors {
        val borderColor: Color
            get() = UIManager.getColor("Component.borderColor")
        val borderWidth: Color
            get() = UIManager.getColor("Component.borderWidth")

        val backgroundColor: Color
            get() = UIManager.getColor("Panel.background")

        val foregroundColor: Color
            get() = UIManager.getColor("Panel.background")
    }
}