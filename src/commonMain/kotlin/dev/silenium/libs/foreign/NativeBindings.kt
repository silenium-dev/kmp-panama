package dev.silenium.libs.foreign

import java.lang.invoke.MethodType
import java.lang.reflect.Method
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.function.Consumer
import java.lang.invoke.MethodHandle as JMethodHandle

expect class SymbolLookup internal constructor(value: Any) {
    internal val value: Any

    fun find(name: String): MemorySegment?
    fun findOrThrow(name: String): MemorySegment
    fun or(other: SymbolLookup): SymbolLookup

    companion object {
        @JvmStatic
        fun loaderLookup(): SymbolLookup

        @JvmStatic
        fun libraryLookup(name: String, arena: Arena): SymbolLookup

        @JvmStatic
        fun libraryLookup(path: Path, arena: Arena): SymbolLookup
    }
}

expect class Linker internal constructor(value: Any) {
    internal val value: Any

    fun downcallHandle(
        descriptor: FunctionDescriptor,
        vararg option: Option,
    ): MethodHandle

    fun downcallHandle(
        symbol: MemorySegment,
        descriptor: FunctionDescriptor
    ): MethodHandle

    fun downcallHandle(
        symbol: MemorySegment,
        descriptor: FunctionDescriptor,
        vararg option: Option,
    ): MethodHandle

    fun upcallStub(
        target: MethodHandle,
        descriptor: FunctionDescriptor,
        arena: Arena
    ): MemorySegment

    fun defaultLookup(): SymbolLookup
    fun canonicalLayouts(): Map<String, MemoryLayout>

    class Option internal constructor(value: Any) {
        internal val value: Any

        companion object {
            @JvmStatic
            fun firstVariadicArg(var0: Int): Option

            @JvmStatic
            fun captureCallState(vararg var0: String): Option

            @JvmStatic
            fun captureStateLayout(): StructLayout

            @JvmStatic
            fun critical(var0: Boolean): Option
        }
    }

    companion object {
        @JvmStatic
        fun nativeLinker(): Linker
    }
}

fun Linker.upcallStub(
    target: MethodHandle,
    descriptor: FunctionDescriptor,
    arena: Arena
): MemorySegment = upcallStub(target, descriptor, arena)

expect object MethodHandles {
    object Lookup {
        @Throws(ReflectiveOperationException::class)
        fun findVirtual(klass: Class<*>, name: String, type: MethodType): MethodHandle
        @Throws(ReflectiveOperationException::class)
        fun unreflect(method: Method): MethodHandle
    }

    @JvmStatic
    fun lookup(): Lookup
}

expect class MethodHandle internal constructor(value: JMethodHandle) {
    internal val value: JMethodHandle

    operator fun invoke(vararg args: Any?): Any?
    fun invokeExact(vararg args: Any?): Any?

    fun bindTo(target: Any?): MethodHandle
    fun asSpreader(klass: Class<*>, count: Int): MethodHandle
}

expect class VarHandle internal constructor(value: Any) {
    internal val value: Any

    fun set(vararg args: Any?)
    fun get(vararg args: Any?): Any?
}

expect class FunctionDescriptor internal constructor(value: Any) {
    internal val value: Any

    fun appendArgumentLayouts(vararg layouts: MemoryLayout): FunctionDescriptor
    fun argumentLayouts(): List<MemoryLayout>
    fun toMethodType(): MethodType

    companion object {
        @JvmStatic
        fun of(returnType: MemoryLayout, vararg parameters: MemoryLayout): FunctionDescriptor

        @JvmStatic
        fun ofVoid(vararg parameters: MemoryLayout): FunctionDescriptor
    }
}

interface SegmentAllocator {
    fun allocate(size: Long, alignment: Long = 1): MemorySegment
    fun allocate(layout: MemoryLayout): MemorySegment
    fun allocate(layout: MemoryLayout, count: Long): MemorySegment

