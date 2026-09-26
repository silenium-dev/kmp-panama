package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.ext.NativeEnum
import dev.silenium.libs.foreign.ext.parseNativeEnum
import dev.silenium.libs.foreign.MemorySegment
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

inline fun <reified E> HasNative.enumField(
    noinline getter: (MemorySegment) -> Int,
    noinline setter: (MemorySegment, Int) -> Unit
): ReadWriteField<E> where E : Enum<E>, E : NativeEnum {
    return EnumField(getter, setter, ::parseNativeEnum)
}

inline fun <reified E> HasNative.enumField(
    noinline getter: (MemorySegment) -> Int,
): ReadOnlyField<E> where E : Enum<E>, E : NativeEnum {
    return ReadOnlyEnumField(getter, ::parseNativeEnum)
}

fun <E> HasNative.enumField(
    getter: (MemorySegment) -> Int,
    setter: (MemorySegment, Int) -> Unit,
    klass: KClass<E>,
): ReadWriteField<E> where E : Enum<E>, E : NativeEnum {
    return EnumField(getter, setter, klass::parseNativeEnum)
}

fun <E> HasNative.enumField(
    getter: (MemorySegment) -> Int,
    klass: KClass<E>,
): ReadOnlyField<E> where E : Enum<E>, E : NativeEnum {
    return ReadOnlyEnumField(getter, klass::parseNativeEnum)
}


open class ReadOnlyEnumField<E>(
    val getter: (MemorySegment) -> Int,
    val parser: (Int) -> E
) : ReadOnlyField<E> where E : Enum<E>, E : NativeEnum {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): E {
        thisRef as HasNative
        return parser(thisRef.value.let(getter))
    }
}

class EnumField<E>(
    getter: (MemorySegment) -> Int,
    val setter: (MemorySegment, Int) -> Unit,
    parser: (Int) -> E,
) : ReadOnlyEnumField<E>(getter, parser), ReadWriteField<E> where E : Enum<E>, E : NativeEnum {
    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: E) {
        thisRef as HasNative
        setter(thisRef.value, value.value)
    }
}
