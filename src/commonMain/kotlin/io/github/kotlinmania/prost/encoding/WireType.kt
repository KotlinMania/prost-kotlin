// port-lint: source src/encoding/wire_type.rs
package io.github.kotlinmania.prost.encoding

import io.github.kotlinmania.prost.DecodeError
import io.github.kotlinmania.prost.DecodeErrorKind

/**
 * Represent the wire type for protobuf encoding.
 *
 * The integer value is equvilant with the encoded value.
 */
enum class WireType(val code: UByte) {
    Varint(0u),
    SixtyFourBit(1u),
    LengthDelimited(2u),
    StartGroup(3u),
    EndGroup(4u),
    ThirtyTwoBit(5u),
    ;

    companion object {
        fun tryFrom(value: ULong): Result<WireType> = when (value) {
            0uL -> Result.success(Varint)
            1uL -> Result.success(SixtyFourBit)
            2uL -> Result.success(LengthDelimited)
            3uL -> Result.success(StartGroup)
            4uL -> Result.success(EndGroup)
            5uL -> Result.success(ThirtyTwoBit)
            else -> Result.failure(DecodeError(DecodeErrorKind.InvalidWireType(value = value)))
        }
    }
}

/**
 * Checks that the expected wire type matches the actual wire type,
 * or returns an error result.
 */
fun checkWireType(expected: WireType, actual: WireType): Result<Unit> =
    if (expected != actual) {
        Result.failure(
            DecodeError(DecodeErrorKind.UnexpectedWireType(actual = actual, expected = expected)),
        )
    } else {
        Result.success(Unit)
    }
