package dev.silenium.libs.foreign.ext.fields

import dev.silenium.libs.foreign.Arena
import dev.silenium.libs.foreign.MemorySegment
import kotlin.reflect.KProperty

interface ReadOnlyField<T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T
}

interface ReadWriteField<T> : ReadOnlyField<T> {
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
}

interface HasNative {
    val arena: Arena
    val value: MemorySegment
}

fun <T> ReadOnlyField<T?>.nonNull(): ReadOnlyField<T> = object : ReadOnlyField<T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = this@nonNull.getValue(thisRef, property)!!
}

fun <T> ReadWriteField<T?>.nonNull(): ReadWriteField<T> = object : ReadWriteField<T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = this@nonNull.getValue(thisRef, property)!!
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
        this@nonNull.setValue(thisRef, property, value)
}

fun <T, M> ReadOnlyField<T>.map(mapper: (T) -> M): ReadOnlyField<M> = object : ReadOnlyField<M> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = mapper(this@map.getValue(thisRef, property))
}

fun <T, M> ReadWriteField<T>.map(inMapper: (T) -> M, outMapper: (M) -> T): ReadWriteField<M> =
    object : ReadWriteField<M> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = inMapper(this@map.getValue(thisRef, property))
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: M) =
            this@map.setValue(thisRef, property, outMapper(value))
    }

class WriteGuardedField<T>(private val writeEnabled: () -> Boolean, private val inner: ReadWriteField<T>) :
    ReadWriteField<T> by inner {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (!writeEnabled()) {
            throw IllegalAccessException("writing to property ${property.name} not allowed")
        }
        inner.setValue(thisRef, property, value)
    }
}

fun <T> ReadWriteField<T>.writeGuard(writeEnabled: () -> Boolean): ReadWriteField<T> =
    WriteGuardedField(writeEnabled, this)
