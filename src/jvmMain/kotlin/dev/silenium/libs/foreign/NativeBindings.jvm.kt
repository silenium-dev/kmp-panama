package dev.silenium.libs.foreign

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.function.Consumer
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.jvm.javaMethod
import java.lang.foreign.AddressLayout as FAddressLayout
import java.lang.foreign.Arena as FArena
import java.lang.foreign.FunctionDescriptor as FFunctionDescriptor
import java.lang.foreign.GroupLayout as FGroupLayout
import java.lang.foreign.Linker as FLinker
import java.lang.foreign.MemoryLayout as FMemoryLayout
import java.lang.foreign.MemorySegment as FMemorySegment
import java.lang.foreign.PaddingLayout as FPaddingLayout
import java.lang.foreign.SequenceLayout as FSequenceLayout
import java.lang.foreign.StructLayout as FStructLayout
import java.lang.foreign.SymbolLookup as FSymbolLookup
import java.lang.foreign.UnionLayout as FUnionLayout
import java.lang.foreign.ValueLayout as FValueLayout
import java.lang.invoke.MethodHandle as JMethodHandle
import java.lang.invoke.MethodHandles as JMethodHandles
import java.lang.invoke.VarHandle as JVarHandle

@ConsistentCopyVisibility
actual data class SymbolLookup internal actual constructor(internal actual val value: Any) {
    internal val lookup: FSymbolLookup get() = value as FSymbolLookup

    actual fun find(name: String) = lookup.find(name).getOrNull()?.let(::MemorySegment)
    actual fun findOrThrow(name: String) = lookup.findOrThrow(name).let(::MemorySegment)
    actual fun or(other: SymbolLookup): SymbolLookup = lookup.or(other.lookup).let(::SymbolLookup)

    actual companion object {
        @JvmStatic
        actual fun loaderLookup(): SymbolLookup =
            FSymbolLookup.loaderLookup().let(::SymbolLookup)

        @JvmStatic
        actual fun libraryLookup(name: String, arena: Arena): SymbolLookup =
            FSymbolLookup.libraryLookup(name, arena.arena).let(::SymbolLookup)

        @JvmStatic
        actual fun libraryLookup(path: Path, arena: Arena): SymbolLookup =
            FSymbolLookup.libraryLookup(path, arena.arena).let(::SymbolLookup)
    }
}

object SegmentMapper {
    @JvmStatic
    fun fromNative(segment: FMemorySegment): MemorySegment = MemorySegment(segment)

    @JvmStatic
    fun toNative(segment: MemorySegment): FMemorySegment = segment.segment

    @JvmStatic
    fun map(handle: MethodHandle): MethodHandle {
        val segmentIndices = handle.value.type().parameterList().withIndex().filter {
            it.value.isAssignableFrom(MemorySegment::class.java)
        }.map(IndexedValue<*>::index)
        val fromNative = MethodHandles.lookup().unreflect(SegmentMapper::fromNative.javaMethod!!)
        val toNative = MethodHandles.lookup().unreflect(SegmentMapper::toNative.javaMethod!!)
        val handle =
            if (handle.value.type().returnType().isAssignableFrom(MemorySegment::class.java)) {
                MethodHandles.filterReturnValue(handle.value, toNative)
            } else {
                handle.value
            }
        return segmentIndices.fold(handle) { result, index ->
            MethodHandles.filterArguments(result, index, fromNative)
        }.let(::MethodHandle)
    }
}

@ConsistentCopyVisibility
actual data class Linker internal actual constructor(internal actual val value: Any) {
    internal val linker: FLinker get() = value as FLinker

    actual fun downcallHandle(
        descriptor: FunctionDescriptor,
        vararg option: Option,
    ): MethodHandle = linker.downcallHandle(
        descriptor.descriptor,
        *option.map(Option::option).toTypedArray()
    ).let(::MethodHandle)

    actual fun downcallHandle(
        symbol: MemorySegment,
        descriptor: FunctionDescriptor
    ): MethodHandle = linker.downcallHandle(
        symbol.segment,
        descriptor.descriptor,
    ).let(::MethodHandle)

    actual fun downcallHandle(
        symbol: MemorySegment,
        descriptor: FunctionDescriptor,
        vararg option: Option,
    ): MethodHandle = linker.downcallHandle(
        symbol.segment,
        descriptor.descriptor,
        *option.map(Option::option).toTypedArray()
    ).let(::MethodHandle)

    actual fun upcallStub(
        target: MethodHandle,
        descriptor: FunctionDescriptor,
        arena: Arena
    ): MemorySegment = linker.upcallStub(
        SegmentMapper.map(target).value,
        descriptor.descriptor,
        arena.arena,
    ).let(::MemorySegment)

    actual fun defaultLookup(): SymbolLookup =
        linker.defaultLookup().let(::SymbolLookup)

    actual fun canonicalLayouts(): Map<String, MemoryLayout> =
        linker.canonicalLayouts().mapValues { (_, it) -> wrapLayout(it) }

    @ConsistentCopyVisibility
actual data class Option internal actual constructor(internal actual val value: Any) {
        internal val option get() = value as FLinker.Option

        actual companion object {
            @JvmStatic
            actual fun firstVariadicArg(var0: Int): Option =
                FLinker.Option.firstVariadicArg(var0).let(::Option)

            @JvmStatic
            actual fun captureCallState(vararg var0: String): Option =
                FLinker.Option.captureCallState(*var0).let(::Option)

            @JvmStatic
            actual fun captureStateLayout(): StructLayout =
                FLinker.Option.captureStateLayout().let(::StructLayout)

            @JvmStatic
            actual fun critical(var0: Boolean): Option =
                FLinker.Option.critical(var0).let(::Option)
        }
    }

    actual companion object {
        @JvmStatic
        actual fun nativeLinker(): Linker =
            FLinker.nativeLinker().let(::Linker)
    }
}