    fun allocateFrom(str: String): MemorySegment
    fun allocateFrom(str: String, charset: Charset): MemorySegment
    fun allocateFrom(layout: ValueLayout.OfByte, value: Byte): MemorySegment
    fun allocateFrom(layout: ValueLayout.OfChar, value: Char): MemorySegment
    fun allocateFrom(layout: ValueLayout.OfShort, value: Short): MemorySegment
    fun allocateFrom(layout: ValueLayout.OfInt, value: Int): MemorySegment
    fun allocateFrom(layout: ValueLayout.OfLong, value: Long): MemorySegment
    fun allocateFrom(layout: ValueLayout.OfFloat, value: Float): MemorySegment
    fun allocateFrom(layout: ValueLayout.OfDouble, value: Double): MemorySegment
    fun allocateFrom(layout: AddressLayout, value: MemorySegment): MemorySegment

    fun allocateFrom(elementLayout: ValueLayout.OfByte, vararg values: Byte): MemorySegment
    fun allocateFrom(elementLayout: ValueLayout.OfChar, vararg values: Char): MemorySegment
    fun allocateFrom(elementLayout: ValueLayout.OfShort, vararg values: Short): MemorySegment
    fun allocateFrom(elementLayout: ValueLayout.OfInt, vararg values: Int): MemorySegment
    fun allocateFrom(elementLayout: ValueLayout.OfLong, vararg values: Long): MemorySegment
    fun allocateFrom(elementLayout: ValueLayout.OfFloat, vararg values: Float): MemorySegment
    fun allocateFrom(elementLayout: ValueLayout.OfDouble, vararg values: Double): MemorySegment
}

expect class Arena internal constructor(value: Any) : SegmentAllocator,
    AutoCloseable {
    internal val value: Any

    companion object {
        @JvmStatic
        fun ofAuto(): Arena

        @JvmStatic
        fun ofShared(): Arena

        @JvmStatic
        fun ofConfined(): Arena

        @JvmStatic
        fun global(): Arena
    }

    val scope: MemorySegment.Scope

    override fun close()

    override fun allocate(size: Long, alignment: Long): MemorySegment
    override fun allocate(layout: MemoryLayout): MemorySegment
    override fun allocate(layout: MemoryLayout, count: Long): MemorySegment

    override fun allocateFrom(str: String): MemorySegment
    override fun allocateFrom(str: String, charset: Charset): MemorySegment
    override fun allocateFrom(layout: ValueLayout.OfByte, value: Byte): MemorySegment
    override fun allocateFrom(layout: ValueLayout.OfChar, value: Char): MemorySegment
    override fun allocateFrom(layout: ValueLayout.OfShort, value: Short): MemorySegment
    override fun allocateFrom(layout: ValueLayout.OfInt, value: Int): MemorySegment
    override fun allocateFrom(layout: ValueLayout.OfLong, value: Long): MemorySegment
    override fun allocateFrom(layout: ValueLayout.OfFloat, value: Float): MemorySegment
    override fun allocateFrom(layout: ValueLayout.OfDouble, value: Double): MemorySegment
    override fun allocateFrom(layout: AddressLayout, value: MemorySegment): MemorySegment
    override fun allocateFrom(elementLayout: ValueLayout.OfByte, vararg values: Byte): MemorySegment
    override fun allocateFrom(elementLayout: ValueLayout.OfChar, vararg values: Char): MemorySegment
    override fun allocateFrom(
        elementLayout: ValueLayout.OfShort,
        vararg values: Short
    ): MemorySegment

    override fun allocateFrom(elementLayout: ValueLayout.OfInt, vararg values: Int): MemorySegment
    override fun allocateFrom(elementLayout: ValueLayout.OfLong, vararg values: Long): MemorySegment
    override fun allocateFrom(
        elementLayout: ValueLayout.OfFloat,
        vararg values: Float
    ): MemorySegment

    override fun allocateFrom(
        elementLayout: ValueLayout.OfDouble,
        vararg values: Double
    ): MemorySegment
}

