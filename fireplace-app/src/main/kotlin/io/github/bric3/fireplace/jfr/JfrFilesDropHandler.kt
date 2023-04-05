/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.jfr

import io.github.bric3.fireplace.ui.toolkit.DragAndDropTarget
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer
import javax.swing.*

internal class JfrFilesDropHandler private constructor(private val pathsHandler: Consumer<List<Path>>) :
    TransferHandler() {
    override fun canImport(support: TransferSupport): Boolean {
        return support.dataFlavors.any(DataFlavor::isFlavorJavaFileListType)
    }

    override fun importData(support: TransferSupport): Boolean {
        if (!this.canImport(support)) {
            return false
        }
        @Suppress("UNCHECKED_CAST")
        val files = try {
            support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
        } catch (ex: UnsupportedFlavorException) {
            ex.printStackTrace()
            return false
        } catch (ex: IOException) {
            ex.printStackTrace()
            return false
        }
        val isJfrFiles = files.all { it.isFile && it.name.endsWith(".jfr") }
        if (!isJfrFiles) {
            return false
        }
        pathsHandler.accept(files.map(File::toPath))
        return true
    }

    companion object {
        fun install(pathsHandler: Consumer<List<Path>>, parent: JComponent, target: DragAndDropTarget) {
            try {
                parent.dropTarget = DropTarget(parent, object : DropTargetAdapter() {
                    override fun dragEnter(dtde: DropTargetDragEvent) {
                        if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            target.activate()
                        }
                    }

                    override fun drop(dtde: DropTargetDropEvent) {
                        /* no-op */
                    }
                })
                target.component.apply {
                    transferHandler = JfrFilesDropHandler(pathsHandler)
                    dropTarget.addDropTargetListener(object : DropTargetAdapter() {
                        override fun dragExit(dte: DropTargetEvent) {
                            target.deactivate()
                        }

                        override fun drop(dtde: DropTargetDropEvent) {
                            target.deactivate()
                        }

                        override fun dropActionChanged(dtde: DropTargetDragEvent) {
                            target.deactivate()
                        }
                    })
                }
            } catch (e: TooManyListenersException) {
                e.printStackTrace()
            }
        }
    }
}