@ConsistentCopyVisibility
actual data class VarHandle internal actual constructor(internal actual val value: Any) {
    internal val varHandle: JVarHandle get() = value as JVarHandle
    actual fun set(vararg args: Any?) {
        val mappedArgs = args.map(Any?::toNative)
        varHandle.toMethodHandle(JVarHandle.AccessMode.SET)
            .invokeWithArguments(mappedArgs)
    }

    actual fun get(vararg args: Any?): Any? {
        val mappedArgs = args.map(Any?::toNative)
        val result = varHandle.toMethodHandle(JVarHandle.AccessMode.GET)
            .invokeWithArguments(mappedArgs)
        return result.fromNative()
    }
}

actual object MethodHandles {
    actual object Lookup {
        @Throws(ReflectiveOperationException::class)
        actual fun findVirtual(klass: Class<*>, name: String, type: MethodType): MethodHandle =
            JMethodHandles.lookup().findVirtual(klass, name, type).let(::MethodHandle)

        @Throws(ReflectiveOperationException::class)
        actual fun unreflect(method: Method): MethodHandle =
            JMethodHandles.lookup().unreflect(method).let(::MethodHandle)
    }

    @JvmStatic
    actual fun lookup(): Lookup = Lookup
}

@ConsistentCopyVisibility
actual data class MethodHandle internal actual constructor(internal actual val value: JMethodHandle) {
    actual operator fun invoke(vararg args: Any?): Any? {
        val mappedArgs = args.map(Any?::toNative)
        val result = value.invokeWithArguments(mappedArgs)
        return result.fromNative()
    }

    actual fun invokeExact(vararg args: Any?): Any? {
        val mappedArgs = args.map(Any?::toNative)
        val result = value.invokeWithArguments(mappedArgs)
        return result.fromNative()
    }

    actual fun bindTo(target: Any?): MethodHandle =
        MethodHandle(value.bindTo(target.toNative()))

    actual fun asSpreader(klass: Class<*>, count: Int): MethodHandle =
        value.asSpreader(klass, count).let(::MethodHandle)
}

private fun Any?.toNative() = when (this) {
    is MemorySegment -> segment
    else -> this
}

private fun Any?.fromNative() = when (this) {
    is FMemorySegment -> MemorySegment(this)
    else -> this
}

@ConsistentCopyVisibility
actual data class FunctionDescriptor internal actual constructor(internal actual val value: Any) {
    internal val descriptor: FFunctionDescriptor get() = value as FFunctionDescriptor

    actual fun appendArgumentLayouts(vararg layouts: MemoryLayout): FunctionDescriptor =
        descriptor.appendArgumentLayouts(*layouts.map(MemoryLayout::layout).toTypedArray())
            .let(::FunctionDescriptor)

    actual fun argumentLayouts(): List<MemoryLayout> =
        descriptor.argumentLayouts().map(::wrapLayout)

    actual fun toMethodType(): MethodType = descriptor.toMethodType()

    actual companion object {
        @JvmStatic
        actual fun of(
            returnType: MemoryLayout,
            vararg parameters: MemoryLayout,
        ) = FFunctionDescriptor.of(
            returnType.layout,
            *parameters.map(MemoryLayout::layout).toTypedArray()
        ).let(::FunctionDescriptor)

        @JvmStatic
        actual fun ofVoid(
            vararg parameters: MemoryLayout,
        ) = FFunctionDescriptor.ofVoid(
            *parameters.map(MemoryLayout::layout).toTypedArray()
        ).let(::FunctionDescriptor)
    }
}

