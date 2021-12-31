package com.github.bric3;

import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import javax.swing.JPanel;
import java.util.function.Supplier;

public class FlameGraphPanel extends JPanel {
    private final Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier;

    public FlameGraphPanel(Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier) {
        this.stacktraceTreeModelSupplier = stacktraceTreeModelSupplier;
    }



}
