package io.github.bric3.fireplace

import org.openjdk.jmc.common.item.IItem
import org.openjdk.jmc.common.item.IItemCollection
import org.openjdk.jmc.common.item.IItemIterable
import org.openjdk.jmc.common.item.IMemberAccessor
import org.openjdk.jmc.common.item.IType
import org.openjdk.jmc.common.unit.IFormatter
import java.lang.invoke.MethodHandles
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Collectors.joining
import kotlin.streams.asSequence

fun <M, O> IMemberAccessor<M, O>.getMemberFromEvent(event: IItem): Any? {
    @Suppress("UNCHECKED_CAST") // getMember doesn't compile with the Kotlin typesystem
    return (this as IMemberAccessor<Any, IItem>).getMember(event)
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