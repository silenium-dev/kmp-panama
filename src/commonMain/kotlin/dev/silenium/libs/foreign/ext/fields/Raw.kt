package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.MemorySegment
import kotlin.reflect.KProperty

fun HasNative.rawField(getter: (MemorySegment) -> MemorySegment) = ReadOnlyRawField(getter)
fun HasNative.rawField(getter: (MemorySegment) -> MemorySegment, setter: (MemorySegment, MemorySegment) -> Unit) =
    RawField(getter, setter)

open class ReadOnlyRawField(
    val getter: (MemorySegment) -> MemorySegment,
) : ReadOnlyField<MemorySegment?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): MemorySegment? {
        thisRef as HasNative
        return thisRef.value.let(getter).takeIf { it != MemorySegment.NULL }
    }
}

class RawField(
    getter: (MemorySegment) -> MemorySegment,
    val setter: (MemorySegment, MemorySegment) -> Unit,
) : ReadOnlyRawField(getter), ReadWriteField<MemorySegment?> {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: MemorySegment?) {
        thisRef as HasNative
        setter(thisRef.value, value ?: MemorySegment.NULL)
    }
}
