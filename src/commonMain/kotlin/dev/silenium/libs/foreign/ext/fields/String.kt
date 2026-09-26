package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.MemorySegment
import kotlin.reflect.KProperty

fun HasNative.stringField(getter: (MemorySegment) -> MemorySegment) = ReadOnlyStringField(getter)
fun HasNative.stringField(getter: (MemorySegment) -> MemorySegment, setter: (MemorySegment, MemorySegment) -> Unit) =
    StringField(getter, setter)

open class ReadOnlyStringField(val getter: (MemorySegment) -> MemorySegment) : ReadOnlyField<String?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): String? {
        thisRef as HasNative
        return getter(thisRef.value)
            .takeIf { it != MemorySegment.NULL }
            ?.reinterpret(Long.MAX_VALUE)
            ?.getString(0L)
    }
}

class StringField(
    getter: (MemorySegment) -> MemorySegment,
    val setter: (MemorySegment, MemorySegment) -> Unit,
) : ReadOnlyStringField(getter), ReadWriteField<String?> {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        thisRef as HasNative
        setter(thisRef.value, value?.let(thisRef.arena::allocateFrom) ?: MemorySegment.NULL)
    }
}

fun HasNative.stringSplitSetField(
    getter: (MemorySegment) -> MemorySegment,
    setter: (MemorySegment, MemorySegment) -> Unit,
    separator: String,
) = stringField(getter, setter).map(
    inMapper = { it?.split(separator)?.toSet() },
    outMapper = { it?.joinToString(separator = separator) },
)
