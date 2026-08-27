// port-lint: source prost/src/message.rs
package io.github.kotlinmania.prost

import io.github.kotlinmania.bytes.BytesMut
import io.github.kotlinmania.bytes.buf.Buf
import io.github.kotlinmania.bytes.buf.BufMut
import io.github.kotlinmania.prost.encoding.DecodeContext
import io.github.kotlinmania.prost.encoding.MessageEncoding
import io.github.kotlinmania.prost.encoding.WireType
import io.github.kotlinmania.prost.encoding.decodeKey
import io.github.kotlinmania.prost.encoding.encodeVarint
import io.github.kotlinmania.prost.encoding.encodedLenVarint

/**
 * A Protocol Buffers message.
 */
interface Message {
    /**
     * Encodes the message to a buffer.
     *
     * Meant to be used only by [Message] implementations.
     */
    fun encodeRaw(buf: BufMut)

    /**
     * Decodes a field from a buffer, and merges it into this instance.
     *
     * Meant to be used only by [Message] implementations.
     */
    fun mergeField(
        tag: UInt,
        wireType: WireType,
        buf: Buf,
        ctx: DecodeContext,
    ): Result<Unit>

    /**
     * Returns the encoded length of the message without a length delimiter.
     */
    fun encodedLen(): Int

    /**
     * Encodes the message to a buffer.
     *
     * An error will be returned if the buffer does not have sufficient capacity.
     */
    fun encode(buf: BufMut): Result<Unit> {
        val required = encodedLen()
        val remaining = buf.remainingMut()
        if (required > remaining) {
            return Result.failure(EncodeError(required, remaining))
        }
        encodeRaw(buf)
        return Result.success(Unit)
    }

    /**
     * Encodes the message to a newly allocated byte array.
     */
    fun encodeToByteArray(): ByteArray {
        val buf = BytesMut.withCapacity(encodedLen())
        encodeRaw(buf)
        return buf.freeze().asSlice()
    }

    /**
     * Encodes the message with a length-delimiter to a buffer.
     *
     * An error will be returned if the buffer does not have sufficient capacity.
     */
    fun encodeLengthDelimited(buf: BufMut): Result<Unit> {
        val len = encodedLen()
        val required = len + encodedLenVarint(len.toULong())
        val remaining = buf.remainingMut()
        if (required > remaining) {
            return Result.failure(EncodeError(required, remaining))
        }
        encodeVarint(len.toULong(), buf)
        encodeRaw(buf)
        return Result.success(Unit)
    }

    /**
     * Encodes the message with a length-delimiter to a newly allocated byte array.
     */
    fun encodeLengthDelimitedToByteArray(): ByteArray {
        val len = encodedLen()
        val required = len + encodedLenVarint(len.toULong())
        val buf = BytesMut.withCapacity(required)
        encodeVarint(len.toULong(), buf)
        encodeRaw(buf)
        return buf.freeze().asSlice()
    }

    /**
     * Decodes an instance of the message from a buffer, merging it into this instance.
     * The entire buffer will be consumed.
     */
    fun merge(buf: Buf): Result<Unit> {
        val ctx = DecodeContext()
        while (buf.hasRemaining()) {
            val keyRes = decodeKey(buf)
            if (keyRes.isFailure) return Result.failure(keyRes.exceptionOrNull()!!)
            val (tag, wireType) = keyRes.getOrThrow()
            val fieldRes = mergeField(tag, wireType, buf, ctx)
            if (fieldRes.isFailure) return fieldRes
        }
        return Result.success(Unit)
    }

    /**
     * Decodes an instance of the message from a byte array, merging it into this instance.
     */
    fun merge(bytes: ByteArray): Result<Unit> = merge(io.github.kotlinmania.bytes.buf.ByteArrayBuf(bytes))

    /**
     * Decodes an instance of the message from [Bytes], merging it into this instance.
     */
    fun merge(bytes: io.github.kotlinmania.bytes.Bytes): Result<Unit> =
        merge(io.github.kotlinmania.bytes.buf.ByteArrayBuf(bytes.asSlice()))

    /**
     * Decodes a length-delimited instance of the message from buffer, and
     * merges it into this instance.
     */
    fun mergeLengthDelimited(buf: Buf): Result<Unit> =
        MessageEncoding.merge(WireType.LengthDelimited, this, buf, DecodeContext())

    /**
     * Decodes a length-delimited instance of the message from a byte array.
     */
    fun mergeLengthDelimited(bytes: ByteArray): Result<Unit> =
        mergeLengthDelimited(io.github.kotlinmania.bytes.buf.ByteArrayBuf(bytes))

    /**
     * Decodes a length-delimited instance of the message from [Bytes].
     */
    fun mergeLengthDelimited(bytes: io.github.kotlinmania.bytes.Bytes): Result<Unit> =
        mergeLengthDelimited(io.github.kotlinmania.bytes.buf.ByteArrayBuf(bytes.asSlice()))

    /**
     * Clears the message, resetting all fields to their default.
     */
    fun clear()
}
