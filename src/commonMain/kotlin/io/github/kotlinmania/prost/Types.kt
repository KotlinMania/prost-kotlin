// port-lint: source prost/src/types.rs
package io.github.kotlinmania.prost

import io.github.kotlinmania.bytes.buf.Buf
import io.github.kotlinmania.bytes.buf.BufMut
import io.github.kotlinmania.prost.encoding.BoolEncoding
import io.github.kotlinmania.prost.encoding.BytesEncoding
import io.github.kotlinmania.prost.encoding.DecodeContext
import io.github.kotlinmania.prost.encoding.DoubleEncoding
import io.github.kotlinmania.prost.encoding.FloatEncoding
import io.github.kotlinmania.prost.encoding.Int32Encoding
import io.github.kotlinmania.prost.encoding.Int64Encoding
import io.github.kotlinmania.prost.encoding.StringEncoding
import io.github.kotlinmania.prost.encoding.UInt32Encoding
import io.github.kotlinmania.prost.encoding.UInt64Encoding
import io.github.kotlinmania.prost.encoding.WireType
import io.github.kotlinmania.prost.encoding.skipField

private fun googleapisTypeUrl(packageName: String, typeName: String): String =
    "type.googleapis.com/$packageName.$typeName"

/**
 * `google.protobuf.BoolValue`
 */
