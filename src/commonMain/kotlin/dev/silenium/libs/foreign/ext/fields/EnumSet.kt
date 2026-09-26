package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.ext.NativeEnum
import dev.silenium.libs.foreign.ext.asNative
import dev.silenium.libs.foreign.ext.parseNativeEnumSet
import dev.silenium.libs.foreign.MemorySegment
import java.util.*
import kotlin.reflect.KProperty

inline fun <reified E> HasNative.enumSetField(
    noinline getter: (MemorySegment) -> Int,
    noinline setter: (MemorySegment, Int) -> Unit
): ReadWriteField<Set<E>> where E : Enum<E>, E : NativeEnum {
    return EnumSetField(getter, setter, ::parseNativeEnumSet)
}

inline fun <reified E> HasNative.enumSetField(
    noinline getter: (MemorySegment) -> Int,
): ReadOnlyField<Set<E>> where E : Enum<E>, E : NativeEnum {
    return ReadOnlyEnumSetField(getter, ::parseNativeEnumSet)
}

open class ReadOnlyEnumSetField<E>(
    val getter: (MemorySegment) -> Int,
    val parser: (Int) -> EnumSet<E>
) : ReadOnlyField<Set<E>> where E : Enum<E>, E : NativeEnum {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): Set<E> {
        thisRef as HasNative
        return parser(thisRef.value.let(getter))
    }
}

class EnumSetField<E>(
    getter: (MemorySegment) -> Int,
    val setter: (MemorySegment, Int) -> Unit,
    parser: (Int) -> EnumSet<E>
) : ReadOnlyEnumSetField<E>(getter, parser), ReadWriteField<Set<E>> where E : Enum<E>, E : NativeEnum {
    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Set<E>) {
        thisRef as HasNative
        setter(thisRef.value, value.asNative())
    }
}
