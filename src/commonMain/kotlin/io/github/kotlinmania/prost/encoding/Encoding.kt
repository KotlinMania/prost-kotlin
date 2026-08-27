// port-lint: source encoding.rs
package io.github.kotlinmania.prost.encoding

import io.github.kotlinmania.bytes.Bytes
import io.github.kotlinmania.bytes.buf.Buf
import io.github.kotlinmania.bytes.buf.BufMut
import io.github.kotlinmania.prost.DecodeError
import io.github.kotlinmania.prost.DecodeErrorKind
import io.github.kotlinmania.prost.Message

const val RECURSION_LIMIT: UInt = 100u
const val MIN_TAG: UInt = 1u
const val MAX_TAG: UInt = 0x1FFFFFFFu

/**
 * Additional information passed to every decode/merge function.
 */
data class DecodeContext(
    internal val recurseCount: UInt = RECURSION_LIMIT,
) {
    fun enterRecursion(): DecodeContext =
        DecodeContext(if (recurseCount > 0u) recurseCount - 1u else 0u)

    fun limitReached(): Result<Unit> =
        if (recurseCount == 0u) {
            Result.failure(DecodeError(DecodeErrorKind.RecursionLimitReached))
        } else {
            Result.success(Unit)
        }
}

/**
 * Encodes a Protobuf field key, which consists of a wire type designator and
 * the field tag.
 */
fun encodeKey(tag: UInt, wireType: WireType, buf: BufMut) {
    require(tag in MIN_TAG..MAX_TAG) { "tag out of range: $tag" }
    val key = (tag shl 3) or wireType.code.toUInt()
    encodeVarint(key.toULong(), buf)
}

/**
 * Decodes a Protobuf field key, which consists of a wire type designator and
 * the field tag.
 */
fun decodeKey(buf: Buf): Result<Pair<UInt, WireType>> {
    val keyResult = decodeVarint(buf)
    if (keyResult.isFailure) {
        return Result.failure(keyResult.exceptionOrNull() ?: DecodeError(DecodeErrorKind.InvalidVarint))
    }
    val key = keyResult.getOrThrow()
    if (key > UInt.MAX_VALUE.toULong()) {
        return Result.failure(DecodeError(DecodeErrorKind.InvalidKey(key)))
    }
    val wireTypeResult = WireType.tryFrom(key and 0x07uL)
    if (wireTypeResult.isFailure) {
        return Result.failure(wireTypeResult.exceptionOrNull() ?: DecodeError(DecodeErrorKind.InvalidWireType(key)))
    }
    val wireType = wireTypeResult.getOrThrow()
    val tag = (key.toUInt() shr 3)
    if (tag < MIN_TAG) {
        return Result.failure(DecodeError(DecodeErrorKind.InvalidTag))
    }
    return Result.success(Pair(tag, wireType))
}

/**
 * Returns the width of an encoded Protobuf field key with the given tag.
 */
fun keyLen(tag: UInt): Int = encodedLenVarint((tag shl 3).toULong())

/**
 * Helper function which abstracts reading a length delimiter prefix followed
 * by decoding values until the length of bytes is exhausted.
 */
fun <T> mergeLoop(
    value: T,
    buf: Buf,
    ctx: DecodeContext,
    merge: (T, Buf, DecodeContext) -> Result<Unit>,
): Result<Unit> {
    val lenResult = decodeVarint(buf)
    if (lenResult.isFailure) {
        return Result.failure(lenResult.exceptionOrNull() ?: DecodeError(DecodeErrorKind.InvalidVarint))
    }
    val len = lenResult.getOrThrow()
    val remaining = buf.remaining()
    if (len > remaining.toULong()) {
        return Result.failure(DecodeError(DecodeErrorKind.BufferUnderflow))
    }

    val limit = remaining - len.toInt()
    while (buf.remaining() > limit) {
        val res = merge(value, buf, ctx)
        if (res.isFailure) return res
    }

    if (buf.remaining() != limit) {
        return Result.failure(DecodeError(DecodeErrorKind.DelimitedLengthExceeded))
    }
    return Result.success(Unit)
}

/**
 * Skips a field on the buffer according to its wire type.
 */
fun skipField(
    wireType: WireType,
    tag: UInt,
    buf: Buf,
    ctx: DecodeContext,
): Result<Unit> {
    val limitCheck = ctx.limitReached()
    if (limitCheck.isFailure) return limitCheck

    val len: Int =
        when (wireType) {
            WireType.Varint -> {
                val varintRes = decodeVarint(buf)
                if (varintRes.isFailure) return Result.failure(varintRes.exceptionOrNull()!!)
                0
            }
            WireType.ThirtyTwoBit -> 4
            WireType.SixtyFourBit -> 8
            WireType.LengthDelimited -> {
                val lenRes = decodeVarint(buf)
                if (lenRes.isFailure) return Result.failure(lenRes.exceptionOrNull()!!)
                val l = lenRes.getOrThrow()
                if (l > Int.MAX_VALUE.toULong()) return Result.failure(DecodeError(DecodeErrorKind.LengthDelimiterTooLarge))
                l.toInt()
            }
            WireType.StartGroup -> {
                while (true) {
                    val keyRes = decodeKey(buf)
                    if (keyRes.isFailure) return Result.failure(keyRes.exceptionOrNull()!!)
                    val (innerTag, innerWireType) = keyRes.getOrThrow()
                    if (innerWireType == WireType.EndGroup) {
                        if (innerTag != tag) {
                            return Result.failure(DecodeError(DecodeErrorKind.UnexpectedEndGroupTag))
                        }
                        break
                    }
                    val skipRes = skipField(innerWireType, innerTag, buf, ctx.enterRecursion())
                    if (skipRes.isFailure) return skipRes
                }
                0
            }
            WireType.EndGroup -> return Result.failure(DecodeError(DecodeErrorKind.UnexpectedEndGroupTag))
        }

    if (len > buf.remaining()) {
        return Result.failure(DecodeError(DecodeErrorKind.BufferUnderflow))
    }
    buf.advance(len)
    return Result.success(Unit)
}

// ---------------------------------------------------------------------------
// Primitive types encoding and decoding modules
// ---------------------------------------------------------------------------

object BoolEncoding {
    fun encode(tag: UInt, value: Boolean, buf: BufMut) {
        encodeKey(tag, WireType.Varint, buf)
        encodeVarint(if (value) 1uL else 0uL, buf)
    }

