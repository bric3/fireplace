package io.github.bric3.fireplace.ui;

import javax.swing.*;

public interface DragAndDropTarget {
    JComponent getComponent();

    void activate();

    void deactivate();
}
