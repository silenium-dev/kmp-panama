package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.MemorySegment
import kotlin.reflect.KProperty

fun HasNative.booleanField(getter: (MemorySegment) -> Boolean) = ReadOnlyBooleanField(getter)
fun HasNative.booleanField(getter: (MemorySegment) -> Boolean, setter: (MemorySegment, Boolean) -> Unit) = BooleanField(getter, setter)

open class ReadOnlyBooleanField(
    val getter: (MemorySegment) -> Boolean,
) : ReadOnlyField<Boolean> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        thisRef as HasNative
        return thisRef.value.let(getter)
    }
}

class BooleanField(
    getter: (MemorySegment) -> Boolean,
    val setter: (MemorySegment, Boolean) -> Unit,
) : ReadOnlyBooleanField(getter), ReadWriteField<Boolean> {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        thisRef as HasNative
        setter(thisRef.value, value)
    }
}
