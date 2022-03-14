package com.github.bric3.fireplace;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;

import javax.swing.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.stream.Collectors.toUnmodifiableList;

class JFRBinder {
    private final List<Consumer<IItemCollection>> eventsBinders = new ArrayList<>();
    private final List<Consumer<List<Path>>> pathsBinders = new ArrayList<>();
    private Runnable onLoadStart;
    private Runnable onLoadEnd;

    public void bindPaths(Consumer<List<Path>> pathsBinder) {
        pathsBinders.add(paths -> SwingUtilities.invokeLater(() -> pathsBinder.accept(paths)));
    }

    public <T> void bindEvents(Function<IItemCollection, T> provider, Consumer<T> componentUpdate) {
        Consumer<IItemCollection> binder = events ->
                CompletableFuture.supplyAsync(() -> provider.apply(events))
                                 .whenComplete((result, throwable) -> {
                                     if (throwable != null) {
                                         throwable.printStackTrace();
                                     } else {
                                         SwingUtilities.invokeLater(() -> componentUpdate.accept(result));
                                     }
                                 });

        eventsBinders.add(binder);
    }

    public void load(List<Path> jfrPaths) {
        if (jfrPaths.isEmpty()) {
            return;
        }
        onLoadStart.run();
        CompletableFuture.runAsync(() -> {
            pathsBinders.forEach(pathsBinder -> pathsBinder.accept(jfrPaths));

            var jfrFiles = jfrPaths.stream()
                                   .peek(path -> System.out.println("Loading " + path))
                                   .map(Path::toFile)
                                   .collect(toUnmodifiableList());


            var eventSupplier = Utils.memoize(() -> CompletableFuture.supplyAsync(() -> {
                IItemCollection events = null;
                try {
                    events = JfrLoaderToolkit.loadEvents(jfrFiles);
                } catch (IOException ioe) {
                    throw new UncheckedIOException(ioe);
                } catch (CouldNotLoadRecordingException e1) {
                    throw new RuntimeException(e1);
                }
                events.stream()
                      .flatMap(IItemIterable::stream)
                      .map(IItem::getType)
                      .map(IType::getIdentifier)
                      .distinct()
                      .forEach(System.out::println);

                return events;
            }).join());


            eventsBinders.forEach(binder -> binder.accept(eventSupplier.get()));

            onLoadEnd.run();
        });
    }

    public void setOnLoadActions(Runnable onLoadStart, Runnable onLoadEnd) {
        this.onLoadStart = () -> SwingUtilities.invokeLater(onLoadStart);
        this.onLoadEnd = () -> SwingUtilities.invokeLater(onLoadEnd);
    }
}
