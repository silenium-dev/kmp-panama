package dev.silenium.libs.foreign.ext

import dev.silenium.libs.foreign.Arena
import dev.silenium.libs.foreign.MemorySegment
import dev.silenium.libs.foreign.ValueLayout
import java.nio.ByteBuffer

fun Arena.pointerTo(segment: MemorySegment?): MemorySegment {
    val pointer = allocate(ValueLayout.ADDRESS)
    pointer.set(ValueLayout.ADDRESS, 0, segment ?: MemorySegment.NULL)
    return pointer
}

fun MemorySegment.asPointerArray(size: Int): List<MemorySegment?> {
    return List(size) { n ->
        getAtIndex(ValueLayout.ADDRESS, n.toLong())
            .takeUnless { it == MemorySegment.NULL }
    }
}

fun <T> MemorySegment.asPointerArray(size: Int, wrapper: (MemorySegment) -> T): List<T?> {
    return List(size) { n ->
        getAtIndex(ValueLayout.ADDRESS, n.toLong())
            .takeUnless { it == MemorySegment.NULL }?.let(wrapper)
    }
}

fun MemorySegment.asIntArray(size: Int): List<Int> {
    return List(size) { getAtIndex(ValueLayout.JAVA_INT, it.toLong()) }
}

fun MemorySegment.asUIntArray(size: Int): List<UInt> {
    return List(size) { getAtIndex(ValueLayout.JAVA_INT, it.toLong()).toUInt() }
}

fun <T> Collection<T>.asNativeArray(arena: Arena, mapper: (T) -> MemorySegment?): MemorySegment {
    val allocated = arena.allocate(ValueLayout.ADDRESS, size.toLong())
    forEachIndexed { idx, it ->
        allocated.setAtIndex(ValueLayout.ADDRESS, idx.toLong(), mapper(it) ?: MemorySegment.NULL)
    }
    return allocated
}

@JvmName("asNativeArrayInt")
fun Collection<Int>.asNativeArray(arena: Arena): MemorySegment {
    return arena.allocateFrom(ValueLayout.JAVA_INT, *toIntArray())
}

@JvmName("asNativeArrayUInt")
fun Collection<UInt>.asNativeArray(arena: Arena): MemorySegment {
    return arena.allocateFrom(ValueLayout.JAVA_INT, *map(UInt::toInt).toIntArray())
}

@JvmName("asNativeArrayMemorySegment")
fun Collection<MemorySegment?>.asNativeArray(arena: Arena): MemorySegment {
    return asNativeArray(arena) { it }
}

fun <E> Set<E>.asNative() where E : Enum<E>, E : NativeEnum = fold(0) { acc, e -> acc or e.value }

fun MemorySegment.getRawField(
    field: (MemorySegment) -> MemorySegment,
    lengthField: (MemorySegment) -> Long,
): MemorySegment? = field(this)
    .takeIf { it != MemorySegment.NULL }
    ?.reinterpret(lengthField(this))

fun MemorySegment.getBufferField(
    field: (MemorySegment) -> MemorySegment,
    lengthField: (MemorySegment) -> Int,
): ByteBuffer? = field(this)
    .takeIf { it != MemorySegment.NULL }
    ?.reinterpret(lengthField(this).toLong())
    ?.asByteBuffer()

fun MemorySegment.setBufferField(
    arena: Arena,
    field: (MemorySegment, MemorySegment) -> Unit,
    lengthField: (MemorySegment, Int) -> Unit,
    value: ByteBuffer?,
) {
    if (value == null) {
        field(this, MemorySegment.NULL)
        lengthField(this, 0)
        return
    }
    val segment = arena.allocate(value.limit().toLong())
    segment.copyFrom(MemorySegment.ofBuffer(value))
    field(this, segment)
    lengthField(this, segment.byteSize().toInt())
}

fun MemorySegment.getStringField(
    field: (MemorySegment) -> MemorySegment,
): String? = field(this)
    .takeIf { it != MemorySegment.NULL }
    ?.reinterpret(Long.MAX_VALUE)
    ?.getString(0L)

fun MemorySegment.setStringField(
    arena: Arena,
    field: (MemorySegment, MemorySegment) -> Unit,
    value: String?,
) {
    if (value == null) {
        field(this, MemorySegment.NULL)
        return
    }
    val segment = arena.allocate(value.length.toLong())
    segment.setString(0L, value)
    field(this, segment)
}