@ConsistentCopyVisibility
actual data class Arena internal actual constructor(internal actual val value: Any) : SegmentAllocator,
    AutoCloseable {
    internal val arena: FArena get() = value as FArena

    actual override fun close() = arena.close()
    actual val scope get() = MemorySegment.Scope(arena.scope())

    actual override fun allocate(size: Long, alignment: Long) =
        MemorySegment(arena.allocate(size, alignment))

    actual override fun allocate(layout: MemoryLayout) =
        MemorySegment(arena.allocate(layout.layout))

    actual override fun allocate(layout: MemoryLayout, count: Long) =
        MemorySegment(arena.allocate(layout.layout, count))

    actual override fun allocateFrom(str: String): MemorySegment =
        arena.allocateFrom(str).let(::MemorySegment)

    actual override fun allocateFrom(str: String, charset: Charset): MemorySegment =
        arena.allocateFrom(str, charset).let(::MemorySegment)

    actual override fun allocateFrom(layout: ValueLayout.OfByte, value: Byte): MemorySegment =
        arena.allocateFrom(layout.layout, value).let(::MemorySegment)

    actual override fun allocateFrom(layout: ValueLayout.OfChar, value: Char): MemorySegment =
        arena.allocateFrom(layout.layout, value).let(::MemorySegment)

    actual override fun allocateFrom(layout: ValueLayout.OfShort, value: Short): MemorySegment =
        arena.allocateFrom(layout.layout, value).let(::MemorySegment)

    actual override fun allocateFrom(layout: ValueLayout.OfInt, value: Int): MemorySegment =
        arena.allocateFrom(layout.layout, value).let(::MemorySegment)

    actual override fun allocateFrom(layout: ValueLayout.OfLong, value: Long): MemorySegment =
        arena.allocateFrom(layout.layout, value).let(::MemorySegment)

    actual override fun allocateFrom(layout: ValueLayout.OfFloat, value: Float): MemorySegment =
        arena.allocateFrom(layout.layout, value).let(::MemorySegment)

    actual override fun allocateFrom(layout: ValueLayout.OfDouble, value: Double): MemorySegment =
        arena.allocateFrom(layout.layout, value).let(::MemorySegment)

    actual override fun allocateFrom(layout: AddressLayout, value: MemorySegment): MemorySegment =
        arena.allocateFrom(layout.layout, value.segment).let(::MemorySegment)

    actual override fun allocateFrom(
        elementLayout: ValueLayout.OfByte,
        vararg values: Byte
    ): MemorySegment = arena.allocateFrom(elementLayout.layout, *values).let(::MemorySegment)

    actual override fun allocateFrom(
        elementLayout: ValueLayout.OfChar,
        vararg values: Char
    ): MemorySegment = arena.allocateFrom(elementLayout.layout, *values).let(::MemorySegment)

    actual override fun allocateFrom(
        elementLayout: ValueLayout.OfShort,
        vararg values: Short
    ): MemorySegment = arena.allocateFrom(elementLayout.layout, *values).let(::MemorySegment)

    actual override fun allocateFrom(
        elementLayout: ValueLayout.OfInt,
        vararg values: Int
    ): MemorySegment = arena.allocateFrom(elementLayout.layout, *values).let(::MemorySegment)

    actual override fun allocateFrom(
        elementLayout: ValueLayout.OfLong,
        vararg values: Long
    ): MemorySegment = arena.allocateFrom(elementLayout.layout, *values).let(::MemorySegment)

    actual override fun allocateFrom(
        elementLayout: ValueLayout.OfFloat,
        vararg values: Float
    ): MemorySegment = arena.allocateFrom(elementLayout.layout, *values).let(::MemorySegment)

    actual override fun allocateFrom(
        elementLayout: ValueLayout.OfDouble,
        vararg values: Double
    ): MemorySegment = arena.allocateFrom(elementLayout.layout, *values).let(::MemorySegment)

    actual companion object {
        @JvmStatic
        actual fun ofAuto() = Arena(FArena.ofAuto())

        @JvmStatic
        actual fun ofShared() = Arena(FArena.ofShared())

        @JvmStatic
        actual fun ofConfined() = Arena(FArena.ofConfined())

        @JvmStatic
        actual fun global() = Arena(FArena.global())
    }
}

