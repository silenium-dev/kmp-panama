package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.ext.asNativeArray
import dev.silenium.libs.foreign.ext.asUIntArray
import dev.silenium.libs.foreign.MemorySegment
import kotlin.reflect.KProperty

fun HasNative.uintListField(
    getter: (MemorySegment) -> MemorySegment,
    sizeGetter: (MemorySegment) -> Int,
): ReadOnlyField<List<UInt>> = ReadOnlyUIntListField(getter, sizeGetter)

fun HasNative.uintListField(
    getter: (MemorySegment) -> MemorySegment,
    sizeGetter: (MemorySegment) -> Int,
    setter: (MemorySegment, MemorySegment) -> Unit,
    sizeSetter: (MemorySegment, Int) -> Unit,
): ReadWriteField<List<UInt>> = UIntListField(getter, sizeGetter, setter, sizeSetter)

fun HasNative.fixedUIntListField(
    getter: (MemorySegment) -> MemorySegment,
    setter: (MemorySegment, MemorySegment) -> Unit,
    size: Int,
): ReadWriteField<List<UInt>> = UIntListField(getter, { size }, setter) { _, newSize -> require(newSize == size) }

fun HasNative.fixedUIntListField(
    getter: (MemorySegment) -> MemorySegment,
    size: Int,
): ReadOnlyField<List<UInt>> = ReadOnlyUIntListField(getter) { size }

open class ReadOnlyUIntListField(
    val getter: (MemorySegment) -> MemorySegment,
    val sizeGetter: (MemorySegment) -> Int,
) : ReadOnlyField<List<UInt>> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): List<UInt> {
        thisRef as HasNative
        return thisRef.value.let(getter).asUIntArray(thisRef.value.let(sizeGetter))
    }
}

class UIntListField(
    getter: (MemorySegment) -> MemorySegment,
    sizeGetter: (MemorySegment) -> Int,
    val setter: (MemorySegment, MemorySegment) -> Unit,
    val sizeSetter: (MemorySegment, Int) -> Unit,
) : ReadOnlyUIntListField(getter, sizeGetter), ReadWriteField<List<UInt>> {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<UInt>) {
        thisRef as HasNative
        sizeSetter(thisRef.value, value.size)
        setter(thisRef.value, value.asNativeArray(thisRef.arena))
    }
}
