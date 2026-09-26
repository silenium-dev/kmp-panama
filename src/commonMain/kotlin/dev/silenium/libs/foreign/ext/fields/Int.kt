package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.MemorySegment
import kotlin.reflect.KProperty

fun HasNative.intField(getter: (MemorySegment) -> Int) = ReadOnlyIntField(getter)
fun HasNative.intField(getter: (MemorySegment) -> Int, setter: (MemorySegment, Int) -> Unit) = IntField(getter, setter)

open class ReadOnlyIntField(
    val getter: (MemorySegment) -> Int,
) : ReadOnlyField<Int> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        thisRef as HasNative
        return thisRef.value.let(getter)
    }
}

class IntField(
    getter: (MemorySegment) -> Int,
    val setter: (MemorySegment, Int) -> Unit,
) : ReadOnlyIntField(getter), ReadWriteField<Int> {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        thisRef as HasNative
        setter(thisRef.value, value)
    }
}