@ConsistentCopyVisibility
actual data class MemorySegment internal actual constructor(internal actual val value: Any) {
    internal val segment: FMemorySegment get() = value as FMemorySegment

    actual fun address(): Long = segment.address()
    actual fun byteSize(): Long = segment.byteSize()
    actual fun asReadOnly() = MemorySegment(segment.asReadOnly())
    actual fun asSlice(offset: Long, size: Long): MemorySegment =
        segment.asSlice(offset, size).let(::MemorySegment)

    actual fun asSlice(offset: Long, newSize: Long, byteAlignment: Long): MemorySegment =
        segment.asSlice(offset, newSize, byteAlignment).let(::MemorySegment)

    actual fun asSlice(offset: Long, layout: MemoryLayout): MemorySegment =
        segment.asSlice(offset, layout.layout).let(::MemorySegment)

    actual fun asSlice(offset: Long): MemorySegment =
        segment.asSlice(offset).let(::MemorySegment)

    actual fun reinterpret(size: Long) = MemorySegment(segment.reinterpret(size))

    actual fun reinterpret(
        size: Long,
        arena: Arena?,
        cleanup: Consumer<MemorySegment>?,
    ): MemorySegment = when {
        arena == null -> segment.reinterpret(size)
        else -> segment.reinterpret(
            size,
            arena.arena,
            cleanup?.let { cleanup ->
                { cleanup.accept(MemorySegment(it)) }
            }
        )
    }.let(::MemorySegment)

    actual fun getString(offset: Long, charset: Charset): String =
        segment.getString(offset, charset)

    actual fun setString(offset: Long, value: String, charset: Charset) =
        segment.setString(offset, value, charset)

    actual fun setString(offset: Long, value: String) =
        segment.setString(offset, value)

    actual fun asByteBuffer(): ByteBuffer =
        segment.asByteBuffer()

    actual fun get(layout: ValueLayout.OfBoolean, offset: Long): Boolean =
        segment.get(layout.layout, offset)

    actual fun set(layout: ValueLayout.OfBoolean, offset: Long, value: Boolean) =
        segment.set(layout.layout, offset, value)

    actual fun getAtIndex(layout: ValueLayout.OfBoolean, index: Long): Boolean =
        segment.getAtIndex(layout.layout, index)

    actual fun setAtIndex(layout: ValueLayout.OfBoolean, index: Long, value: Boolean) =
        segment.setAtIndex(layout.layout, index, value)

    actual fun get(layout: ValueLayout.OfByte, offset: Long): Byte =
        segment.get(layout.layout, offset)

    actual fun set(layout: ValueLayout.OfByte, offset: Long, value: Byte) =
        segment.set(layout.layout, offset, value)

    actual fun getAtIndex(layout: ValueLayout.OfByte, index: Long): Byte =
        segment.getAtIndex(layout.layout, index)

    actual fun setAtIndex(layout: ValueLayout.OfByte, index: Long, value: Byte) =
        segment.setAtIndex(layout.layout, index, value)

    actual fun get(layout: ValueLayout.OfChar, offset: Long): Char =
        segment.get(layout.layout, offset)

    actual fun set(layout: ValueLayout.OfChar, offset: Long, value: Char) =
        segment.set(layout.layout, offset, value)

    actual fun getAtIndex(layout: ValueLayout.OfChar, index: Long): Char =
        segment.getAtIndex(layout.layout, index)

    actual fun setAtIndex(layout: ValueLayout.OfChar, index: Long, value: Char) =
        segment.setAtIndex(layout.layout, index, value)

    actual fun get(layout: ValueLayout.OfShort, offset: Long): Short =
        segment.get(layout.layout, offset)

    actual fun set(layout: ValueLayout.OfShort, offset: Long, value: Short) =
        segment.set(layout.layout, offset, value)

    actual fun getAtIndex(layout: ValueLayout.OfShort, index: Long): Short =
        segment.getAtIndex(layout.layout, index)

    actual fun setAtIndex(layout: ValueLayout.OfShort, index: Long, value: Short) =
        segment.setAtIndex(layout.layout, index, value)

    actual fun get(layout: ValueLayout.OfInt, offset: Long): Int =
        segment.get(layout.layout, offset)

    actual fun set(layout: ValueLayout.OfInt, offset: Long, value: Int) =
        segment.set(layout.layout, offset, value)

    actual fun getAtIndex(layout: ValueLayout.OfInt, index: Long): Int =
        segment.getAtIndex(layout.layout, index)

    actual fun setAtIndex(layout: ValueLayout.OfInt, index: Long, value: Int) =
        segment.setAtIndex(layout.layout, index, value)

    actual fun get(layout: ValueLayout.OfLong, offset: Long): Long =
        segment.get(layout.layout, offset)

    actual fun set(layout: ValueLayout.OfLong, offset: Long, value: Long) =
        segment.set(layout.layout, offset, value)

    actual fun getAtIndex(layout: ValueLayout.OfLong, index: Long): Long =
        segment.getAtIndex(layout.layout, index)

    actual fun setAtIndex(layout: ValueLayout.OfLong, index: Long, value: Long) =
        segment.setAtIndex(layout.layout, index, value)

    actual fun get(layout: ValueLayout.OfFloat, offset: Long): Float =
        segment.get(layout.layout, offset)

    actual fun set(layout: ValueLayout.OfFloat, offset: Long, value: Float) =
        segment.set(layout.layout, offset, value)

    actual fun getAtIndex(layout: ValueLayout.OfFloat, index: Long): Float =
        segment.getAtIndex(layout.layout, index)

    actual fun setAtIndex(layout: ValueLayout.OfFloat, index: Long, value: Float) =
        segment.setAtIndex(layout.layout, index, value)

    actual fun get(layout: ValueLayout.OfDouble, offset: Long): Double =
        segment.get(layout.layout, offset)

    actual fun set(layout: ValueLayout.OfDouble, offset: Long, value: Double) =
        segment.set(layout.layout, offset, value)

    actual fun getAtIndex(layout: ValueLayout.OfDouble, index: Long): Double =
        segment.getAtIndex(layout.layout, index)

    actual fun setAtIndex(layout: ValueLayout.OfDouble, index: Long, value: Double) =
        segment.setAtIndex(layout.layout, index, value)

    actual fun get(layout: AddressLayout, offset: Long): MemorySegment =
        segment.get(layout.layout, offset).let(::MemorySegment)

    actual fun set(layout: AddressLayout, offset: Long, value: MemorySegment) =
        segment.set(layout.layout, offset, value.segment)

    actual fun getAtIndex(layout: AddressLayout, index: Long): MemorySegment =
        segment.getAtIndex(layout.layout, index).let(::MemorySegment)

    actual fun setAtIndex(layout: AddressLayout, index: Long, value: MemorySegment) =
        segment.setAtIndex(layout.layout, index, value.segment)

    actual fun copyFrom(src: MemorySegment): MemorySegment =
        segment.copyFrom(src.segment).let(::MemorySegment)

    @ConsistentCopyVisibility
actual data class Scope(val scope: FMemorySegment.Scope) {
        actual val isAlive: Boolean get() = scope.isAlive
    }

    actual companion object {
        @JvmStatic
        actual fun ofBuffer(buffer: ByteBuffer): MemorySegment =
            FMemorySegment.ofBuffer(buffer).let(::MemorySegment)

        @JvmStatic
        actual fun ofArray(values: ByteArray): MemorySegment =
            FMemorySegment.ofArray(values).let(::MemorySegment)

        @JvmStatic
        actual fun ofArray(values: CharArray): MemorySegment =
            FMemorySegment.ofArray(values).let(::MemorySegment)

        @JvmStatic
        actual fun ofArray(values: ShortArray): MemorySegment =
            FMemorySegment.ofArray(values).let(::MemorySegment)

        @JvmStatic
        actual fun ofArray(values: IntArray): MemorySegment =
            FMemorySegment.ofArray(values).let(::MemorySegment)

        @JvmStatic
        actual fun ofArray(values: LongArray): MemorySegment =
            FMemorySegment.ofArray(values).let(::MemorySegment)

        @JvmStatic
        actual fun ofArray(values: FloatArray): MemorySegment =
            FMemorySegment.ofArray(values).let(::MemorySegment)

        @JvmStatic
        actual fun ofArray(values: DoubleArray): MemorySegment =
            FMemorySegment.ofArray(values).let(::MemorySegment)

        @JvmStatic
        actual fun ofAddress(address: Long): MemorySegment =
            FMemorySegment.ofAddress(address).let(::MemorySegment)

        @JvmStatic
        actual val NULL = MemorySegment(FMemorySegment.NULL)

        @JvmStatic
        actual fun copy(
            src: MemorySegment,
            srcOffset: Long,
            dst: MemorySegment,
            dstOffset: Long,
            bytes: Long
        ) = FMemorySegment.copy(src.segment, srcOffset, dst.segment, dstOffset, bytes)

        @JvmStatic
        actual fun copy(
            src: MemorySegment,
            srcElementLayout: ValueLayout,
            srcOffset: Long,
            dst: MemorySegment,
            dstElementLayout: ValueLayout,
            dstOffset: Long,
            count: Long,
        ) = FMemorySegment.copy(
            src.segment,
            srcElementLayout.layout,
            srcOffset,
            dst.segment,
            dstElementLayout.layout,
            dstOffset,
            count
        )

        @JvmStatic
        actual fun copy(
            src: MemorySegment,
            srcLayout: ValueLayout,
            srcOffset: Long,
            dstArray: Any,
            dstIndex: Int,
            count: Int
        ) = FMemorySegment.copy(
            src.segment,
            srcLayout.layout,
            srcOffset,
            dstArray,
            dstIndex,
            count
        )

        @JvmStatic
        actual fun copy(
            srcArray: Any,
            srcIndex: Int,
            dst: MemorySegment,
            dstLayout: ValueLayout,
            dstOffset: Long,
            count: Int
        ) = FMemorySegment.copy(
            srcArray,
            srcIndex,
            dst.segment,
            dstLayout.layout,
            dstOffset,
            count
        )

        @JvmStatic
        actual fun mismatch(
            src: MemorySegment,
            srcRange: LongRange,
            dst: MemorySegment,
            dstRange: LongRange
        ): Long = FMemorySegment.mismatch(
            src.segment,
            srcRange.first,
            srcRange.last + 1,
            dst.segment,
            dstRange.first,
            dstRange.last + 1
        )
    }
}