    fun merge(wireType: WireType, buf: Buf, ctx: DecodeContext): Result<Boolean> {
        val check = checkWireType(WireType.Varint, wireType)
        if (check.isFailure) return Result.failure(check.exceptionOrNull()!!)
        val res = decodeVarint(buf)
        return if (res.isSuccess) Result.success(res.getOrThrow() != 0uL) else Result.failure(res.exceptionOrNull()!!)
    }

    fun encodeRepeated(tag: UInt, values: List<Boolean>, buf: BufMut) {
        for (v in values) encode(tag, v, buf)
    }

    fun encodePacked(tag: UInt, values: List<Boolean>, buf: BufMut) {
        if (values.isEmpty()) return
        encodeKey(tag, WireType.LengthDelimited, buf)
        val len = values.size
        encodeVarint(len.toULong(), buf)
        for (v in values) encodeVarint(if (v) 1uL else 0uL, buf)
    }

    fun mergeRepeated(wireType: WireType, values: MutableList<Boolean>, buf: Buf, ctx: DecodeContext): Result<Unit> =
        if (wireType == WireType.LengthDelimited) {
            mergeLoop(values, buf, ctx) { list, b, c ->
                val res = merge(WireType.Varint, b, c)
                if (res.isSuccess) {
                    list.add(res.getOrThrow())
                    Result.success(Unit)
                } else {
                    Result.failure(res.exceptionOrNull()!!)
                }
            }
        } else {
            val res = merge(wireType, buf, ctx)
            if (res.isSuccess) {
                values.add(res.getOrThrow())
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        }

    fun encodedLen(tag: UInt, value: Boolean): Int = keyLen(tag) + encodedLenVarint(if (value) 1uL else 0uL)

    fun encodedLenRepeated(tag: UInt, values: List<Boolean>): Int = keyLen(tag) * values.size + values.size

    fun encodedLenPacked(tag: UInt, values: List<Boolean>): Int {
        if (values.isEmpty()) return 0
        val len = values.size
        return keyLen(tag) + encodedLenVarint(len.toULong()) + len
    }
}

object Int32Encoding {
    fun encode(tag: UInt, value: Int, buf: BufMut) {
        encodeKey(tag, WireType.Varint, buf)
        encodeVarint(value.toLong().toULong(), buf)
    }

    fun merge(wireType: WireType, buf: Buf, ctx: DecodeContext): Result<Int> {
        val check = checkWireType(WireType.Varint, wireType)
        if (check.isFailure) return Result.failure(check.exceptionOrNull()!!)
        val res = decodeVarint(buf)
        return if (res.isSuccess) Result.success(res.getOrThrow().toLong().toInt()) else Result.failure(res.exceptionOrNull()!!)
    }

    fun encodeRepeated(tag: UInt, values: List<Int>, buf: BufMut) {
        for (v in values) encode(tag, v, buf)
    }

    fun encodePacked(tag: UInt, values: List<Int>, buf: BufMut) {
        if (values.isEmpty()) return
        encodeKey(tag, WireType.LengthDelimited, buf)
        val len = values.sumOf { encodedLenVarint(it.toLong().toULong()) }
        encodeVarint(len.toULong(), buf)
        for (v in values) encodeVarint(v.toLong().toULong(), buf)
    }

    fun mergeRepeated(wireType: WireType, values: MutableList<Int>, buf: Buf, ctx: DecodeContext): Result<Unit> =
        if (wireType == WireType.LengthDelimited) {
            mergeLoop(values, buf, ctx) { list, b, c ->
                val res = merge(WireType.Varint, b, c)
                if (res.isSuccess) {
                    list.add(res.getOrThrow())
                    Result.success(Unit)
                } else {
                    Result.failure(res.exceptionOrNull()!!)
                }
            }
        } else {
            val res = merge(wireType, buf, ctx)
            if (res.isSuccess) {
                values.add(res.getOrThrow())
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        }

    fun encodedLen(tag: UInt, value: Int): Int = keyLen(tag) + encodedLenVarint(value.toLong().toULong())

    fun encodedLenRepeated(tag: UInt, values: List<Int>): Int = keyLen(tag) * values.size + values.sumOf { encodedLenVarint(it.toLong().toULong()) }

    fun encodedLenPacked(tag: UInt, values: List<Int>): Int {
        if (values.isEmpty()) return 0
        val len = values.sumOf { encodedLenVarint(it.toLong().toULong()) }
        return keyLen(tag) + encodedLenVarint(len.toULong()) + len
    }
}

object Int64Encoding {
    fun encode(tag: UInt, value: Long, buf: BufMut) {
        encodeKey(tag, WireType.Varint, buf)
        encodeVarint(value.toULong(), buf)
    }

    fun merge(wireType: WireType, buf: Buf, ctx: DecodeContext): Result<Long> {
        val check = checkWireType(WireType.Varint, wireType)
        if (check.isFailure) return Result.failure(check.exceptionOrNull()!!)
        val res = decodeVarint(buf)
        return if (res.isSuccess) Result.success(res.getOrThrow().toLong()) else Result.failure(res.exceptionOrNull()!!)
    }

    fun encodeRepeated(tag: UInt, values: List<Long>, buf: BufMut) {
        for (v in values) encode(tag, v, buf)
    }

    fun encodePacked(tag: UInt, values: List<Long>, buf: BufMut) {
        if (values.isEmpty()) return
        encodeKey(tag, WireType.LengthDelimited, buf)
        val len = values.sumOf { encodedLenVarint(it.toULong()) }
        encodeVarint(len.toULong(), buf)
        for (v in values) encodeVarint(v.toULong(), buf)
    }

    fun mergeRepeated(wireType: WireType, values: MutableList<Long>, buf: Buf, ctx: DecodeContext): Result<Unit> =
        if (wireType == WireType.LengthDelimited) {
            mergeLoop(values, buf, ctx) { list, b, c ->
                val res = merge(WireType.Varint, b, c)
                if (res.isSuccess) {
                    list.add(res.getOrThrow())
                    Result.success(Unit)
                } else {
                    Result.failure(res.exceptionOrNull()!!)
                }
            }
        } else {
            val res = merge(wireType, buf, ctx)
            if (res.isSuccess) {
                values.add(res.getOrThrow())
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        }

    fun encodedLen(tag: UInt, value: Long): Int = keyLen(tag) + encodedLenVarint(value.toULong())

    fun encodedLenRepeated(tag: UInt, values: List<Long>): Int = keyLen(tag) * values.size + values.sumOf { encodedLenVarint(it.toULong()) }

    fun encodedLenPacked(tag: UInt, values: List<Long>): Int {
        if (values.isEmpty()) return 0
        val len = values.sumOf { encodedLenVarint(it.toULong()) }
        return keyLen(tag) + encodedLenVarint(len.toULong()) + len
    }
}

object UInt32Encoding {
    fun encode(tag: UInt, value: UInt, buf: BufMut) {
        encodeKey(tag, WireType.Varint, buf)
        encodeVarint(value.toULong(), buf)
    }

