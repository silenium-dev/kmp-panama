package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.MemorySegment
import kotlin.reflect.KProperty

inline fun <reified E : HasNative> HasNative.inlineStructListField(
    noinline getter: (MemorySegment, Long) -> MemorySegment,
    noinline setter: (MemorySegment, Long, MemorySegment) -> Unit,
    noinline parser: (MemorySegment) -> E,
    noinline mapper: (E) -> MemorySegment,
    length: Int,
): ReadWriteField<List<E>> {
    return InlineListField(getter, setter, parser, mapper, length)
}

inline fun <reified E : HasNative> HasNative.inlineStructListField(
    noinline getter: (MemorySegment, Long) -> MemorySegment,
    noinline parser: (MemorySegment) -> E,
    length: Int,
): ReadOnlyField<List<E>> {
    return ReadOnlyInlineListField(getter, parser, length)
}

open class ReadOnlyInlineListField<E : HasNative>(
    val getter: (MemorySegment, Long) -> MemorySegment,
    val elementParser: (MemorySegment) -> E,
    val length: Int,
) : ReadOnlyField<List<E>> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): List<E> {
        thisRef as HasNative
        return List(length) {
            getter(thisRef.value, it.toLong()).let(elementParser)
        }
    }
}

class InlineListField<E : HasNative>(
    getter: (MemorySegment, Long) -> MemorySegment,
    val setter: (MemorySegment, Long, MemorySegment) -> Unit,
    elementParser: (MemorySegment) -> E,
    val elementMapper: (E) -> MemorySegment,
    length: Int,
) : ReadOnlyInlineListField<E>(getter, elementParser, length), ReadWriteField<List<E>> {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<E>) {
        thisRef as HasNative
        value.forEachIndexed { idx, it ->
            setter(thisRef.value, idx.toLong(), elementMapper(it))
        }
    }
}
