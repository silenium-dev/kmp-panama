package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.MemorySegment
import kotlin.reflect.KProperty

fun HasNative.uintField(getter: (MemorySegment) -> Int) = ReadOnlyUIntField(getter)
fun HasNative.uintField(getter: (MemorySegment) -> Int, setter: (MemorySegment, Int) -> Unit) = UIntField(getter, setter)

open class ReadOnlyUIntField(
    val getter: (MemorySegment) -> Int,
) : ReadOnlyField<UInt> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): UInt {
        thisRef as HasNative
        return thisRef.value.let(getter).toUInt()
    }
}

class UIntField(
    getter: (MemorySegment) -> Int,
    val setter: (MemorySegment, Int) -> Unit,
) : ReadOnlyUIntField(getter), ReadWriteField<UInt> {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: UInt) {
        thisRef as HasNative
        setter(thisRef.value, value.toInt())
    }
}
