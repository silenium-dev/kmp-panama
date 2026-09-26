package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.MemorySegment
import kotlin.reflect.KProperty

fun HasNative.sizedRawField(getter: (MemorySegment) -> MemorySegment, sizeGetter: (MemorySegment) -> Long) =
    ReadOnlySizedRawField(getter, sizeGetter)

fun HasNative.sizedRawField(
    getter: (MemorySegment) -> MemorySegment, sizeGetter: (MemorySegment) -> Long,
    setter: (MemorySegment, MemorySegment) -> Unit, sizeSetter: (MemorySegment, Long) -> Unit
) = RawSizedField(getter, sizeGetter, setter, sizeSetter)

open class ReadOnlySizedRawField(
    val getter: (MemorySegment) -> MemorySegment,
    val sizeGetter: (MemorySegment) -> Long,
) : ReadOnlyField<MemorySegment?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): MemorySegment? {
        thisRef as HasNative
        val size = thisRef.value.let(sizeGetter)
        return thisRef.value.let(getter).takeIf { it != MemorySegment.NULL }?.reinterpret(size)
    }
}

class RawSizedField(
    getter: (MemorySegment) -> MemorySegment,
    sizeGetter: (MemorySegment) -> Long,
    val setter: (MemorySegment, MemorySegment) -> Unit,
    val sizeSetter: (MemorySegment, Long) -> Unit,
) : ReadOnlySizedRawField(getter, sizeGetter), ReadWriteField<MemorySegment?> {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: MemorySegment?) {
        thisRef as HasNative
        setter(thisRef.value, value ?: MemorySegment.NULL)
        sizeSetter(thisRef.value, value?.byteSize() ?: 0L)
    }
}
