package dev.silenium.libs.foreign.ext

import dev.silenium.libs.foreign.Arena
import dev.silenium.libs.foreign.FunctionDescriptor
import dev.silenium.libs.foreign.Linker
import dev.silenium.libs.foreign.MemorySegment
import dev.silenium.libs.foreign.MethodHandles
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

fun KFunction<*>.upcallStub(
    thiz: Any,
    linker: Linker,
    descriptor: FunctionDescriptor,
    arena: Arena
): MemorySegment = linker.upcallStub(
    MethodHandles.lookup().unreflect(this.javaMethod!!).bindTo(thiz),
    descriptor,
    arena,
)