expect class MemorySegment internal constructor(value: Any) {
    internal val value: Any

    fun address(): Long
    fun byteSize(): Long
    fun asReadOnly(): MemorySegment
    fun asSlice(offset: Long, size: Long): MemorySegment
    fun asSlice(offset: Long, newSize: Long, byteAlignment: Long): MemorySegment
    fun asSlice(offset: Long, layout: MemoryLayout): MemorySegment
    fun asSlice(offset: Long): MemorySegment
    fun reinterpret(size: Long): MemorySegment
    fun reinterpret(
        size: Long,
        arena: Arena? = null,
        cleanup: Consumer<MemorySegment>? = {},
    ): MemorySegment

    fun getString(offset: Long, charset: Charset = Charsets.UTF_8): String
    fun setString(offset: Long, value: String)
    fun setString(offset: Long, value: String, charset: Charset)
    fun asByteBuffer(): ByteBuffer

    fun get(layout: ValueLayout.OfBoolean, offset: Long): Boolean
    fun set(layout: ValueLayout.OfBoolean, offset: Long, value: Boolean)
    fun getAtIndex(layout: ValueLayout.OfBoolean, index: Long): Boolean
    fun setAtIndex(layout: ValueLayout.OfBoolean, index: Long, value: Boolean)

    fun get(layout: ValueLayout.OfByte, offset: Long): Byte
    fun set(layout: ValueLayout.OfByte, offset: Long, value: Byte)
    fun getAtIndex(layout: ValueLayout.OfByte, index: Long): Byte
    fun setAtIndex(layout: ValueLayout.OfByte, index: Long, value: Byte)

    fun get(layout: ValueLayout.OfChar, offset: Long): Char
    fun set(layout: ValueLayout.OfChar, offset: Long, value: Char)
    fun getAtIndex(layout: ValueLayout.OfChar, index: Long): Char
    fun setAtIndex(layout: ValueLayout.OfChar, index: Long, value: Char)

    fun get(layout: ValueLayout.OfShort, offset: Long): Short
    fun set(layout: ValueLayout.OfShort, offset: Long, value: Short)
    fun getAtIndex(layout: ValueLayout.OfShort, index: Long): Short
    fun setAtIndex(layout: ValueLayout.OfShort, index: Long, value: Short)

    fun get(layout: ValueLayout.OfInt, offset: Long): Int
    fun set(layout: ValueLayout.OfInt, offset: Long, value: Int)
    fun getAtIndex(layout: ValueLayout.OfInt, index: Long): Int
    fun setAtIndex(layout: ValueLayout.OfInt, index: Long, value: Int)

    fun get(layout: ValueLayout.OfLong, offset: Long): Long
    fun set(layout: ValueLayout.OfLong, offset: Long, value: Long)
    fun getAtIndex(layout: ValueLayout.OfLong, index: Long): Long
    fun setAtIndex(layout: ValueLayout.OfLong, index: Long, value: Long)

    fun get(layout: ValueLayout.OfFloat, offset: Long): Float
    fun set(layout: ValueLayout.OfFloat, offset: Long, value: Float)
    fun getAtIndex(layout: ValueLayout.OfFloat, index: Long): Float
    fun setAtIndex(layout: ValueLayout.OfFloat, index: Long, value: Float)

    fun get(layout: ValueLayout.OfDouble, offset: Long): Double
    fun set(layout: ValueLayout.OfDouble, offset: Long, value: Double)
    fun getAtIndex(layout: ValueLayout.OfDouble, index: Long): Double
    fun setAtIndex(layout: ValueLayout.OfDouble, index: Long, value: Double)

    fun get(layout: AddressLayout, offset: Long): MemorySegment
    fun set(layout: AddressLayout, offset: Long, value: MemorySegment)
    fun getAtIndex(layout: AddressLayout, index: Long): MemorySegment
    fun setAtIndex(layout: AddressLayout, index: Long, value: MemorySegment)

    fun copyFrom(src: MemorySegment): MemorySegment

    class Scope {
        val isAlive: Boolean
    }

    companion object {
        @JvmStatic
        fun ofBuffer(buffer: ByteBuffer): MemorySegment

        @JvmStatic
        fun ofArray(values: ByteArray): MemorySegment

        @JvmStatic
        fun ofArray(values: CharArray): MemorySegment

        @JvmStatic
        fun ofArray(values: ShortArray): MemorySegment

        @JvmStatic
        fun ofArray(values: IntArray): MemorySegment

        @JvmStatic
        fun ofArray(values: LongArray): MemorySegment

        @JvmStatic
        fun ofArray(values: FloatArray): MemorySegment

        @JvmStatic
        fun ofArray(values: DoubleArray): MemorySegment

        @JvmStatic
        fun ofAddress(address: Long): MemorySegment

        @JvmStatic
        val NULL: MemorySegment

        @JvmStatic
        fun copy(
            src: MemorySegment,
            srcOffset: Long,
            dst: MemorySegment,
            dstOffset: Long,
            bytes: Long
        )

        @JvmStatic
        fun copy(
            src: MemorySegment,
            srcElementLayout: ValueLayout,
            srcOffset: Long,
            dst: MemorySegment,
            dstElementLayout: ValueLayout,
            dstOffset: Long,
            count: Long,
        )

        @JvmStatic
        fun copy(
            src: MemorySegment,
            srcLayout: ValueLayout,
            srcOffset: Long,
            dstArray: Any,
            dstIndex: Int,
            count: Int
        )

        @JvmStatic
        fun copy(
            srcArray: Any,
            srcIndex: Int,
            dst: MemorySegment,
            dstLayout: ValueLayout,
            dstOffset: Long,
            count: Int
        )

        @JvmStatic
        fun mismatch(
            src: MemorySegment,
            srcRange: LongRange,
            dst: MemorySegment,
            dstRange: LongRange
        ): Long
    }
}

