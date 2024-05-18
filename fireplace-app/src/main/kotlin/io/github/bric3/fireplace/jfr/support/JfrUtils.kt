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

import org.openjdk.jmc.common.item.IAttribute
import org.openjdk.jmc.common.item.IItem
import org.openjdk.jmc.common.item.IItemCollection
import org.openjdk.jmc.common.item.IItemIterable
import org.openjdk.jmc.common.item.IMemberAccessor
import org.openjdk.jmc.common.item.IType
import org.openjdk.jmc.common.item.ItemCollectionToolkit
import org.openjdk.jmc.common.item.ItemFilters
import org.openjdk.jmc.common.unit.IFormatter
import org.openjdk.jmc.common.unit.IQuantity
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel
import java.lang.invoke.MethodHandles
import java.util.*
import java.util.function.Supplier
import java.util.stream.Collectors
import java.util.stream.Collectors.joining
import java.util.stream.StreamSupport
import kotlin.streams.asSequence

fun <M, O> IMemberAccessor<M, O>.getMemberFromEvent(event: IItem): M {
    @Suppress("UNCHECKED_CAST") // getMember doesn't compile with the Kotlin typesystem
    return (this as IMemberAccessor<M, IItem>).getMember(event)
}

fun <T> IFormatter<T>.formatValue(value: Any?): String? {
    value ?: return null
    @Suppress("UNCHECKED_CAST") // format doesn't compile with the Kotlin typesystem
    return (this as IFormatter<Any>).format(value)
}



fun collectionDescription(items: IItemCollection): String? {
    val itemCountByType: Map<IType<*>, Long> =
        items.stream()
            .filter { obj: IItemIterable -> obj.hasItems() }
            .collect(
                Collectors.toMap(
                    IItemIterable::getType,
                    IItemIterable::getItemCount,
                    java.lang.Long::sum
                )
            )
    return if (itemCountByType.size < 4) {
        itemCountByType.entries.stream()
            .map { (eventType, eventCount) -> eventCount.toString() + " " + eventType.name }
            .sorted()
            .collect(joining(", "))
    } else {
        buildString {
            append(itemCountByType.values.stream()
                .mapToLong { obj: Long -> obj }
                .sum())
            append(" events over ")
            append(itemCountByType.size)
            append(" types")
        }
    }
}

/**
 * The Type category of the events is not accessible via the public API,
 * this code hacks the internals to get it.
 *
 * Underneath [IItemCollection] and [IItemIterable], there is the
 * [org.openjdk.jmc.flightrecorder.EventCollection] that event arrays
 * [org.openjdk.jmc.flightrecorder.internal.EventArray] and this class holds
 * the data for the event type and its category.
 */
object TypeCategoryExtractor {
    private val internalEventTypeEntry = Class.forName("org.openjdk.jmc.flightrecorder.EventCollection\$EventTypeEntry")
    private val internalEventArray = Class.forName("org.openjdk.jmc.flightrecorder.internal.EventArray")
    
    private val EventTypeEntry_events = MethodHandles.privateLookupIn(
        internalEventTypeEntry,
        MethodHandles.lookup()
    ).findVarHandle(internalEventTypeEntry, "events", internalEventArray)

    private val EventArray_type = MethodHandles.privateLookupIn(
        internalEventArray,
        MethodHandles.lookup()
    ).findVarHandle(internalEventArray, "type", IType::class.java)

    private val EventArray_typeCategory = MethodHandles.privateLookupIn(
        internalEventArray,
        MethodHandles.lookup()
    ).findVarHandle(internalEventArray, "typeCategory", Array<String>::class.java)

    fun extract(events: IItemCollection): Map<IType<*>, TypeCategory> {
        return events.stream()
            .filter { itemIterable -> internalEventTypeEntry.isInstance(itemIterable) }
            .asSequence()
            .map {
                val eventArray = EventTypeEntry_events.get(it)

                val type = EventArray_type.get(eventArray) as IType<*>

                @Suppress("UNCHECKED_CAST")
                val category = EventArray_typeCategory.get(eventArray) as Array<String>

                it.itemCount

                TypeCategory(type, listOf(*category), it.itemCount)
            }
            .associateBy { (type, _, _) -> type }
    }
}

data class TypeCategory(val type: IType<*>, val categories: List<String>, val count: Long)

fun IItemCollection.stacktraceTreeModel(nodeWeightAttribute: IAttribute<IQuantity>? = null): StacktraceTreeModel {
    val methodFrameSeparator = FrameSeparator(FrameSeparator.FrameCategorization.METHOD, false)
    val invertedStacks = false
    return StacktraceTreeModel(
        this,
        methodFrameSeparator,
        invertedStacks,
        nodeWeightAttribute
    )
}

fun Iterable<IItem>.stacktraceTreeModel(nodeWeightAttribute: IAttribute<IQuantity>? = null): StacktraceTreeModel {
    return this.toItemCollection().stacktraceTreeModel(nodeWeightAttribute)
}

fun Iterable<IItem>.toItemCollection(parallel: Boolean = false): IItemCollection {
    return ItemCollectionToolkit.build(StreamSupport.stream(this.spliterator(), parallel))
}

fun <T, K> byThreads(
    events: IItemCollection,
    classifier: IAttribute<T>,
    subAttribute: IAttribute<K>? = null
): Map<K, Supplier<List<IItem>>> {
    val mappingAttribute = subAttribute ?: classifier

    val hasEventThread = ItemFilters.hasAttribute(classifier)
    val eventsWithEventThread = events.apply(hasEventThread)

    return eventsWithEventThread.parallelStream()
        .flatMap { itemIterable ->
            val accessor = mappingAttribute.getAccessor(itemIterable.type)!!

            itemIterable.parallelStream()
                .map {
                    accessor.getMember(it) to it
                }
        }
        .collect(
            Collectors.toMap(
                {
                    @Suppress("UNCHECKED_CAST")
                    it.first as K
                },
                { Supplier { listOf(it.second) } },
                { a, b -> Supplier { a.get() + b.get() } }
            )
        )
}
