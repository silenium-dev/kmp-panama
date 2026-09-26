package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.ext.getBufferField
import dev.silenium.libs.foreign.ext.setBufferField
import dev.silenium.libs.foreign.MemorySegment
import java.nio.ByteBuffer
import kotlin.reflect.KProperty

fun HasNative.byteBufferField(getter: (MemorySegment) -> MemorySegment, lengthGetter: (MemorySegment) -> Int) =
    ReadOnlyByteBufferField(getter, lengthGetter)
fun HasNative.byteBufferField(
    getter: (MemorySegment) -> MemorySegment,
    lengthGetter: (MemorySegment) -> Int,
    setter: (MemorySegment, MemorySegment) -> Unit,
    lengthSetter: (MemorySegment, Int) -> Unit,
) = ByteBufferField(getter, lengthGetter, setter, lengthSetter)

open class ReadOnlyByteBufferField(
    val getter: (MemorySegment) -> MemorySegment,
    val lengthGetter: (MemorySegment) -> Int,
) : ReadOnlyField<ByteBuffer?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): ByteBuffer? {
        thisRef as HasNative
        return thisRef.value.getBufferField(getter, lengthGetter)
    }
}

class ByteBufferField(
    getter: (MemorySegment) -> MemorySegment,
    lengthGetter: (MemorySegment) -> Int,
    val setter: (MemorySegment, MemorySegment) -> Unit,
    val lengthSetter: (MemorySegment, Int) -> Unit,
) : ReadOnlyByteBufferField(getter, lengthGetter), ReadWriteField<ByteBuffer?> {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: ByteBuffer?) {
        thisRef as HasNative
        thisRef.value.setBufferField(thisRef.arena, setter, lengthSetter, value)
    }
}
