// port-lint: source prost/src/encoding/varint.rs
package io.github.kotlinmania.prost.encoding

import io.github.kotlinmania.bytes.buf.Buf
import io.github.kotlinmania.bytes.buf.BufMut
import io.github.kotlinmania.prost.DecodeError
import io.github.kotlinmania.prost.DecodeErrorKind
import kotlin.math.min

/**
 * Encodes an integer value into LEB128 variable length format, and writes it to the buffer.
 * The buffer must have enough remaining space (maximum 10 bytes).
 */
fun encodeVarint(value: ULong, buf: BufMut) {
    var v = value
    for (i in 0 until 10) {
        if (v < 0x80uL) {
            buf.putU8(v.toUByte())
            break
        } else {
            buf.putU8(((v and 0x7FuL) or 0x80uL).toUByte())
            v = v shr 7
        }
    }
}

/**
 * Returns the encoded length of the value in LEB128 variable length format.
 * The returned value will be between 1 and 10, inclusive.
 */
fun encodedLenVarint(value: ULong): Int {
    val log2value = 63 - (value or 1uL).toLong().countLeadingZeroBits()
    return (log2value * 9 + (64 + 9)) / 64
}

/**
 * Decodes a LEB128-encoded variable length integer from the buffer.
 */
fun decodeVarint(buf: Buf): Result<ULong> {
    val bytes = buf.chunk()
    val len = bytes.size
    if (len == 0) {
        return Result.failure(DecodeError(DecodeErrorKind.InvalidVarint))
    }

    val byte = bytes[0].toUByte()
    return if (byte < 0x80u) {
        buf.advance(1)
        Result.success(byte.toULong())
    } else if (len > 10 || bytes[len - 1].toUByte() < 0x80u) {
        val sliceResult = decodeVarintSlice(bytes)
        if (sliceResult.isSuccess) {
            val (value, advance) = sliceResult.getOrThrow()
            buf.advance(advance)
            Result.success(value)
        } else {
            Result.failure(sliceResult.exceptionOrNull() ?: DecodeError(DecodeErrorKind.InvalidVarint))
        }
    } else {
        decodeVarintSlow(buf)
    }
}

/**
 * Decodes a LEB128-encoded variable length integer from the slice, returning the value and the
 * number of bytes read.
 */
private fun decodeVarintSlice(bytes: ByteArray): Result<Pair<ULong, Int>> {
    if (bytes.isEmpty()) {
        return Result.failure(DecodeError(DecodeErrorKind.InvalidVarint))
    }
    if (!(bytes.size > 10 || bytes[bytes.size - 1].toUByte() < 0x80u)) {
        return Result.failure(DecodeError(DecodeErrorKind.InvalidVarint))
    }

    var b = bytes[0].toUByte()
    var part0: UInt = b.toUInt()
    if (b < 0x80u) {
        return Result.success(Pair(part0.toULong(), 1))
    }
    part0 -= 0x80u
    b = bytes[1].toUByte()
    part0 += b.toUInt() shl 7
    if (b < 0x80u) {
        return Result.success(Pair(part0.toULong(), 2))
    }
    part0 -= 0x80u shl 7
    b = bytes[2].toUByte()
    part0 += b.toUInt() shl 14
    if (b < 0x80u) {
        return Result.success(Pair(part0.toULong(), 3))
    }
    part0 -= 0x80u shl 14
    b = bytes[3].toUByte()
    part0 += b.toUInt() shl 21
    if (b < 0x80u) {
        return Result.success(Pair(part0.toULong(), 4))
    }
    part0 -= 0x80u shl 21
    var value = part0.toULong()

    b = bytes[4].toUByte()
    var part1: UInt = b.toUInt()
    if (b < 0x80u) {
        return Result.success(Pair(value + (part1.toULong() shl 28), 5))
    }
    part1 -= 0x80u
    b = bytes[5].toUByte()
    part1 += b.toUInt() shl 7
    if (b < 0x80u) {
        return Result.success(Pair(value + (part1.toULong() shl 28), 6))
    }
    part1 -= 0x80u shl 7
    b = bytes[6].toUByte()
    part1 += b.toUInt() shl 14
    if (b < 0x80u) {
        return Result.success(Pair(value + (part1.toULong() shl 28), 7))
    }
    part1 -= 0x80u shl 14
    b = bytes[7].toUByte()
    part1 += b.toUInt() shl 21
    if (b < 0x80u) {
        return Result.success(Pair(value + (part1.toULong() shl 28), 8))
    }
    part1 -= 0x80u shl 21
    value += part1.toULong() shl 28

    b = bytes[8].toUByte()
    var part2: UInt = b.toUInt()
    if (b < 0x80u) {
        return Result.success(Pair(value + (part2.toULong() shl 56), 9))
    }
    part2 -= 0x80u
    b = bytes[9].toUByte()
    part2 += b.toUInt() shl 7
    if (b < 0x02u) {
        return Result.success(Pair(value + (part2.toULong() shl 56), 10))
    }

    return Result.failure(DecodeError(DecodeErrorKind.InvalidVarint))
}

/**
 * Decodes a LEB128-encoded variable length integer from the buffer, advancing the buffer as
 * necessary.
 */
private fun decodeVarintSlow(buf: Buf): Result<ULong> {
    var value = 0uL
    val limit = min(10, buf.remaining())
    for (count in 0 until limit) {
        val byte = buf.getU8()
        value = value or ((byte.toULong() and 0x7FuL) shl (count * 7))
        if (byte <= 0x7Fu) {
            return if (count == 9 && byte >= 0x02u) {
                Result.failure(DecodeError(DecodeErrorKind.InvalidVarint))
            } else {
                Result.success(value)
            }
        }
    }

    return Result.failure(DecodeError(DecodeErrorKind.InvalidVarint))
}
