// port-lint: source prost/src/encoding/length_delimiter.rs
package io.github.kotlinmania.prost.encoding

import io.github.kotlinmania.bytes.buf.Buf
import io.github.kotlinmania.bytes.buf.BufMut
import io.github.kotlinmania.prost.DecodeError
import io.github.kotlinmania.prost.DecodeErrorKind
import io.github.kotlinmania.prost.EncodeError

/**
 * Encodes a length delimiter to the buffer.
 *
 * An error will be returned if the buffer does not have sufficient capacity to encode the delimiter.
 */
fun encodeLengthDelimiter(length: Int, buf: BufMut): Result<Unit> {
    val lengthULong = length.toULong()
    val required = encodedLenVarint(lengthULong)
    val remaining = buf.remainingMut()
    if (required > remaining) {
        return Result.failure(EncodeError(required, remaining))
    }
    encodeVarint(lengthULong, buf)
    return Result.success(Unit)
}

/**
 * Returns the encoded length of a length delimiter.
 *
 * Applications may use this method to ensure sufficient buffer capacity before calling
 * [encodeLengthDelimiter]. The returned size will be between 1 and 10, inclusive.
 */
fun lengthDelimiterLen(length: Int): Int = encodedLenVarint(length.toULong())

/**
 * Decodes a length delimiter from the buffer.
 */
fun decodeLengthDelimiter(buf: Buf): Result<Int> {
    val lengthResult = decodeVarint(buf)
    if (lengthResult.isFailure) {
        return Result.failure(lengthResult.exceptionOrNull() ?: DecodeError(DecodeErrorKind.InvalidVarint))
    }
    val length = lengthResult.getOrThrow()
    if (length > Int.MAX_VALUE.toULong()) {
        return Result.failure(DecodeError(DecodeErrorKind.LengthDelimiterTooLarge))
    }
    return Result.success(length.toInt())
}
