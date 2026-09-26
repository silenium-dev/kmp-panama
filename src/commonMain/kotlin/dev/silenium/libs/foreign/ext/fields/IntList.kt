package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.ext.asIntArray
import dev.silenium.libs.foreign.ext.asNativeArray
import dev.silenium.libs.foreign.MemorySegment
import kotlin.reflect.KProperty

fun HasNative.intListField(
    getter: (MemorySegment) -> MemorySegment,
    sizeGetter: (MemorySegment) -> Int,
): ReadOnlyField<List<Int>> = ReadOnlyIntListField(getter, sizeGetter)

fun HasNative.intListField(
    getter: (MemorySegment) -> MemorySegment,
    sizeGetter: (MemorySegment) -> Int,
    setter: (MemorySegment, MemorySegment) -> Unit,
    sizeSetter: (MemorySegment, Int) -> Unit,
): ReadWriteField<List<Int>> = IntListField(getter, sizeGetter, setter, sizeSetter)

fun HasNative.fixedIntListField(
    getter: (MemorySegment) -> MemorySegment,
    setter: (MemorySegment, MemorySegment) -> Unit,
    size: Int,
): ReadWriteField<List<Int>> = IntListField(getter, { size }, setter) { _, newSize -> require(newSize == size) }

fun HasNative.fixedIntListField(
    getter: (MemorySegment) -> MemorySegment,
    size: Int,
): ReadOnlyField<List<Int>> = ReadOnlyIntListField(getter) { size }

open class ReadOnlyIntListField(
    val getter: (MemorySegment) -> MemorySegment,
    val sizeGetter: (MemorySegment) -> Int,
) : ReadOnlyField<List<Int>> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): List<Int> {
        thisRef as HasNative
        return thisRef.value.let(getter).asIntArray(thisRef.value.let(sizeGetter))
    }
}

class IntListField(
    getter: (MemorySegment) -> MemorySegment,
    sizeGetter: (MemorySegment) -> Int,
    val setter: (MemorySegment, MemorySegment) -> Unit,
    val sizeSetter: (MemorySegment, Int) -> Unit,
) : ReadOnlyIntListField(getter, sizeGetter), ReadWriteField<List<Int>> {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<Int>) {
        thisRef as HasNative
        sizeSetter(thisRef.value, value.size)
        setter(thisRef.value, value.asNativeArray(thisRef.arena))
    }
}