    fun merge(wireType: WireType, buf: Buf, ctx: DecodeContext): Result<UInt> {
        val check = checkWireType(WireType.Varint, wireType)
        if (check.isFailure) return Result.failure(check.exceptionOrNull()!!)
        val res = decodeVarint(buf)
        return if (res.isSuccess) Result.success(res.getOrThrow().toUInt()) else Result.failure(res.exceptionOrNull()!!)
    }

    fun encodeRepeated(tag: UInt, values: List<UInt>, buf: BufMut) {
        for (v in values) encode(tag, v, buf)
    }

    fun encodePacked(tag: UInt, values: List<UInt>, buf: BufMut) {
        if (values.isEmpty()) return
        encodeKey(tag, WireType.LengthDelimited, buf)
        val len = values.sumOf { encodedLenVarint(it.toULong()) }
        encodeVarint(len.toULong(), buf)
        for (v in values) encodeVarint(v.toULong(), buf)
    }

    fun mergeRepeated(wireType: WireType, values: MutableList<UInt>, buf: Buf, ctx: DecodeContext): Result<Unit> =
        if (wireType == WireType.LengthDelimited) {
            mergeLoop(values, buf, ctx) { list, b, c ->
                val res = merge(WireType.Varint, b, c)
                if (res.isSuccess) {
                    list.add(res.getOrThrow())
                    Result.success(Unit)
                } else {
                    Result.failure(res.exceptionOrNull()!!)
                }
            }
        } else {
            val res = merge(wireType, buf, ctx)
            if (res.isSuccess) {
                values.add(res.getOrThrow())
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        }

    fun encodedLen(tag: UInt, value: UInt): Int = keyLen(tag) + encodedLenVarint(value.toULong())

    fun encodedLenRepeated(tag: UInt, values: List<UInt>): Int = keyLen(tag) * values.size + values.sumOf { encodedLenVarint(it.toULong()) }

    fun encodedLenPacked(tag: UInt, values: List<UInt>): Int {
        if (values.isEmpty()) return 0
        val len = values.sumOf { encodedLenVarint(it.toULong()) }
        return keyLen(tag) + encodedLenVarint(len.toULong()) + len
    }
}

object UInt64Encoding {
    fun encode(tag: UInt, value: ULong, buf: BufMut) {
        encodeKey(tag, WireType.Varint, buf)
        encodeVarint(value, buf)
    }

    fun merge(wireType: WireType, buf: Buf, ctx: DecodeContext): Result<ULong> {
        val check = checkWireType(WireType.Varint, wireType)
        if (check.isFailure) return Result.failure(check.exceptionOrNull()!!)
        return decodeVarint(buf)
    }

    fun encodeRepeated(tag: UInt, values: List<ULong>, buf: BufMut) {
        for (v in values) encode(tag, v, buf)
    }

    fun encodePacked(tag: UInt, values: List<ULong>, buf: BufMut) {
        if (values.isEmpty()) return
        encodeKey(tag, WireType.LengthDelimited, buf)
        val len = values.sumOf { encodedLenVarint(it) }
        encodeVarint(len.toULong(), buf)
        for (v in values) encodeVarint(v, buf)
    }

    fun mergeRepeated(wireType: WireType, values: MutableList<ULong>, buf: Buf, ctx: DecodeContext): Result<Unit> =
        if (wireType == WireType.LengthDelimited) {
            mergeLoop(values, buf, ctx) { list, b, c ->
                val res = merge(WireType.Varint, b, c)
                if (res.isSuccess) {
                    list.add(res.getOrThrow())
                    Result.success(Unit)
                } else {
                    Result.failure(res.exceptionOrNull()!!)
                }
            }
        } else {
            val res = merge(wireType, buf, ctx)
            if (res.isSuccess) {
                values.add(res.getOrThrow())
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        }

    fun encodedLen(tag: UInt, value: ULong): Int = keyLen(tag) + encodedLenVarint(value)

    fun encodedLenRepeated(tag: UInt, values: List<ULong>): Int = keyLen(tag) * values.size + values.sumOf { encodedLenVarint(it) }

    fun encodedLenPacked(tag: UInt, values: List<ULong>): Int {
        if (values.isEmpty()) return 0
        val len = values.sumOf { encodedLenVarint(it) }
        return keyLen(tag) + encodedLenVarint(len.toULong()) + len
    }
}

object SInt32Encoding {
    private fun toZigzag(value: Int): ULong =
        (((value shl 1) xor (value shr 31)).toUInt()).toULong()

    private fun fromZigzag(value: ULong): Int {
        val v = value.toUInt()
        return ((v shr 1).toInt()) xor (-((v and 1u).toInt()))
    }

    fun encode(tag: UInt, value: Int, buf: BufMut) {
        encodeKey(tag, WireType.Varint, buf)
        encodeVarint(toZigzag(value), buf)
    }

    fun merge(wireType: WireType, buf: Buf, ctx: DecodeContext): Result<Int> {
        val check = checkWireType(WireType.Varint, wireType)
        if (check.isFailure) return Result.failure(check.exceptionOrNull()!!)
        val res = decodeVarint(buf)
        return if (res.isSuccess) Result.success(fromZigzag(res.getOrThrow())) else Result.failure(res.exceptionOrNull()!!)
    }

    fun encodeRepeated(tag: UInt, values: List<Int>, buf: BufMut) {
        for (v in values) encode(tag, v, buf)
    }

    fun encodePacked(tag: UInt, values: List<Int>, buf: BufMut) {
        if (values.isEmpty()) return
        encodeKey(tag, WireType.LengthDelimited, buf)
        val len = values.sumOf { encodedLenVarint(toZigzag(it)) }
        encodeVarint(len.toULong(), buf)
        for (v in values) encodeVarint(toZigzag(v), buf)
    }

