package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.MemorySegment
import kotlin.reflect.KProperty

fun <S> subset(read: (MemorySegment) -> S) = ReadOnlySubset(read)
fun <S> subset(read: (MemorySegment) -> S, write: S.(MemorySegment) -> Unit) = Subset(read, write)

open class ReadOnlySubset<S>(val read: (MemorySegment) -> S) : ReadOnlyField<S> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): S {
        thisRef as HasNative
        return thisRef.value.let(read)
    }
}

class Subset<S>(
    read: (MemorySegment) -> S,
    val write: S.(MemorySegment) -> Unit,
) : ReadWriteField<S>, ReadOnlySubset<S>(read) {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: S) {
        thisRef as HasNative
        write(value, thisRef.value)
    }
}
