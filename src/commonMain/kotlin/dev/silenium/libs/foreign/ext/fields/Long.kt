package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.MemorySegment
import kotlin.reflect.KProperty

fun HasNative.longField(getter: (MemorySegment) -> Long) = ReadOnlyLongField(getter)
fun HasNative.longField(getter: (MemorySegment) -> Long, setter: (MemorySegment, Long) -> Unit) = LongField(getter, setter)

open class ReadOnlyLongField(
    val getter: (MemorySegment) -> Long,
) : ReadOnlyField<Long> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Long {
        thisRef as HasNative
        return thisRef.value.let(getter)
    }
}

class LongField(
    getter: (MemorySegment) -> Long,
    val setter: (MemorySegment, Long) -> Unit,
) : ReadOnlyLongField(getter), ReadWriteField<Long> {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
        thisRef as HasNative
        setter(thisRef.value, value)
    }
}
