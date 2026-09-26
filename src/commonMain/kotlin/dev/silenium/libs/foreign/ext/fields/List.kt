package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.ext.asNativeArray
import dev.silenium.libs.foreign.ext.asPointerArray
import dev.silenium.libs.foreign.MemorySegment
import kotlin.reflect.KProperty

fun <E : HasNative> HasNative.pointerListField(
    getter: (MemorySegment) -> MemorySegment,
    sizeGetter: (MemorySegment) -> Int,
    wrapper: (MemorySegment) -> E,
): ReadOnlyField<List<E?>> = ReadOnlyListField(getter, sizeGetter, wrapper)

fun <E : HasNative> HasNative.pointerListField(
    getter: (MemorySegment) -> MemorySegment,
    sizeGetter: (MemorySegment) -> Int,
    setter: (MemorySegment, MemorySegment) -> Unit,
    sizeSetter: (MemorySegment, Int) -> Unit,
    wrapper: (MemorySegment) -> E,
): ReadWriteField<List<E?>> = ListField(getter, sizeGetter, setter, sizeSetter, wrapper)

fun <E : HasNative> HasNative.fixedPointerListField(
    getter: (MemorySegment) -> MemorySegment,
    size: Int,
    wrapper: (MemorySegment) -> E,
): ReadOnlyField<List<E?>> = ReadOnlyListField(getter, { size }, wrapper)

fun <E : HasNative> HasNative.fixedPointerListField(
    getter: (MemorySegment) -> MemorySegment,
    setter: (MemorySegment, MemorySegment) -> Unit,
    size: Int,
    wrapper: (MemorySegment) -> E,
): ReadWriteField<List<E?>> = ListField(getter, { size }, setter, { _, newSize -> require(newSize == size) }, wrapper)

open class ReadOnlyListField<E : HasNative>(
    val getter: (MemorySegment) -> MemorySegment,
    val sizeGetter: (MemorySegment) -> Int,
    val wrapper: (MemorySegment) -> E,
) : ReadOnlyField<List<E?>> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): List<E?> {
        thisRef as HasNative
        return thisRef.value.let(getter).asPointerArray(thisRef.value.let(sizeGetter), wrapper)
    }
}

class ListField<E : HasNative>(
    getter: (MemorySegment) -> MemorySegment,
    sizeGetter: (MemorySegment) -> Int,
    val setter: (MemorySegment, MemorySegment) -> Unit,
    val sizeSetter: (MemorySegment, Int) -> Unit,
    wrapper: (MemorySegment) -> E,
) : ReadOnlyListField<E>(getter, sizeGetter, wrapper), ReadWriteField<List<E?>> {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<E?>) {
        thisRef as HasNative
        sizeSetter(thisRef.value, value.size)
        setter(thisRef.value, value.asNativeArray(thisRef.arena) { it?.value })
    }
}
