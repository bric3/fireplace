package com.github.bric3.fireplace;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

final class JfrFilesDropHandler extends TransferHandler {
    private final Consumer<List<Path>> pathsHandler;

    JfrFilesDropHandler(Consumer<List<Path>> pathsHandler) {this.pathsHandler = pathsHandler;}

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