    fun mergeRepeated(wireType: WireType, values: MutableList<Int>, buf: Buf, ctx: DecodeContext): Result<Unit> =
        if (wireType == WireType.LengthDelimited) {
            mergeLoop(values, buf, ctx) { list, b, c ->
                val res = merge(WireType.Varint, b, c)
                if (res.isSuccess) {
                    list.add(res.getOrThrow())
                    Result.success(Unit)
                } else {
                    Result.failure(res.exceptionOrNull()!!)
                }
            }
        } else {
            val res = merge(wireType, buf, ctx)
            if (res.isSuccess) {
                values.add(res.getOrThrow())
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        }

    fun encodedLen(tag: UInt, value: Int): Int = keyLen(tag) + encodedLenVarint(toZigzag(value))

    fun encodedLenRepeated(tag: UInt, values: List<Int>): Int = keyLen(tag) * values.size + values.sumOf { encodedLenVarint(toZigzag(it)) }

    fun encodedLenPacked(tag: UInt, values: List<Int>): Int {
        if (values.isEmpty()) return 0
        val len = values.sumOf { encodedLenVarint(toZigzag(it)) }
        return keyLen(tag) + encodedLenVarint(len.toULong()) + len
    }
}

object SInt64Encoding {
    private fun toZigzag(value: Long): ULong =
        ((value shl 1) xor (value shr 63)).toULong()

    private fun fromZigzag(value: ULong): Long {
        val v = value.toLong()
        return (v ushr 1) xor -(v and 1L)
    }

    fun encode(tag: UInt, value: Long, buf: BufMut) {
        encodeKey(tag, WireType.Varint, buf)
        encodeVarint(toZigzag(value), buf)
    }

    fun merge(wireType: WireType, buf: Buf, ctx: DecodeContext): Result<Long> {
        val check = checkWireType(WireType.Varint, wireType)
        if (check.isFailure) return Result.failure(check.exceptionOrNull()!!)
        val res = decodeVarint(buf)
        return if (res.isSuccess) Result.success(fromZigzag(res.getOrThrow())) else Result.failure(res.exceptionOrNull()!!)
    }

    fun encodeRepeated(tag: UInt, values: List<Long>, buf: BufMut) {
        for (v in values) encode(tag, v, buf)
    }

    fun encodePacked(tag: UInt, values: List<Long>, buf: BufMut) {
        if (values.isEmpty()) return
        encodeKey(tag, WireType.LengthDelimited, buf)
        val len = values.sumOf { encodedLenVarint(toZigzag(it)) }
        encodeVarint(len.toULong(), buf)
        for (v in values) encodeVarint(toZigzag(v), buf)
    }

    fun mergeRepeated(wireType: WireType, values: MutableList<Long>, buf: Buf, ctx: DecodeContext): Result<Unit> =
        if (wireType == WireType.LengthDelimited) {
            mergeLoop(values, buf, ctx) { list, b, c ->
                val res = merge(WireType.Varint, b, c)
                if (res.isSuccess) {
                    list.add(res.getOrThrow())
                    Result.success(Unit)
                } else {
                    Result.failure(res.exceptionOrNull()!!)
                }
            }
        } else {
            val res = merge(wireType, buf, ctx)
            if (res.isSuccess) {
                values.add(res.getOrThrow())
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        }

    fun encodedLen(tag: UInt, value: Long): Int = keyLen(tag) + encodedLenVarint(toZigzag(value))

    fun encodedLenRepeated(tag: UInt, values: List<Long>): Int = keyLen(tag) * values.size + values.sumOf { encodedLenVarint(toZigzag(it)) }

    fun encodedLenPacked(tag: UInt, values: List<Long>): Int {
        if (values.isEmpty()) return 0
        val len = values.sumOf { encodedLenVarint(toZigzag(it)) }
        return keyLen(tag) + encodedLenVarint(len.toULong()) + len
    }
}

object Fixed32Encoding {
    fun encode(tag: UInt, value: UInt, buf: BufMut) {
        encodeKey(tag, WireType.ThirtyTwoBit, buf)
        buf.putU32Le(value)
    }

    fun merge(wireType: WireType, buf: Buf, ctx: DecodeContext): Result<UInt> {
        val check = checkWireType(WireType.ThirtyTwoBit, wireType)
        if (check.isFailure) return Result.failure(check.exceptionOrNull()!!)
        if (buf.remaining() < 4) return Result.failure(DecodeError(DecodeErrorKind.BufferUnderflow))
        return Result.success(buf.getU32Le())
    }

    fun encodeRepeated(tag: UInt, values: List<UInt>, buf: BufMut) {
        for (v in values) encode(tag, v, buf)
    }

    fun encodePacked(tag: UInt, values: List<UInt>, buf: BufMut) {
        if (values.isEmpty()) return
        encodeKey(tag, WireType.LengthDelimited, buf)
        val len = values.size * 4
        encodeVarint(len.toULong(), buf)
        for (v in values) buf.putU32Le(v)
    }

    fun mergeRepeated(wireType: WireType, values: MutableList<UInt>, buf: Buf, ctx: DecodeContext): Result<Unit> =
        if (wireType == WireType.LengthDelimited) {
            mergeLoop(values, buf, ctx) { list, b, c ->
                val res = merge(WireType.ThirtyTwoBit, b, c)
                if (res.isSuccess) {
                    list.add(res.getOrThrow())
                    Result.success(Unit)
                } else {
                    Result.failure(res.exceptionOrNull()!!)
                }
            }
        } else {
            val res = merge(wireType, buf, ctx)
            if (res.isSuccess) {
                values.add(res.getOrThrow())
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        }

    fun encodedLen(tag: UInt, value: UInt): Int = keyLen(tag) + 4

    fun encodedLenRepeated(tag: UInt, values: List<UInt>): Int = (keyLen(tag) + 4) * values.size

    fun encodedLenPacked(tag: UInt, values: List<UInt>): Int {
        if (values.isEmpty()) return 0
        val len = 4 * values.size
        return keyLen(tag) + encodedLenVarint(len.toULong()) + len
    }
}

object Fixed64Encoding {
    fun encode(tag: UInt, value: ULong, buf: BufMut) {
        encodeKey(tag, WireType.SixtyFourBit, buf)
        buf.putU64Le(value)
    }