actual sealed interface MemoryLayout {
    actual val value: Any
    val layout: FMemoryLayout
    actual fun byteSize(): Long = layout.byteSize()
    actual val byteAlignment: Long get() = layout.byteAlignment()
    actual fun byteOffset(vararg path: PathElement) =
        layout.byteOffset(*path.map(PathElement::element).toTypedArray())

    actual fun varHandle(vararg path: PathElement): VarHandle =
        layout.varHandle(*path.map(PathElement::element).toTypedArray()).let(::VarHandle)

    actual fun withName(name: String): MemoryLayout
    actual fun select(vararg path: PathElement): MemoryLayout =
        layout.select(*path.map(PathElement::element).toTypedArray())
            .let(::wrapLayout)

    actual fun sliceHandle(vararg path: PathElement): MethodHandle =
        layout.sliceHandle(*path.map(PathElement::element).toTypedArray())
            .let(::MethodHandle)

    actual fun withByteAlignment(alignment: Long): MemoryLayout

    @ConsistentCopyVisibility
actual data class PathElement internal actual constructor(internal actual val value: Any) {
        constructor(element: FMemoryLayout.PathElement) : this(element as Any)

        internal val element: FMemoryLayout.PathElement get() = value as FMemoryLayout.PathElement

        actual companion object {
            @JvmStatic
            actual fun groupElement(name: String): PathElement =
                FMemoryLayout.PathElement.groupElement(name).let(::PathElement)

            @JvmStatic
            actual fun groupElement(index: Long): PathElement =
                FMemoryLayout.PathElement.groupElement(index).let(::PathElement)

            @JvmStatic
            actual fun sequenceElement(index: Long): PathElement =
                FMemoryLayout.PathElement.sequenceElement(index).let(::PathElement)

            @JvmStatic
            actual fun sequenceElement(): PathElement =
                FMemoryLayout.PathElement.sequenceElement().let(::PathElement)

            @JvmStatic
            actual fun dereferenceElement(): PathElement =
                FMemoryLayout.PathElement.dereferenceElement().let(::PathElement)
        }
    }

    actual companion object {
        @JvmStatic
        actual fun sequenceLayout(elementCount: Long, elementLayout: MemoryLayout): SequenceLayout =
            FMemoryLayout.sequenceLayout(elementCount, elementLayout.layout).let(::SequenceLayout)

        @JvmStatic
        actual fun structLayout(vararg elements: MemoryLayout): StructLayout =
            FMemoryLayout.structLayout(*elements.map { it.layout }.toTypedArray())
                .let(::StructLayout)

        @JvmStatic
        actual fun paddingLayout(byteSize: Long): PaddingLayout =
            FMemoryLayout.paddingLayout(byteSize).let(::PaddingLayout)

        @JvmStatic
        actual fun unionLayout(vararg elements: MemoryLayout): UnionLayout =
            FMemoryLayout.unionLayout(*elements.map { it.layout }.toTypedArray())
                .let(::UnionLayout)
    }
}

