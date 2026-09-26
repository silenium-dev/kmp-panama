package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.MemorySegment
import kotlin.reflect.KProperty


inline fun <reified T : HasNative> HasNative.structField(
    noinline getter: (MemorySegment) -> MemorySegment,
    noinline setter: (MemorySegment, MemorySegment) -> Unit,
    noinline parser: (MemorySegment) -> T,
): ReadWriteField<T?> {
    return StructField(getter, setter, parser)
}

inline fun <reified T : HasNative> HasNative.structField(
    noinline getter: (MemorySegment) -> MemorySegment,
    noinline parser: (MemorySegment) -> T,
): ReadOnlyField<T?> {
    return ReadOnlyStructField(getter, parser)
}

open class ReadOnlyStructField<T : HasNative>(
    val getter: (MemorySegment) -> MemorySegment,
    val parser: (MemorySegment) -> T,
) : ReadOnlyField<T?> {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        thisRef as HasNative
        return thisRef.value
            .let(getter)
            .takeIf { it != MemorySegment.NULL }
            ?.let(parser)
    }
}

class StructField<T : HasNative>(
    getter: (MemorySegment) -> MemorySegment,
    val setter: (MemorySegment, MemorySegment) -> Unit,
    parser: (MemorySegment) -> T,
) : ReadOnlyStructField<T>(getter, parser), ReadWriteField<T?> {
    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        thisRef as HasNative
        setter(thisRef.value, value?.value ?: MemorySegment.NULL)
    }
}
