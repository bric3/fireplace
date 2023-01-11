package io.github.bric3.fireplace

import org.openjdk.jmc.common.item.IAccessorKey
import org.openjdk.jmc.common.item.IItem
import org.openjdk.jmc.common.item.IItemCollection
import org.openjdk.jmc.common.item.IItemIterable
import org.openjdk.jmc.common.item.ItemFilters
import org.openjdk.jmc.common.item.ItemToolkit
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel
import java.util.function.Consumer

object JfrAnalyzer {
    @JvmStatic
    fun stackTraceAllocationFun(events: IItemCollection): StacktraceTreeModel {
        val methodFrameSeparator = FrameSeparator(FrameSeparator.FrameCategorization.METHOD, false)
        val allocCollection = events.apply(
            ItemFilters.type(
                setOf(
                    "jdk.ObjectAllocationInNewTLAB",
                    "jdk.ObjectAllocationOutsideTLAB"
                )
            )
        )
        return StacktraceTreeModel(
            allocCollection,
            methodFrameSeparator,
            false,
            // JdkAttributes.ALLOCATION_SIZE
        )


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

    @JvmStatic
    fun stackTraceCPUFun(events: IItemCollection): StacktraceTreeModel {
        val methodFrameSeparator = FrameSeparator(FrameSeparator.FrameCategorization.METHOD, false)
        val allocCollection = events.apply(
            ItemFilters.type(
                setOf(
                    "jdk.ExecutionSample"
                )
            )
        )
        val invertedStacks = false
        return StacktraceTreeModel(
            allocCollection,
            methodFrameSeparator,
            invertedStacks,
            // JdkAttributes.SAMPLE_WEIGHT
        )
    }

    private fun otherEvents(events: IItemCollection) {
        events.apply(
            ItemFilters.type(
                setOf(
                    "jdk.CPUInformation",
                    "jdk.OSInformation",
                    "jdk.ActiveRecording",
                    // "jdk.ActiveSetting", // async profiler settings ?
                    "jdk.JVMInformation"
                )
            )
        ).forEach { eventsCollection: IItemIterable ->
            eventsCollection.stream().limit(10).forEach { event: IItem ->
                println(
                    """
                    ${event.type.identifier}
                    """.trimIndent()
                )
                val itemType = ItemToolkit.getItemType(event)
                itemType.accessorKeys.keys.forEach(Consumer { accessorKey: IAccessorKey<*> ->
                    println("${accessorKey.identifier}=${itemType.getAccessor(accessorKey).getMember(event)}")
                })
            }
        }
    }

    @JvmStatic
    fun jvmSystemProperties(events: IItemCollection): String {
        return buildString {
            events.apply(
                ItemFilters.type(
                    setOf(
                        "jdk.InitialSystemProperty"
                    )
                )
            ).forEach { eventsCollection: IItemIterable ->
                val keyAccessor = eventsCollection.type.getAccessor(JdkAttributes.ENVIRONMENT_KEY.key)
                val valueAccessor = eventsCollection.type.getAccessor(JdkAttributes.ENVIRONMENT_VALUE.key)
                eventsCollection.stream().forEach { event: IItem ->
                    append(keyAccessor.getMember(event))
                    append(" = ")
                    append(valueAccessor.getMember(event))
                    append("\n")
                }
            }
        }
    }

    @JvmStatic
    fun nativeLibraries(events: IItemCollection): String {
        return buildString {
            events.apply(
                ItemFilters.type(
                    setOf(
                        "jdk.NativeLibrary"
                    )
                )
            ).forEach { eventsCollection: IItemIterable ->
                val nativeLibNameAccessor = eventsCollection.type.getAccessor(
                    JdkAttributes.NATIVE_LIBRARY_NAME.key
                )
                eventsCollection.stream()
                    .forEach { event: IItem ->
                        append(nativeLibNameAccessor.getMember(event))
                        append("\n")
                    }
            }
        }
    }
}