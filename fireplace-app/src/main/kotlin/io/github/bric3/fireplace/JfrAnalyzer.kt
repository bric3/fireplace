/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace

import org.openjdk.jmc.common.item.IAccessorKey
import org.openjdk.jmc.common.item.IItem
import org.openjdk.jmc.common.item.IItemCollection
import org.openjdk.jmc.common.item.IItemIterable
import org.openjdk.jmc.common.item.ItemFilters
import org.openjdk.jmc.common.item.ItemToolkit
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters
import java.util.function.Consumer

object JfrAnalyzer {
    @JvmStatic
    fun allocInOutTlab(events: IItemCollection): IItemCollection {
        return events.apply(JdkFilters.ALLOC_ALL)
    }

    @JvmStatic
    fun executionSamples(events: IItemCollection): IItemCollection {
        return events.apply(JdkFilters.EXECUTION_SAMPLE)
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
    fun jvmSystemProperties(events: IItemCollection): Map<String, String> {
        return buildMap {
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
                    put(
                        keyAccessor.getMember(event),
                        valueAccessor.getMember(event)
                    )
                }
            }
        }
    }

    @JvmStatic
    fun nativeLibraries(events: IItemCollection): List<String> {
        return buildList {
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
                        this.add(nativeLibNameAccessor.getMember(event))
                    }
            }
        }
    }
}