    fun merge(wireType: WireType, buf: Buf, ctx: DecodeContext): Result<ULong> {
        val check = checkWireType(WireType.SixtyFourBit, wireType)
        if (check.isFailure) return Result.failure(check.exceptionOrNull()!!)
        if (buf.remaining() < 8) return Result.failure(DecodeError(DecodeErrorKind.BufferUnderflow))
        return Result.success(buf.getU64Le())
    }

    fun encodeRepeated(tag: UInt, values: List<ULong>, buf: BufMut) {
        for (v in values) encode(tag, v, buf)
    }

    fun encodePacked(tag: UInt, values: List<ULong>, buf: BufMut) {
        if (values.isEmpty()) return
        encodeKey(tag, WireType.LengthDelimited, buf)
        val len = values.size * 8
        encodeVarint(len.toULong(), buf)
        for (v in values) buf.putU64Le(v)
    }

    fun mergeRepeated(wireType: WireType, values: MutableList<ULong>, buf: Buf, ctx: DecodeContext): Result<Unit> =
        if (wireType == WireType.LengthDelimited) {
            mergeLoop(values, buf, ctx) { list, b, c ->
                val res = merge(WireType.SixtyFourBit, b, c)
                if (res.isSuccess) {
                    list.add(res.getOrThrow())
                    Result.success(Unit)
                } else {
                    Result.failure(res.exceptionOrNull()!!)
                }
            }
        } else {
            val res = merge(wireType, buf, ctx)
            if (res.isSuccess) {
                values.add(res.getOrThrow())
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        }

    fun encodedLen(tag: UInt, value: ULong): Int = keyLen(tag) + 8

    fun encodedLenRepeated(tag: UInt, values: List<ULong>): Int = (keyLen(tag) + 8) * values.size

    fun encodedLenPacked(tag: UInt, values: List<ULong>): Int {
        if (values.isEmpty()) return 0
        val len = 8 * values.size
        return keyLen(tag) + encodedLenVarint(len.toULong()) + len
    }
}

object SFixed32Encoding {
    fun encode(tag: UInt, value: Int, buf: BufMut) {
        encodeKey(tag, WireType.ThirtyTwoBit, buf)
        buf.putI32Le(value)
    }

    fun merge(wireType: WireType, buf: Buf, ctx: DecodeContext): Result<Int> {
        val check = checkWireType(WireType.ThirtyTwoBit, wireType)
        if (check.isFailure) return Result.failure(check.exceptionOrNull()!!)
        if (buf.remaining() < 4) return Result.failure(DecodeError(DecodeErrorKind.BufferUnderflow))
        return Result.success(buf.getI32Le())
    }

    fun encodeRepeated(tag: UInt, values: List<Int>, buf: BufMut) {
        for (v in values) encode(tag, v, buf)
    }

    fun encodePacked(tag: UInt, values: List<Int>, buf: BufMut) {
        if (values.isEmpty()) return
        encodeKey(tag, WireType.LengthDelimited, buf)
        val len = values.size * 4
        encodeVarint(len.toULong(), buf)
        for (v in values) buf.putI32Le(v)
    }

    fun mergeRepeated(wireType: WireType, values: MutableList<Int>, buf: Buf, ctx: DecodeContext): Result<Unit> =
        if (wireType == WireType.LengthDelimited) {
            mergeLoop(values, buf, ctx) { list, b, c ->
                val res = merge(WireType.ThirtyTwoBit, b, c)
                if (res.isSuccess) {
                    list.add(res.getOrThrow())
                    Result.success(Unit)
                } else {
                    Result.failure(res.exceptionOrNull()!!)
                }
            }
        } else {
            val res = merge(wireType, buf, ctx)
            if (res.isSuccess) {
                values.add(res.getOrThrow())
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        }

    fun encodedLen(tag: UInt, value: Int): Int = keyLen(tag) + 4

    fun encodedLenRepeated(tag: UInt, values: List<Int>): Int = (keyLen(tag) + 4) * values.size

    fun encodedLenPacked(tag: UInt, values: List<Int>): Int {
        if (values.isEmpty()) return 0
        val len = 4 * values.size
        return keyLen(tag) + encodedLenVarint(len.toULong()) + len
    }
}

object SFixed64Encoding {
    fun encode(tag: UInt, value: Long, buf: BufMut) {
        encodeKey(tag, WireType.SixtyFourBit, buf)
        buf.putI64Le(value)
    }

    fun merge(wireType: WireType, buf: Buf, ctx: DecodeContext): Result<Long> {
        val check = checkWireType(WireType.SixtyFourBit, wireType)
        if (check.isFailure) return Result.failure(check.exceptionOrNull()!!)
        if (buf.remaining() < 8) return Result.failure(DecodeError(DecodeErrorKind.BufferUnderflow))
        return Result.success(buf.getI64Le())
    }

    fun encodeRepeated(tag: UInt, values: List<Long>, buf: BufMut) {
        for (v in values) encode(tag, v, buf)
    }

    fun encodePacked(tag: UInt, values: List<Long>, buf: BufMut) {
        if (values.isEmpty()) return
        encodeKey(tag, WireType.LengthDelimited, buf)
        val len = values.size * 8
        encodeVarint(len.toULong(), buf)
        for (v in values) buf.putI64Le(v)
    }

    fun mergeRepeated(wireType: WireType, values: MutableList<Long>, buf: Buf, ctx: DecodeContext): Result<Unit> =
        if (wireType == WireType.LengthDelimited) {
            mergeLoop(values, buf, ctx) { list, b, c ->
                val res = merge(WireType.SixtyFourBit, b, c)
                if (res.isSuccess) {
                    list.add(res.getOrThrow())
                    Result.success(Unit)
                } else {
                    Result.failure(res.exceptionOrNull()!!)
                }
            }
        } else {
            val res = merge(wireType, buf, ctx)
            if (res.isSuccess) {
                values.add(res.getOrThrow())
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        }

    fun encodedLen(tag: UInt, value: Long): Int = keyLen(tag) + 8

    fun encodedLenRepeated(tag: UInt, values: List<Long>): Int = (keyLen(tag) + 8) * values.size

    fun encodedLenPacked(tag: UInt, values: List<Long>): Int {
        if (values.isEmpty()) return 0
        val len = 8 * values.size
        return keyLen(tag) + encodedLenVarint(len.toULong()) + len
    }
}

object FloatEncoding {
    fun encode(tag: UInt, value: Float, buf: BufMut) {
        encodeKey(tag, WireType.ThirtyTwoBit, buf)
        buf.putF32Le(value)
    }

