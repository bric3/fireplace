package io.github.bric3.fireplace;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

final class JfrFilesDropHandler extends TransferHandler {
    private final Consumer<List<Path>> pathsHandler;

    private JfrFilesDropHandler(Consumer<List<Path>> pathsHandler) {
        this.pathsHandler = pathsHandler;
    }

    public static void install(Consumer<List<Path>> pathsHandler, JComponent parent, DnDTarget target) {
        try {
            parent.setDropTarget(new DropTarget(parent, new DropTargetAdapter() {
                @Override
                public void dragEnter(DropTargetDragEvent dtde) {
                    var dataFlavorSupported = dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
                    if (dataFlavorSupported) {
                        target.activate();
                    }
                }

                @Override
                public void drop(DropTargetDropEvent dtde) {/* no-op */}
            }));

            target.getComponent().setTransferHandler(new JfrFilesDropHandler(pathsHandler));
            target.getComponent().getDropTarget().addDropTargetListener(new DropTargetAdapter() {
                @Override
                public void dragExit(DropTargetEvent dte) {
                    target.deactivate();
                }

                @Override
                public void drop(DropTargetDropEvent dtde) {
                    target.deactivate();
                }

                @Override
                public void dropActionChanged(DropTargetDragEvent dtde) {
                    target.deactivate();
                }
            });
        } catch (TooManyListenersException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        return Arrays.stream(support.getDataFlavors())
                     .anyMatch(DataFlavor::isFlavorJavaFileListType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean importData(TransferHandler.TransferSupport support) {
        if (!this.canImport(support)) {
            return false;
        }

        List<File> files;
        try {
            files = (List<File>) support.getTransferable()
                                        .getTransferData(DataFlavor.javaFileListFlavor);
        } catch (UnsupportedFlavorException | IOException ex) {
            ex.printStackTrace();
            return false;
        }

        var isJfrFiles = files.stream()
                              .allMatch(f -> f.isFile() && f.getName().endsWith(".jfr"));
        if (!isJfrFiles) {
            return false;
        }

        pathsHandler.accept(files.stream().map(File::toPath).collect(toList()));
        return true;
    }
}