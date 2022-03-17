package com.github.bric3.fireplace;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class FirePlaceSwtMain {

    private Shell shell;

    public static void main(String[] args) {
        if (ProcessHandle.current().info().commandLine().stream().noneMatch(s -> s.contains("-XstartOnFirstThread")) &&
            System.getProperty("os.name").toLowerCase().contains("mac")) {
            System.err.println("java command requires -XstartOnFirstThread on macOs");
            System.exit(1);
        }

        Display.setAppName("FirePlace SWT Experiment");
        Display.setAppVersion("0.0.0");
        var display = new Display();
        // display.asyncExec(() -> {
        //
        // });


        Shell shell = new FirePlaceSwtMain().launchApp(display, args);
        // shell.pack();
        shell.open();
        shell.forceActive();

        // Set up the SWT event loop
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                // If no more entries in the event queue
                display.sleep();
            }
        }
        display.dispose();
    }

    private Shell launchApp(Display display, String[] args) {
        shell = new Shell(display, SWT.BORDER | SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.RESIZE);
        shell.setText("FirePlace SWT Experiment");
        shell.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent event) {
                // do something
            }
        });

        shell.setLayout(new FillLayout(SWT.VERTICAL));

        // Adding widgets to the shell
        Text helloText = new Text(shell, SWT.CENTER);
        helloText.setText("Hello SWT!");
        helloText.pack();

        Composite composite = new Composite(shell, SWT.CENTER);
        var text = new Text(composite, SWT.CENTER);
        text.setText("test");
        text.pack();
        composite.pack();


        return shell;
    }
}