@ConsistentCopyVisibility
actual data class SequenceLayout internal actual constructor(actual override val value: Any) :
    MemoryLayout {
    override val layout: FSequenceLayout
        get() = value as FSequenceLayout

    actual override fun withName(name: String): SequenceLayout =
        layout.withName(name).let(::SequenceLayout)

    actual override fun withByteAlignment(alignment: Long): SequenceLayout =
        layout.withByteAlignment(alignment).let(::SequenceLayout)

    actual fun elementCount(): Long = layout.elementCount()
    actual fun elementLayout(): MemoryLayout = layout.elementLayout().let(::wrapLayout)
}

actual sealed interface GroupLayout : MemoryLayout {
    override val layout: FGroupLayout
        get() = value as FGroupLayout
    actual override val value: Any
    actual override fun withName(name: String): GroupLayout
    actual override fun withByteAlignment(alignment: Long): GroupLayout
    actual fun memberLayouts(): List<MemoryLayout> =
        layout.memberLayouts().map(::wrapLayout)
}

@ConsistentCopyVisibility
actual data class PaddingLayout internal actual constructor(actual override val value: Any) :
    MemoryLayout {
    override val layout: FMemoryLayout
        get() = value as FMemoryLayout

    actual override fun withName(name: String): PaddingLayout =
        layout.withName(name).let(::PaddingLayout)

    actual override fun withByteAlignment(alignment: Long): PaddingLayout =
        layout.withByteAlignment(alignment).let(::PaddingLayout)
}

