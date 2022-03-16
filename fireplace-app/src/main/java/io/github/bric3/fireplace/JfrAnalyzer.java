package io.github.bric3.fireplace;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import java.util.Set;

public class JfrAnalyzer {


    static StacktraceTreeModel stackTraceAllocationFun(IItemCollection events) {
        var methodFrameSeparator = new FrameSeparator(FrameCategorization.METHOD, false);

        var allocCollection = events.apply(ItemFilters.type(Set.of(
                "jdk.ObjectAllocationInNewTLAB",
                "jdk.ObjectAllocationOutsideTLAB"
        )));

        return new StacktraceTreeModel(allocCollection,
                                       methodFrameSeparator,
                                       false
                //                                       , JdkAttributes.ALLOCATION_SIZE
        );


        //        allocCollection.forEach(eventsCollection -> {
        //            var stackAccessor = eventsCollection.getType().getAccessor(JfrAttributes.EVENT_STACKTRACE.getKey());
        //            eventsCollection.stream().limit(10).forEach(item -> {
        //                var stack = stackAccessor.getMember(item);
        //
        //                if (stack == null || stack.getFrames() == null) {
        //                    return;
        //                }
        //
        //
        //            });
        //        });
    }

    static StacktraceTreeModel stackTraceCPUFun(IItemCollection events) {
        var methodFrameSeparator = new FrameSeparator(FrameCategorization.METHOD, false);

        var allocCollection = events.apply(ItemFilters.type(Set.of(
                "jdk.ExecutionSample"
        )));

        var invertedStacks = false;
        return new StacktraceTreeModel(allocCollection,
                                       methodFrameSeparator,
                                       invertedStacks
                //                                      , JdkAttributes.SAMPLE_WEIGHT
        );
    }

    private static void otherEvents(IItemCollection events) {
        events.apply(ItemFilters.type(Set.of(
                "jdk.CPUInformation",
                "jdk.OSInformation",
                "jdk.ActiveRecording",
                //                "jdk.ActiveSetting", // async profiler settings ?
                "jdk.JVMInformation"
        ))).forEach(eventsCollection -> {
            eventsCollection.stream().limit(10).forEach(event -> {
                System.out.println("\n" + event.getType().getIdentifier());
                IType<IItem> itemType = ItemToolkit.getItemType(event);
                itemType.getAccessorKeys().keySet().forEach((accessorKey) -> {
                    System.out.println(accessorKey.getIdentifier() + "=" + itemType.getAccessor(accessorKey).getMember(event));
                });
            });
        });
    }

    static String jvmSystemProperties(IItemCollection events) {
        var stringBuilder = new StringBuilder();

        events.apply(ItemFilters.type(Set.of(
                "jdk.InitialSystemProperty"
        ))).forEach(eventsCollection -> {
            var keyAccessor = eventsCollection.getType().getAccessor(JdkAttributes.ENVIRONMENT_KEY.getKey());
            var valueAccessor = eventsCollection.getType().getAccessor(JdkAttributes.ENVIRONMENT_VALUE.getKey());
            eventsCollection.stream().forEach(event -> stringBuilder.append(keyAccessor.getMember(event)).append(" = ").append(valueAccessor.getMember(event)).append("\n"));
        });

        return stringBuilder.toString();
    }

    static String nativeLibraries(IItemCollection events) {
        var stringBuilder = new StringBuilder();

        events.apply(ItemFilters.type(Set.of(
                "jdk.NativeLibrary"
        ))).forEach(eventsCollection -> {
            var nativeLibNameAccessor = eventsCollection.getType().getAccessor(JdkAttributes.NATIVE_LIBRARY_NAME.getKey());
            eventsCollection.stream().forEach(event -> stringBuilder.append(nativeLibNameAccessor.getMember(event)).append("\n"));
        });
        return stringBuilder.toString();
    }
}
