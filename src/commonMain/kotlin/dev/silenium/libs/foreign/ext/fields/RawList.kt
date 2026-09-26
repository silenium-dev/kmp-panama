package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.ext.asNativeArray
import dev.silenium.libs.foreign.ext.asPointerArray
import dev.silenium.libs.foreign.MemorySegment
import kotlin.reflect.KProperty

fun HasNative.rawListField(
    getter: (MemorySegment) -> MemorySegment,
    sizeGetter: (MemorySegment) -> Int,
) = ReadOnlyRawListField(getter, sizeGetter)

fun HasNative.rawListField(
    getter: (MemorySegment) -> MemorySegment,
    sizeGetter: (MemorySegment) -> Int,
    setter: (MemorySegment, MemorySegment) -> Unit,
    sizeSetter: (MemorySegment, Int) -> Unit,
) = RawListField(getter, sizeGetter, setter, sizeSetter)

fun HasNative.fixedRawListField(
    getter: (MemorySegment) -> MemorySegment,
    size: Int,
) = ReadOnlyRawListField(getter) { size }

fun HasNative.fixedRawListField(
    getter: (MemorySegment) -> MemorySegment,
    setter: (MemorySegment, MemorySegment) -> Unit,
    size: Int,
) = RawListField(getter, sizeGetter = { size }, setter, sizeSetter = { _, newSize -> require(newSize == size) })

open class ReadOnlyRawListField(
    val getter: (MemorySegment) -> MemorySegment,
    val sizeGetter: (MemorySegment) -> Int = { 0 },
) : ReadOnlyField<List<MemorySegment?>> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): List<MemorySegment?> {
        thisRef as HasNative
        return thisRef.value.let(getter).asPointerArray(thisRef.value.let(sizeGetter))
    }
}

class RawListField(
    getter: (MemorySegment) -> MemorySegment,
    sizeGetter: (MemorySegment) -> Int,
    val setter: (MemorySegment, MemorySegment) -> Unit,
    val sizeSetter: (MemorySegment, Int) -> Unit,
) : ReadOnlyRawListField(getter, sizeGetter), ReadWriteField<List<MemorySegment?>> {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<MemorySegment?>) {
        thisRef as HasNative
        sizeSetter(thisRef.value, value.size)
        setter(thisRef.value, value.asNativeArray(thisRef.arena))
    }
}