@ConsistentCopyVisibility
actual data class StructLayout internal actual constructor(actual override val value: Any) :
    GroupLayout {
    override val layout: FStructLayout
        get() = value as FStructLayout

    actual override fun withName(name: String): StructLayout =
        layout.withName(name).let(::StructLayout)

    actual override fun withByteAlignment(alignment: Long): StructLayout =
        layout.withByteAlignment(alignment).let(::StructLayout)
}

@ConsistentCopyVisibility
actual data class UnionLayout internal actual constructor(actual override val value: Any) :
    GroupLayout {
    override val layout: FUnionLayout
        get() = value as FUnionLayout

    actual override fun withName(name: String): UnionLayout =
        layout.withName(name).let(::UnionLayout)

    actual override fun withByteAlignment(alignment: Long): UnionLayout =
        layout.withByteAlignment(alignment).let(::UnionLayout)
}

actual sealed interface ValueLayout : MemoryLayout {
    abstract override val layout: FValueLayout

    actual override fun withName(name: String): ValueLayout
    actual override fun withByteAlignment(alignment: Long): ValueLayout

    @ConsistentCopyVisibility
actual data class OfBoolean internal actual constructor(actual override val value: Any) :
        ValueLayout {
        override val layout: FValueLayout.OfBoolean get() = value as FValueLayout.OfBoolean

        actual override fun withName(name: String): OfBoolean =
            layout.withName(name).let(::OfBoolean)

        actual override fun withByteAlignment(alignment: Long): OfBoolean =
            layout.withByteAlignment(alignment).let(::OfBoolean)
    }

    @ConsistentCopyVisibility
actual data class OfByte internal actual constructor(actual override val value: Any) :
        ValueLayout {
        override val layout: FValueLayout.OfByte get() = value as FValueLayout.OfByte

        actual override fun withName(name: String): OfByte =
            layout.withName(name).let(::OfByte)

        actual override fun withByteAlignment(alignment: Long): OfByte =
            layout.withByteAlignment(alignment).let(::OfByte)
    }

    @ConsistentCopyVisibility
actual data class OfChar internal actual constructor(actual override val value: Any) :
        ValueLayout {
        override val layout: FValueLayout.OfChar get() = value as FValueLayout.OfChar

        actual override fun withName(name: String): OfChar =
            layout.withName(name).let(::OfChar)

        actual override fun withByteAlignment(alignment: Long): OfChar =
            layout.withByteAlignment(alignment).let(::OfChar)
    }

    @ConsistentCopyVisibility
actual data class OfShort internal actual constructor(actual override val value: Any) :
        ValueLayout {
        override val layout: FValueLayout.OfShort get() = value as FValueLayout.OfShort

        actual override fun withName(name: String): OfShort =
            layout.withName(name).let(::OfShort)

        actual override fun withByteAlignment(alignment: Long): OfShort =
            layout.withByteAlignment(alignment).let(::OfShort)
    }

    @ConsistentCopyVisibility
actual data class OfInt internal actual constructor(actual override val value: Any) :
        ValueLayout {
        override val layout: FValueLayout.OfInt get() = value as FValueLayout.OfInt

        actual override fun withName(name: String): OfInt =
            layout.withName(name).let(::OfInt)

        actual override fun withByteAlignment(alignment: Long): OfInt =
            layout.withByteAlignment(alignment).let(::OfInt)
    }

    @ConsistentCopyVisibility
actual data class OfLong internal actual constructor(actual override val value: Any) :
        ValueLayout {
        override val layout: FValueLayout.OfLong get() = value as FValueLayout.OfLong

        actual override fun withName(name: String): OfLong =
            layout.withName(name).let(::OfLong)

        actual override fun withByteAlignment(alignment: Long): OfLong =
            layout.withByteAlignment(alignment).let(::OfLong)
    }

    @ConsistentCopyVisibility
actual data class OfFloat internal actual constructor(actual override val value: Any) :
        ValueLayout {
        override val layout: FValueLayout.OfFloat get() = value as FValueLayout.OfFloat

        actual override fun withName(name: String): OfFloat =
            layout.withName(name).let(::OfFloat)

        actual override fun withByteAlignment(alignment: Long): OfFloat =
            layout.withByteAlignment(alignment).let(::OfFloat)
    }

    @ConsistentCopyVisibility
actual data class OfDouble internal actual constructor(actual override val value: Any) :
        ValueLayout {
        override val layout: FValueLayout.OfDouble get() = value as FValueLayout.OfDouble

        actual override fun withName(name: String): OfDouble =
            layout.withName(name).let(::OfDouble)

        actual override fun withByteAlignment(alignment: Long): OfDouble =
            layout.withByteAlignment(alignment).let(::OfDouble)
    }

    actual companion object {
        @JvmStatic
        actual val ADDRESS: AddressLayout =
            FValueLayout.ADDRESS.let(::AddressLayout)

        @JvmStatic
        actual val JAVA_BYTE: OfByte =
            FValueLayout.JAVA_BYTE.let(::OfByte)

        @JvmStatic
        actual val JAVA_BOOLEAN: OfBoolean =
            FValueLayout.JAVA_BOOLEAN.let(::OfBoolean)

        @JvmStatic
        actual val JAVA_CHAR: OfChar =
            FValueLayout.JAVA_CHAR.let(::OfChar)

        @JvmStatic
        actual val JAVA_SHORT: OfShort =
            FValueLayout.JAVA_SHORT.let(::OfShort)

        @JvmStatic
        actual val JAVA_INT: OfInt =
            FValueLayout.JAVA_INT.let(::OfInt)

        @JvmStatic
        actual val JAVA_LONG: OfLong =
            FValueLayout.JAVA_LONG.let(::OfLong)

        @JvmStatic
        actual val JAVA_FLOAT: OfFloat =
            FValueLayout.JAVA_FLOAT.let(::OfFloat)

        @JvmStatic
        actual val JAVA_DOUBLE: OfDouble =
            FValueLayout.JAVA_DOUBLE.let(::OfDouble)

        @JvmStatic
        actual val ADDRESS_UNALIGNED: AddressLayout =
            FValueLayout.ADDRESS_UNALIGNED.let(::AddressLayout)

        @JvmStatic
        actual val JAVA_CHAR_UNALIGNED: OfChar =
            FValueLayout.JAVA_CHAR_UNALIGNED.let(::OfChar)

        @JvmStatic
        actual val JAVA_SHORT_UNALIGNED: OfShort =
            FValueLayout.JAVA_SHORT_UNALIGNED.let(::OfShort)

        @JvmStatic
        actual val JAVA_INT_UNALIGNED: OfInt =
            FValueLayout.JAVA_INT_UNALIGNED.let(::OfInt)

        @JvmStatic
        actual val JAVA_LONG_UNALIGNED: OfLong =
            FValueLayout.JAVA_LONG_UNALIGNED.let(::OfLong)

        @JvmStatic
        actual val JAVA_FLOAT_UNALIGNED: OfFloat =
            FValueLayout.JAVA_FLOAT_UNALIGNED.let(::OfFloat)

        @JvmStatic
        actual val JAVA_DOUBLE_UNALIGNED: OfDouble =
            FValueLayout.JAVA_DOUBLE_UNALIGNED.let(::OfDouble)
    }
}

