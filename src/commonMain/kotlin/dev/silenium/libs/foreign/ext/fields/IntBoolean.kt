package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.MemorySegment
import kotlin.reflect.KProperty

fun HasNative.intBooleanField(getter: (MemorySegment) -> Int) = ReadOnlyIntBooleanField(getter)
fun HasNative.intBooleanField(getter: (MemorySegment) -> Int, setter: (MemorySegment, Int) -> Unit) =
    IntBooleanField(getter, setter)

open class ReadOnlyIntBooleanField(
    val getter: (MemorySegment) -> Int,
) : ReadOnlyField<Boolean> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        thisRef as HasNative
        return thisRef.value.let(getter) > 0
    }
}

class IntBooleanField(
    getter: (MemorySegment) -> Int,
    val setter: (MemorySegment, Int) -> Unit,
) : ReadOnlyIntBooleanField(getter), ReadWriteField<Boolean> {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        thisRef as HasNative
        setter(thisRef.value, if (value) 1 else 0)
    }
}