    fun merge(wireType: WireType, buf: Buf, ctx: DecodeContext): Result<Float> {
        val check = checkWireType(WireType.ThirtyTwoBit, wireType)
        if (check.isFailure) return Result.failure(check.exceptionOrNull()!!)
        if (buf.remaining() < 4) return Result.failure(DecodeError(DecodeErrorKind.BufferUnderflow))
        return Result.success(buf.getF32Le())
    }

    fun encodeRepeated(tag: UInt, values: List<Float>, buf: BufMut) {
        for (v in values) encode(tag, v, buf)
    }

    fun encodePacked(tag: UInt, values: List<Float>, buf: BufMut) {
        if (values.isEmpty()) return
        encodeKey(tag, WireType.LengthDelimited, buf)
        val len = values.size * 4
        encodeVarint(len.toULong(), buf)
        for (v in values) buf.putF32Le(v)
    }

    fun mergeRepeated(wireType: WireType, values: MutableList<Float>, buf: Buf, ctx: DecodeContext): Result<Unit> =
        if (wireType == WireType.LengthDelimited) {
            mergeLoop(values, buf, ctx) { list, b, c ->
                val res = merge(WireType.ThirtyTwoBit, b, c)
                if (res.isSuccess) {
                    list.add(res.getOrThrow())
                    Result.success(Unit)
                } else {
                    Result.failure(res.exceptionOrNull()!!)
                }
            }
        } else {
            val res = merge(wireType, buf, ctx)
            if (res.isSuccess) {
                values.add(res.getOrThrow())
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        }

    fun encodedLen(tag: UInt, value: Float): Int = keyLen(tag) + 4

    fun encodedLenRepeated(tag: UInt, values: List<Float>): Int = (keyLen(tag) + 4) * values.size

    fun encodedLenPacked(tag: UInt, values: List<Float>): Int {
        if (values.isEmpty()) return 0
        val len = 4 * values.size
        return keyLen(tag) + encodedLenVarint(len.toULong()) + len
    }
}

object DoubleEncoding {
    fun encode(tag: UInt, value: Double, buf: BufMut) {
        encodeKey(tag, WireType.SixtyFourBit, buf)
        buf.putF64Le(value)
    }

    fun merge(wireType: WireType, buf: Buf, ctx: DecodeContext): Result<Double> {
        val check = checkWireType(WireType.SixtyFourBit, wireType)
        if (check.isFailure) return Result.failure(check.exceptionOrNull()!!)
        if (buf.remaining() < 8) return Result.failure(DecodeError(DecodeErrorKind.BufferUnderflow))
        return Result.success(buf.getF64Le())
    }

    fun encodeRepeated(tag: UInt, values: List<Double>, buf: BufMut) {
        for (v in values) encode(tag, v, buf)
    }

    fun encodePacked(tag: UInt, values: List<Double>, buf: BufMut) {
        if (values.isEmpty()) return
        encodeKey(tag, WireType.LengthDelimited, buf)
        val len = values.size * 8
        encodeVarint(len.toULong(), buf)
        for (v in values) buf.putF64Le(v)
    }

    fun mergeRepeated(wireType: WireType, values: MutableList<Double>, buf: Buf, ctx: DecodeContext): Result<Unit> =
        if (wireType == WireType.LengthDelimited) {
            mergeLoop(values, buf, ctx) { list, b, c ->
                val res = merge(WireType.SixtyFourBit, b, c)
                if (res.isSuccess) {
                    list.add(res.getOrThrow())
                    Result.success(Unit)
                } else {
                    Result.failure(res.exceptionOrNull()!!)
                }
            }
        } else {
            val res = merge(wireType, buf, ctx)
            if (res.isSuccess) {
                values.add(res.getOrThrow())
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull()!!)
            }
        }

    fun encodedLen(tag: UInt, value: Double): Int = keyLen(tag) + 8

    fun encodedLenRepeated(tag: UInt, values: List<Double>): Int = (keyLen(tag) + 8) * values.size

    fun encodedLenPacked(tag: UInt, values: List<Double>): Int {
        if (values.isEmpty()) return 0
        val len = 8 * values.size
        return keyLen(tag) + encodedLenVarint(len.toULong()) + len
    }
}

private fun isValidUtf8(bytes: ByteArray): Boolean {
    var i = 0
    val n = bytes.size
    while (i < n) {
        val b0 = bytes[i].toInt() and 0xFF
        if (b0 < 0x80) {
            i++
        } else if (b0 in 0xC2..0xDF) {
            if (i + 1 >= n) return false
            val b1 = bytes[i + 1].toInt() and 0xFF
            if (b1 !in 0x80..0xBF) return false
            i += 2
        } else if (b0 == 0xE0) {
            if (i + 2 >= n) return false
            val b1 = bytes[i + 1].toInt() and 0xFF
            val b2 = bytes[i + 2].toInt() and 0xFF
            if (b1 !in 0xA0..0xBF || b2 !in 0x80..0xBF) return false
            i += 3
        } else if (b0 in 0xE1..0xEC || b0 in 0xEE..0xEF) {
            if (i + 2 >= n) return false
            val b1 = bytes[i + 1].toInt() and 0xFF
            val b2 = bytes[i + 2].toInt() and 0xFF
            if (b1 !in 0x80..0xBF || b2 !in 0x80..0xBF) return false
            i += 3
        } else if (b0 == 0xED) {
            if (i + 2 >= n) return false
            val b1 = bytes[i + 1].toInt() and 0xFF
            val b2 = bytes[i + 2].toInt() and 0xFF
            if (b1 !in 0x80..0x9F || b2 !in 0x80..0xBF) return false
            i += 3
        } else if (b0 == 0xF0) {
            if (i + 3 >= n) return false
            val b1 = bytes[i + 1].toInt() and 0xFF
            val b2 = bytes[i + 2].toInt() and 0xFF
            val b3 = bytes[i + 3].toInt() and 0xFF
            if (b1 !in 0x90..0xBF || b2 !in 0x80..0xBF || b3 !in 0x80..0xBF) return false
            i += 4
        } else if (b0 in 0xF1..0xF3) {
            if (i + 3 >= n) return false
            val b1 = bytes[i + 1].toInt() and 0xFF
            val b2 = bytes[i + 2].toInt() and 0xFF
            val b3 = bytes[i + 3].toInt() and 0xFF
            if (b1 !in 0x80..0xBF || b2 !in 0x80..0xBF || b3 !in 0x80..0xBF) return false
            i += 4
        } else if (b0 == 0xF4) {
            if (i + 3 >= n) return false
            val b1 = bytes[i + 1].toInt() and 0xFF
            val b2 = bytes[i + 2].toInt() and 0xFF
            val b3 = bytes[i + 3].toInt() and 0xFF
            if (b1 !in 0x80..0x8F || b2 !in 0x80..0xBF || b3 !in 0x80..0xBF) return false
            i += 4
        } else {
            return false
        }
    }
    return true
}

object StringEncoding {
    fun encode(tag: UInt, value: String, buf: BufMut) {
        val bytes = value.encodeToByteArray()
        encodeKey(tag, WireType.LengthDelimited, buf)
        encodeVarint(bytes.size.toULong(), buf)
        buf.putSlice(bytes)
    }