data class BoolValue(
    var value: Boolean = false,
) : Message,
    Name {
    override val typeName: String = "BoolValue"
    override val packageName: String = "google.protobuf"

    override fun typeUrl(): String = googleapisTypeUrl(packageName, typeName)

    override fun encodeRaw(buf: BufMut) {
        if (value) {
            BoolEncoding.encode(1u, value, buf)
        }
    }

    override fun mergeField(
        tag: UInt,
        wireType: WireType,
        buf: Buf,
        ctx: DecodeContext,
    ): Result<Unit> =
        if (tag == 1u) {
            val res = BoolEncoding.merge(wireType, buf, ctx)
            if (res.isSuccess) {
                value = res.getOrThrow()
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        } else {
            skipField(wireType, tag, buf, ctx)
        }

    override fun encodedLen(): Int = if (value) 2 else 0

    override fun clear() {
        value = false
    }
}

/**
 * `google.protobuf.UInt32Value`
 */
data class UInt32Value(
    var value: UInt = 0u,
) : Message,
    Name {
    override val typeName: String = "UInt32Value"
    override val packageName: String = "google.protobuf"

    override fun typeUrl(): String = googleapisTypeUrl(packageName, typeName)

    override fun encodeRaw(buf: BufMut) {
        if (value != 0u) {
            UInt32Encoding.encode(1u, value, buf)
        }
    }

    override fun mergeField(
        tag: UInt,
        wireType: WireType,
        buf: Buf,
        ctx: DecodeContext,
    ): Result<Unit> =
        if (tag == 1u) {
            val res = UInt32Encoding.merge(wireType, buf, ctx)
            if (res.isSuccess) {
                value = res.getOrThrow()
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        } else {
            skipField(wireType, tag, buf, ctx)
        }

    override fun encodedLen(): Int = if (value != 0u) UInt32Encoding.encodedLen(1u, value) else 0

    override fun clear() {
        value = 0u
    }
}

/**
 * `google.protobuf.UInt64Value`
 */
data class UInt64Value(
    var value: ULong = 0uL,
) : Message,
    Name {
    override val typeName: String = "UInt64Value"
    override val packageName: String = "google.protobuf"

    override fun typeUrl(): String = googleapisTypeUrl(packageName, typeName)

    override fun encodeRaw(buf: BufMut) {
        if (value != 0uL) {
            UInt64Encoding.encode(1u, value, buf)
        }
    }

    override fun mergeField(
        tag: UInt,
        wireType: WireType,
        buf: Buf,
        ctx: DecodeContext,
    ): Result<Unit> =
        if (tag == 1u) {
            val res = UInt64Encoding.merge(wireType, buf, ctx)
            if (res.isSuccess) {
                value = res.getOrThrow()
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        } else {
            skipField(wireType, tag, buf, ctx)
        }

    override fun encodedLen(): Int = if (value != 0uL) UInt64Encoding.encodedLen(1u, value) else 0

    override fun clear() {
        value = 0uL
    }
}

/**
 * `google.protobuf.Int32Value`
 */
data class Int32Value(
    var value: Int = 0,
) : Message,
    Name {
    override val typeName: String = "Int32Value"
    override val packageName: String = "google.protobuf"

    override fun typeUrl(): String = googleapisTypeUrl(packageName, typeName)

    override fun encodeRaw(buf: BufMut) {
        if (value != 0) {
            Int32Encoding.encode(1u, value, buf)
        }
    }

    override fun mergeField(
        tag: UInt,
        wireType: WireType,
        buf: Buf,
        ctx: DecodeContext,
    ): Result<Unit> =
        if (tag == 1u) {
            val res = Int32Encoding.merge(wireType, buf, ctx)
            if (res.isSuccess) {
                value = res.getOrThrow()
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        } else {
            skipField(wireType, tag, buf, ctx)
        }

    override fun encodedLen(): Int = if (value != 0) Int32Encoding.encodedLen(1u, value) else 0

    override fun clear() {
        value = 0
    }
}

/**
 * `google.protobuf.Int64Value`
 */
data class Int64Value(
    var value: Long = 0L,
) : Message,
    Name {
    override val typeName: String = "Int64Value"
    override val packageName: String = "google.protobuf"

    override fun typeUrl(): String = googleapisTypeUrl(packageName, typeName)

    override fun encodeRaw(buf: BufMut) {
        if (value != 0L) {
            Int64Encoding.encode(1u, value, buf)
        }
    }

    override fun mergeField(
        tag: UInt,
        wireType: WireType,
        buf: Buf,
        ctx: DecodeContext,
    ): Result<Unit> =
        if (tag == 1u) {
            val res = Int64Encoding.merge(wireType, buf, ctx)
            if (res.isSuccess) {
                value = res.getOrThrow()
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        } else {
            skipField(wireType, tag, buf, ctx)
        }

    override fun encodedLen(): Int = if (value != 0L) Int64Encoding.encodedLen(1u, value) else 0

    override fun clear() {
        value = 0L
    }
}

/**
 * `google.protobuf.FloatValue`
 */
data class FloatValue(
    var value: Float = 0.0f,
) : Message,
    Name {
    override val typeName: String = "FloatValue"
    override val packageName: String = "google.protobuf"

    override fun typeUrl(): String = googleapisTypeUrl(packageName, typeName)

    override fun encodeRaw(buf: BufMut) {
        if (value != 0.0f) {
            FloatEncoding.encode(1u, value, buf)
        }
    }

    override fun mergeField(
        tag: UInt,
        wireType: WireType,
        buf: Buf,
        ctx: DecodeContext,
    ): Result<Unit> =
        if (tag == 1u) {
            val res = FloatEncoding.merge(wireType, buf, ctx)
            if (res.isSuccess) {
                value = res.getOrThrow()
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        } else {
            skipField(wireType, tag, buf, ctx)
        }

    override fun encodedLen(): Int = if (value != 0.0f) FloatEncoding.encodedLen(1u, value) else 0

    override fun clear() {
        value = 0.0f
    }
}

/**
 * `google.protobuf.DoubleValue`
 */
data class DoubleValue(
    var value: Double = 0.0,
) : Message,
    Name {
    override val typeName: String = "DoubleValue"
    override val packageName: String = "google.protobuf"

    override fun typeUrl(): String = googleapisTypeUrl(packageName, typeName)

    override fun encodeRaw(buf: BufMut) {
        if (value != 0.0) {
            DoubleEncoding.encode(1u, value, buf)
        }
    }

    override fun mergeField(
        tag: UInt,
        wireType: WireType,
        buf: Buf,
        ctx: DecodeContext,
    ): Result<Unit> =
        if (tag == 1u) {
            val res = DoubleEncoding.merge(wireType, buf, ctx)
            if (res.isSuccess) {
                value = res.getOrThrow()
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        } else {
            skipField(wireType, tag, buf, ctx)
        }

    override fun encodedLen(): Int = if (value != 0.0) DoubleEncoding.encodedLen(1u, value) else 0

    override fun clear() {
        value = 0.0
    }
}

/**
 * `google.protobuf.StringValue`
 */
data class StringValue(
    var value: String = "",
) : Message,
    Name {
    override val typeName: String = "StringValue"
    override val packageName: String = "google.protobuf"

    override fun typeUrl(): String = googleapisTypeUrl(packageName, typeName)

    override fun encodeRaw(buf: BufMut) {
        if (value.isNotEmpty()) {
            StringEncoding.encode(1u, value, buf)
        }
    }

    override fun mergeField(
        tag: UInt,
        wireType: WireType,
        buf: Buf,
        ctx: DecodeContext,
    ): Result<Unit> =
        if (tag == 1u) {
            val res = StringEncoding.merge(wireType, buf, ctx)
            if (res.isSuccess) {
                value = res.getOrThrow()
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        } else {
            skipField(wireType, tag, buf, ctx)
        }

    override fun encodedLen(): Int = if (value.isNotEmpty()) StringEncoding.encodedLen(1u, value) else 0

    override fun clear() {
        value = ""
    }
}

/**
 * `google.protobuf.BytesValue`
 */
data class BytesValue(
    var value: ByteArray = ByteArray(0),
) : Message,
    Name {
    override val typeName: String = "BytesValue"
    override val packageName: String = "google.protobuf"

    override fun typeUrl(): String = googleapisTypeUrl(packageName, typeName)

    override fun encodeRaw(buf: BufMut) {
        if (value.isNotEmpty()) {
            BytesEncoding.encode(1u, value, buf)
        }
    }

    override fun mergeField(
        tag: UInt,
        wireType: WireType,
        buf: Buf,
        ctx: DecodeContext,
    ): Result<Unit> =
        if (tag == 1u) {
            val res = BytesEncoding.merge(wireType, buf, ctx)
            if (res.isSuccess) {
                value = res.getOrThrow()
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        } else {
            skipField(wireType, tag, buf, ctx)
        }

    override fun encodedLen(): Int = if (value.isNotEmpty()) BytesEncoding.encodedLen(1u, value) else 0

    override fun clear() {
        value = ByteArray(0)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BytesValue) return false
        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int = value.contentHashCode()
}

/**
 * `google.protobuf.Empty`
 */
data object Empty : Message, Name {
    override val typeName: String = "Empty"
    override val packageName: String = "google.protobuf"

    override fun typeUrl(): String = googleapisTypeUrl(packageName, typeName)

    override fun encodeRaw(buf: BufMut) {
        buf.hashCode()
    }

    override fun mergeField(
        tag: UInt,
        wireType: WireType,
        buf: Buf,
        ctx: DecodeContext,
    ): Result<Unit> = skipField(wireType, tag, buf, ctx)

    override fun encodedLen(): Int = 0

    override fun clear() {}
}
