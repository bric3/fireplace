package io.github.bric3.fireplace;

import javax.swing.*;

public interface DnDTarget {
    JComponent getComponent();

    void activate();

    void deactivate();
}