expect sealed interface MemoryLayout {
    val value: Any

    @Suppress("RedundantModalityModifier")
    open fun byteSize(): Long

    @Suppress("RedundantModalityModifier")
    open val byteAlignment: Long

    @Suppress("RedundantModalityModifier")
    open fun byteOffset(vararg path: PathElement): Long

    @Suppress("RedundantModalityModifier")
    open fun varHandle(vararg path: PathElement): VarHandle
    fun withName(name: String): MemoryLayout
    fun withByteAlignment(alignment: Long): MemoryLayout

    @Suppress("RedundantModalityModifier")
    open fun select(vararg path: PathElement): MemoryLayout

    @Suppress("RedundantModalityModifier")
    open fun sliceHandle(vararg path: PathElement): MethodHandle

    class PathElement internal constructor(value: Any) {
        internal val value: Any

        companion object {
            @JvmStatic
            fun groupElement(name: String): PathElement

            @JvmStatic
            fun groupElement(index: Long): PathElement

            @JvmStatic
            fun sequenceElement(index: Long): PathElement

            @JvmStatic
            fun sequenceElement(): PathElement

            @JvmStatic
            fun dereferenceElement(): PathElement
        }
    }

    companion object {
        @JvmStatic
        fun sequenceLayout(elementCount: Long, elementLayout: MemoryLayout): SequenceLayout

        @JvmStatic
        fun structLayout(vararg elements: MemoryLayout): StructLayout

        @JvmStatic
        fun paddingLayout(byteSize: Long): PaddingLayout

        @JvmStatic
        fun unionLayout(vararg elements: MemoryLayout): UnionLayout
    }
}

expect class SequenceLayout internal constructor(value: Any) : MemoryLayout {
    override val value: Any
    override fun withName(name: String): SequenceLayout
    override fun withByteAlignment(alignment: Long): SequenceLayout
    fun elementCount(): Long
    fun elementLayout(): MemoryLayout
}

expect sealed interface GroupLayout : MemoryLayout {
    override val value: Any
    override fun withName(name: String): GroupLayout
    override fun withByteAlignment(alignment: Long): GroupLayout

    @Suppress("RedundantModalityModifier")
    open fun memberLayouts(): List<MemoryLayout>
}

