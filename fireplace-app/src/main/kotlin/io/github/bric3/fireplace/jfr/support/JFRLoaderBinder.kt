/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.jfr.support

import io.github.bric3.fireplace.Utils
import org.openjdk.jmc.common.item.IItemCollection
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collectors.toUnmodifiableList
import javax.swing.SwingUtilities

class JFRLoaderBinder {
    private val eventsBinders: MutableList<Consumer<IItemCollection>> = mutableListOf()
    private val pathsBinders: MutableList<Consumer<List<Path>>> = mutableListOf()
    private lateinit var onLoadStart: Runnable
    private lateinit var onLoadEnd: Runnable
    private lateinit var eventSupplierFuture: CompletableFuture<Supplier<IItemCollection>>

    /**
     * Bind a component to computed events.
     *
     * The [provider] function is called with the events as parameter and runs on the fork/join pool.
     * The [componentUpdate] function is called with the result of the [provider] function and runs on the EDT.
     */
    fun <T> bindEvents(provider: Function<IItemCollection, T>, componentUpdate: Consumer<T>) {
        val eventBinder: (IItemCollection) -> Unit = { events: IItemCollection ->
            CompletableFuture.supplyAsync { provider.apply(events) }
                .whenComplete { result, throwable ->
                    if (throwable != null) {
                        throwable.printStackTrace()
                    } else {
                        SwingUtilities.invokeLater { componentUpdate.accept(result) }
                    }
                }
        }
        eventsBinders.add(eventBinder)

        // run immediately if future already done
        if ((this::eventSupplierFuture.isInitialized && eventSupplierFuture.isDone)) {
            eventSupplierFuture.thenAcceptAsync { eventSupplier ->
                eventBinder(eventSupplier.get())
            }
        }
    }

    fun bindPaths(pathsBinder: Consumer<List<Path>>) {
        pathsBinders.add { paths -> SwingUtilities.invokeLater { pathsBinder.accept(paths) } }
    }

    internal fun loadJfrFiles(jfrPaths: List<Path>) {
        if (jfrPaths.isEmpty()) {
            return
        }
        onLoadStart.run()
        CompletableFuture.runAsync {
            pathsBinders.forEach { it.accept(jfrPaths) }
        }

        eventSupplierFuture = CompletableFuture.supplyAsync {
            val jfrFiles = jfrPaths.stream()
                .peek { path -> println("Loading $path") }
                .map { path -> path.toFile() }
                .collect(toUnmodifiableList())

            return@supplyAsync Utils.memoize {
                CompletableFuture.supplyAsync {
                    val events: IItemCollection = try {
                        JfrLoaderToolkit.loadEvents(jfrFiles)
                    } catch (ioe: IOException) {
                        throw UncheckedIOException(ioe)
                    } catch (e1: CouldNotLoadRecordingException) {
                        throw RuntimeException(e1)
                    }

                    if (Utils.isDebugging) {
                        TypeCategoryExtractor.extract(events)
                            .forEach { (type, category) -> println("$type -> $category") }
                    }

                    events
                }.join()
            }
        }

        eventSupplierFuture.thenAcceptAsync { eventSupplier ->
            eventsBinders.forEach { binder -> binder.accept(eventSupplier.get()) }
        }.whenCompleteAsync { _, throwable ->
            throwable?.printStackTrace()
            onLoadEnd.run()
        }
    }

    internal fun setOnLoadActions(onLoadStart: Runnable?, onLoadEnd: Runnable?) {
        this.onLoadStart = Runnable { SwingUtilities.invokeLater(onLoadStart) }
        this.onLoadEnd = Runnable { SwingUtilities.invokeLater(onLoadEnd) }
    }
}