@ConsistentCopyVisibility
actual data class AddressLayout internal actual constructor(actual override val value: Any) :
    ValueLayout {
    override val layout: FAddressLayout get() = value as FAddressLayout

    actual override fun withName(name: String): AddressLayout =
        layout.withName(name).let(::AddressLayout)

    actual override fun withByteAlignment(alignment: Long): AddressLayout =
        layout.withByteAlignment(alignment).let(::AddressLayout)

    actual fun withTargetLayout(layout: MemoryLayout): AddressLayout =
        this.layout.withTargetLayout(layout.layout).let(::AddressLayout)
}

internal actual fun wrapLayout(inner: Any): MemoryLayout = when (inner) {
    is FStructLayout -> StructLayout(inner)
    is FUnionLayout -> UnionLayout(inner)
    is FPaddingLayout -> PaddingLayout(inner)
    is FSequenceLayout -> SequenceLayout(inner)
    is FAddressLayout -> AddressLayout(inner)
    is FValueLayout.OfBoolean -> ValueLayout.OfBoolean(inner)
    is FValueLayout.OfByte -> ValueLayout.OfByte(inner)
    is FValueLayout.OfChar -> ValueLayout.OfChar(inner)
    is FValueLayout.OfDouble -> ValueLayout.OfDouble(inner)
    is FValueLayout.OfFloat -> ValueLayout.OfFloat(inner)
    is FValueLayout.OfInt -> ValueLayout.OfInt(inner)
    is FValueLayout.OfLong -> ValueLayout.OfLong(inner)
    is FValueLayout.OfShort -> ValueLayout.OfShort(inner)
    else -> throw IllegalArgumentException("Unknown layout type: ${inner::class}")
}