expect class PaddingLayout internal constructor(value: Any) : MemoryLayout {
    override val value: Any
    override fun withName(name: String): PaddingLayout
    override fun withByteAlignment(alignment: Long): PaddingLayout
}

expect class StructLayout internal constructor(value: Any) : GroupLayout {
    override val value: Any
    override fun withName(name: String): StructLayout
    override fun withByteAlignment(alignment: Long): StructLayout
}

expect class UnionLayout internal constructor(value: Any) : GroupLayout {
    override val value: Any
    override fun withName(name: String): UnionLayout
    override fun withByteAlignment(alignment: Long): UnionLayout
}

expect sealed interface ValueLayout : MemoryLayout {
    override fun withName(name: String): ValueLayout
    override fun withByteAlignment(alignment: Long): ValueLayout

    class OfBoolean internal constructor(value: Any) : ValueLayout {
        override val value: Any
        override fun withName(name: String): OfBoolean
        override fun withByteAlignment(alignment: Long): OfBoolean
    }

    class OfByte internal constructor(value: Any) : ValueLayout {
        override val value: Any
        override fun withName(name: String): OfByte
        override fun withByteAlignment(alignment: Long): OfByte
    }

    class OfChar internal constructor(value: Any) : ValueLayout {
        override val value: Any
        override fun withName(name: String): OfChar
        override fun withByteAlignment(alignment: Long): OfChar
    }

    class OfShort internal constructor(value: Any) : ValueLayout {
        override val value: Any
        override fun withName(name: String): OfShort
        override fun withByteAlignment(alignment: Long): OfShort
    }

    class OfInt internal constructor(value: Any) : ValueLayout {
        override val value: Any
        override fun withName(name: String): OfInt
        override fun withByteAlignment(alignment: Long): OfInt
    }

    class OfLong internal constructor(value: Any) : ValueLayout {
        override val value: Any
        override fun withName(name: String): OfLong
        override fun withByteAlignment(alignment: Long): OfLong
    }

    class OfFloat internal constructor(value: Any) : ValueLayout {
        override val value: Any
        override fun withName(name: String): OfFloat
        override fun withByteAlignment(alignment: Long): OfFloat
    }

    class OfDouble internal constructor(value: Any) : ValueLayout {
        override val value: Any
        override fun withName(name: String): OfDouble
        override fun withByteAlignment(alignment: Long): OfDouble
    }

    companion object {
        @JvmStatic
        val ADDRESS: AddressLayout

        @JvmStatic
        val JAVA_BYTE: OfByte

        @JvmStatic
        val JAVA_BOOLEAN: OfBoolean

        @JvmStatic
        val JAVA_CHAR: OfChar

        @JvmStatic
        val JAVA_SHORT: OfShort

        @JvmStatic
        val JAVA_INT: OfInt

        @JvmStatic
        val JAVA_LONG: OfLong

        @JvmStatic
        val JAVA_FLOAT: OfFloat

        @JvmStatic
        val JAVA_DOUBLE: OfDouble

        @JvmStatic
        val ADDRESS_UNALIGNED: AddressLayout

        @JvmStatic
        val JAVA_CHAR_UNALIGNED: OfChar

        @JvmStatic
        val JAVA_SHORT_UNALIGNED: OfShort

        @JvmStatic
        val JAVA_INT_UNALIGNED: OfInt

        @JvmStatic
        val JAVA_LONG_UNALIGNED: OfLong

        @JvmStatic
        val JAVA_FLOAT_UNALIGNED: OfFloat

        @JvmStatic
        val JAVA_DOUBLE_UNALIGNED: OfDouble
    }
}

expect class AddressLayout internal constructor(value: Any) : ValueLayout {
    override val value: Any
    override fun withName(name: String): AddressLayout
    override fun withByteAlignment(alignment: Long): AddressLayout
    fun withTargetLayout(layout: MemoryLayout): AddressLayout
}

internal expect fun wrapLayout(inner: Any): MemoryLayout
