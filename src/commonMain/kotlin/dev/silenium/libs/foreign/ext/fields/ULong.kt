package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.MemorySegment
import kotlin.reflect.KProperty

fun HasNative.ulongField(getter: (MemorySegment) -> Long) = ReadOnlyULongField(getter)
fun HasNative.ulongField(getter: (MemorySegment) -> Long, setter: (MemorySegment, Long) -> Unit) =
    ULongField(getter, setter)

open class ReadOnlyULongField(
    val getter: (MemorySegment) -> Long,
) : ReadOnlyField<ULong> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): ULong {
        thisRef as HasNative
        return thisRef.value.let(getter).toULong()
    }
}

class ULongField(
    getter: (MemorySegment) -> Long,
    val setter: (MemorySegment, Long) -> Unit,
) : ReadOnlyULongField(getter), ReadWriteField<ULong> {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: ULong) {
        thisRef as HasNative
        setter(thisRef.value, value.toLong())
    }
}