    fun merge(wireType: WireType, buf: Buf, ctx: DecodeContext): Result<String> {
        val check = checkWireType(WireType.LengthDelimited, wireType)
        if (check.isFailure) return Result.failure(check.exceptionOrNull()!!)
        val lenRes = decodeVarint(buf)
        if (lenRes.isFailure) return Result.failure(lenRes.exceptionOrNull()!!)
        val len = lenRes.getOrThrow()
        if (len > buf.remaining().toULong()) return Result.failure(DecodeError(DecodeErrorKind.BufferUnderflow))
        val lenInt = len.toInt()
        val slice = ByteArray(lenInt)
        buf.copyToSlice(slice)
        if (!isValidUtf8(slice)) {
            return Result.failure(DecodeError(DecodeErrorKind.InvalidString))
        }
        return try {
            Result.success(slice.decodeToString())
        } catch (e: Exception) {
            Result.failure(DecodeError(DecodeErrorKind.InvalidString))
        }
    }

    fun encodeRepeated(tag: UInt, values: List<String>, buf: BufMut) {
        for (v in values) encode(tag, v, buf)
    }

    fun mergeRepeated(wireType: WireType, values: MutableList<String>, buf: Buf, ctx: DecodeContext): Result<Unit> {
        val res = merge(wireType, buf, ctx)
        return if (res.isSuccess) {
            values.add(res.getOrThrow())
            Result.success(Unit)
        } else {
            Result.failure(res.exceptionOrNull()!!)
        }
    }

    fun encodedLen(tag: UInt, value: String): Int {
        val len = value.encodeToByteArray().size
        return keyLen(tag) + encodedLenVarint(len.toULong()) + len
    }

    fun encodedLenRepeated(tag: UInt, values: List<String>): Int =
        values.sumOf { encodedLen(tag, it) }
}

object BytesEncoding {
    fun encode(tag: UInt, value: ByteArray, buf: BufMut) {
        encodeKey(tag, WireType.LengthDelimited, buf)
        encodeVarint(value.size.toULong(), buf)
        buf.putSlice(value)
    }

    fun encode(tag: UInt, value: Bytes, buf: BufMut) {
        encodeKey(tag, WireType.LengthDelimited, buf)
        encodeVarint(value.len().toULong(), buf)
        buf.putSlice(value.asSlice())
    }

    fun merge(wireType: WireType, buf: Buf, ctx: DecodeContext): Result<ByteArray> {
        val check = checkWireType(WireType.LengthDelimited, wireType)
        if (check.isFailure) return Result.failure(check.exceptionOrNull()!!)
        val lenRes = decodeVarint(buf)
        if (lenRes.isFailure) return Result.failure(lenRes.exceptionOrNull()!!)
        val len = lenRes.getOrThrow()
        if (len > buf.remaining().toULong()) return Result.failure(DecodeError(DecodeErrorKind.BufferUnderflow))
        val slice = ByteArray(len.toInt())
        buf.copyToSlice(slice)
        return Result.success(slice)
    }

    fun mergeBytes(wireType: WireType, buf: Buf, ctx: DecodeContext): Result<Bytes> {
        val check = checkWireType(WireType.LengthDelimited, wireType)
        if (check.isFailure) return Result.failure(check.exceptionOrNull()!!)
        val lenRes = decodeVarint(buf)
        if (lenRes.isFailure) return Result.failure(lenRes.exceptionOrNull()!!)
        val len = lenRes.getOrThrow()
        if (len > buf.remaining().toULong()) return Result.failure(DecodeError(DecodeErrorKind.BufferUnderflow))
        return Result.success(buf.copyToBytes(len.toInt()))
    }

    fun encodeRepeated(tag: UInt, values: List<ByteArray>, buf: BufMut) {
        for (v in values) encode(tag, v, buf)
    }

    fun mergeRepeated(wireType: WireType, values: MutableList<ByteArray>, buf: Buf, ctx: DecodeContext): Result<Unit> {
        val res = merge(wireType, buf, ctx)
        return if (res.isSuccess) {
            values.add(res.getOrThrow())
            Result.success(Unit)
        } else {
            Result.failure(res.exceptionOrNull()!!)
        }
    }

    fun encodedLen(tag: UInt, value: ByteArray): Int =
        keyLen(tag) + encodedLenVarint(value.size.toULong()) + value.size

    fun encodedLen(tag: UInt, value: Bytes): Int =
        keyLen(tag) + encodedLenVarint(value.len().toULong()) + value.len()

    fun encodedLenRepeated(tag: UInt, values: List<ByteArray>): Int =
        values.sumOf { encodedLen(tag, it) }
}

object MessageEncoding {
    fun <M : Message> encode(tag: UInt, msg: M, buf: BufMut) {
        encodeKey(tag, WireType.LengthDelimited, buf)
        encodeVarint(msg.encodedLen().toULong(), buf)
        msg.encodeRaw(buf)
    }

    fun <M : Message> merge(
        wireType: WireType,
        msg: M,
        buf: Buf,
        ctx: DecodeContext,
    ): Result<Unit> {
        val check = checkWireType(WireType.LengthDelimited, wireType)
        if (check.isFailure) return check
        val limitCheck = ctx.limitReached()
        if (limitCheck.isFailure) return limitCheck
        return mergeLoop(msg, buf, ctx.enterRecursion()) { m, b, c ->
            val keyRes = decodeKey(b)
            if (keyRes.isFailure) return@mergeLoop Result.failure(keyRes.exceptionOrNull()!!)
            val (tag, wt) = keyRes.getOrThrow()
            m.mergeField(tag, wt, b, c)
        }
    }

    fun <M : Message> encodeRepeated(tag: UInt, messages: List<M>, buf: BufMut) {
        for (msg in messages) encode(tag, msg, buf)
    }

    fun <M : Message> mergeRepeated(
        wireType: WireType,
        messages: MutableList<M>,
        factory: () -> M,
        buf: Buf,
        ctx: DecodeContext,
    ): Result<Unit> {
        val check = checkWireType(WireType.LengthDelimited, wireType)
        if (check.isFailure) return check
        val msg = factory()
        val res = merge(WireType.LengthDelimited, msg, buf, ctx)
        if (res.isSuccess) {
            messages.add(msg)
            return Result.success(Unit)
        }
        return res
    }

    fun <M : Message> encodedLen(tag: UInt, msg: M): Int {
        val len = msg.encodedLen()
        return keyLen(tag) + encodedLenVarint(len.toULong()) + len
    }

    fun <M : Message> encodedLenRepeated(tag: UInt, messages: List<M>): Int =
        messages.sumOf { encodedLen(tag, it) }
}

object GroupEncoding {
    fun <M : Message> encode(tag: UInt, msg: M, buf: BufMut) {
        encodeKey(tag, WireType.StartGroup, buf)
        msg.encodeRaw(buf)
        encodeKey(tag, WireType.EndGroup, buf)
    }

    fun <M : Message> merge(
        tag: UInt,
        wireType: WireType,
        msg: M,
        buf: Buf,
        ctx: DecodeContext,
    ): Result<Unit> {
        val check = checkWireType(WireType.StartGroup, wireType)
        if (check.isFailure) return check
        val limitCheck = ctx.limitReached()
        if (limitCheck.isFailure) return limitCheck

        while (true) {
            val keyRes = decodeKey(buf)
            if (keyRes.isFailure) return Result.failure(keyRes.exceptionOrNull()!!)
            val (fieldTag, fieldWireType) = keyRes.getOrThrow()
            if (fieldWireType == WireType.EndGroup) {
                if (fieldTag != tag) {
                    return Result.failure(DecodeError(DecodeErrorKind.UnexpectedEndGroupTag))
                }
                return Result.success(Unit)
            }
            val mergeRes = msg.mergeField(fieldTag, fieldWireType, buf, ctx.enterRecursion())
            if (mergeRes.isFailure) return mergeRes
        }
    }

    fun <M : Message> encodeRepeated(tag: UInt, messages: List<M>, buf: BufMut) {
        for (msg in messages) encode(tag, msg, buf)
    }

    fun <M : Message> mergeRepeated(
        tag: UInt,
        wireType: WireType,
        messages: MutableList<M>,
        factory: () -> M,
        buf: Buf,
        ctx: DecodeContext,
    ): Result<Unit> {
        val check = checkWireType(WireType.StartGroup, wireType)
        if (check.isFailure) return check
        val msg = factory()
        val res = merge(tag, WireType.StartGroup, msg, buf, ctx)
        if (res.isSuccess) {
            messages.add(msg)
            return Result.success(Unit)
        }
        return res
    }

    fun <M : Message> encodedLen(tag: UInt, msg: M): Int =
        2 * keyLen(tag) + msg.encodedLen()

    fun <M : Message> encodedLenRepeated(tag: UInt, messages: List<M>): Int =
        2 * keyLen(tag) * messages.size + messages.sumOf { it.encodedLen() }
}

object MapEncoding {
    fun <K, V> encode(
        keyEncode: (UInt, K, BufMut) -> Unit,
        keyEncodedLen: (UInt, K) -> Int,
        valEncode: (UInt, V, BufMut) -> Unit,
        valEncodedLen: (UInt, V) -> Int,
        valDefault: V?,
        tag: UInt,
        values: Map<K, V>,
        buf: BufMut,
    ) {
        for ((key, value) in values) {
            val skipKey = key == null
            val skipVal = value == valDefault
            val len =
                (if (skipKey) 0 else keyEncodedLen(1u, key)) +
                    (if (skipVal) 0 else valEncodedLen(2u, value))

            encodeKey(tag, WireType.LengthDelimited, buf)
            encodeVarint(len.toULong(), buf)
            if (!skipKey) {
                keyEncode(1u, key, buf)
            }
            if (!skipVal && value != null) {
                valEncode(2u, value, buf)
            }
        }
    }

    fun <K, V> merge(
        keyMerge: (WireType, Buf, DecodeContext) -> Result<K>,
        valMerge: (WireType, Buf, DecodeContext) -> Result<V>,
        defaultKey: K,
        defaultVal: V,
        values: MutableMap<K, V>,
        buf: Buf,
        ctx: DecodeContext,
    ): Result<Unit> {
        val limitCheck = ctx.limitReached()
        if (limitCheck.isFailure) return limitCheck

        var currentKey = defaultKey
        var currentVal = defaultVal

        val loopRes =
            mergeLoop(Unit, buf, ctx.enterRecursion()) { _, b, c ->
                val keyRes = decodeKey(b)
                if (keyRes.isFailure) return@mergeLoop Result.failure(keyRes.exceptionOrNull()!!)
                val (tag, wireType) = keyRes.getOrThrow()
                when (tag) {
                    1u -> {
                        val kRes = keyMerge(wireType, b, c)
                        if (kRes.isFailure) return@mergeLoop Result.failure(kRes.exceptionOrNull()!!)
                        currentKey = kRes.getOrThrow()
                        Result.success(Unit)
                    }
                    2u -> {
                        val vRes = valMerge(wireType, b, c)
                        if (vRes.isFailure) return@mergeLoop Result.failure(vRes.exceptionOrNull()!!)
                        currentVal = vRes.getOrThrow()
                        Result.success(Unit)
                    }
                    else -> skipField(wireType, tag, b, c)
                }
            }

        if (loopRes.isSuccess) {
            values[currentKey] = currentVal
            return Result.success(Unit)
        }
        return loopRes
    }

    fun <K, V> encodedLen(
        keyEncodedLen: (UInt, K) -> Int,
        valEncodedLen: (UInt, V) -> Int,
        valDefault: V?,
        tag: UInt,
        values: Map<K, V>,
    ): Int {
        var total = 0
        for ((key, value) in values) {
            val len =
                (if (key == null) 0 else keyEncodedLen(1u, key)) +
                    (if (value == valDefault || value == null) 0 else valEncodedLen(2u, value))
            total += keyLen(tag) + encodedLenVarint(len.toULong()) + len
        }
        return total
    }
}
