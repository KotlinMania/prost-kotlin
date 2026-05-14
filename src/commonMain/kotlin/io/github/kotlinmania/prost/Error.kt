// port-lint: source src/error.rs
package io.github.kotlinmania.prost

import io.github.kotlinmania.prost.encoding.WireType

/**
 * Protobuf encoding and decoding errors.
 */

/**
 * A Protobuf message decoding error.
 *
 * [DecodeError] indicates that the input buffer does not contain a valid
 * Protobuf message. The error details should be considered 'best effort': in
 * general it is not possible to exactly pinpoint why data is malformed.
 */
class DecodeError : Exception {

    internal val description: DecodeErrorKind
    private val stack: MutableList<Pair<String, String>>

    internal constructor(description: DecodeErrorKind) {
        this.description = description
        this.stack = mutableListOf()
    }

    /**
     * Creates a new [DecodeError] with a 'best effort' root cause description.
     *
     * Meant to be used only by [Message] implementations.
     */
    @Deprecated(
        "This function was meant for internal use only. Because of internal-only documentation it " +
            "was publicly available and it is actually used by users. The prost project intents to " +
            "remove this function in the next breaking release.",
    )
    constructor(description: String) {
        this.description = DecodeErrorKind.Other(description = description)
        this.stack = mutableListOf()
    }

    /**
     * Pushes a (message, field) name location pair on to the location stack.
     *
     * Meant to be used only by [Message] implementations.
     */
    fun push(message: String, field: String) {
        stack.add(message to field)
    }

    override val message: String
        get() = buildString {
            append("failed to decode Protobuf message: ")
            for ((messageName, fieldName) in stack) {
                append(messageName)
                append('.')
                append(fieldName)
                append(": ")
            }
            append(description.toString())
        }

    override fun toString(): String =
        "DecodeError(description=$description, stack=$stack)"

    override fun equals(other: Any?): Boolean =
        other is DecodeError && description == other.description && stack == other.stack

    override fun hashCode(): Int = 31 * description.hashCode() + stack.hashCode()

    companion object {
        /**
         * Creates a new [DecodeError] with a [DecodeErrorKind.UnexpectedTypeUrl].
         *
         * Must only be used by `prost_types::Any` implementation.
         */
        fun newUnexpectedTypeUrl(actual: String, expected: String): DecodeError =
            DecodeError(DecodeErrorKind.UnexpectedTypeUrl(actual = actual, expected = expected))
    }
}

internal sealed class DecodeErrorKind {
    /** Length delimiter exceeds maximum usize value */
    data object LengthDelimiterTooLarge : DecodeErrorKind() {
        override fun toString(): String = "length delimiter exceeds maximum usize value"
    }

    /** Invalid varint */
    data object InvalidVarint : DecodeErrorKind() {
        override fun toString(): String = "invalid varint"
    }

    /** Recursion limit reached */
    data object RecursionLimitReached : DecodeErrorKind() {
        override fun toString(): String = "recursion limit reached"
    }

    /** Invalid wire type value */
    data class InvalidWireType(val value: ULong) : DecodeErrorKind() {
        override fun toString(): String = "invalid wire type value: $value"
    }

    /** Invalid key value */
    data class InvalidKey(val key: ULong) : DecodeErrorKind() {
        override fun toString(): String = "invalid key value: $key"
    }

    /** Invalid tag value: 0 */
    data object InvalidTag : DecodeErrorKind() {
        override fun toString(): String = "invalid tag value: 0"
    }

    /** Invalid wire type */
    data class UnexpectedWireType(
        val actual: WireType,
        val expected: WireType,
    ) : DecodeErrorKind() {
        override fun toString(): String = "invalid wire type: $actual (expected $expected)"
    }

    /** Buffer underflow */
    data object BufferUnderflow : DecodeErrorKind() {
        override fun toString(): String = "buffer underflow"
    }

    /** Delimited length exceeded */
    data object DelimitedLengthExceeded : DecodeErrorKind() {
        override fun toString(): String = "delimited length exceeded"
    }

    /** Unexpected end group tag */
    data object UnexpectedEndGroupTag : DecodeErrorKind() {
        override fun toString(): String = "unexpected end group tag"
    }

    /** Invalid string value: data is not UTF-8 encoded */
    data object InvalidString : DecodeErrorKind() {
        override fun toString(): String = "invalid string value: data is not UTF-8 encoded"
    }

    /** Unexpected type URL */
    data class UnexpectedTypeUrl(val actual: String, val expected: String) : DecodeErrorKind() {
        override fun toString(): String =
            "unexpected type URL.type_url: expected type URL: \"$expected\" (got: \"$actual\")"
    }

    /** A textual description of a problem */
    data class Other(val description: String) : DecodeErrorKind() {
        override fun toString(): String = description
    }
}

/**
 * A Protobuf message encoding error.
 *
 * [EncodeError] always indicates that a message failed to encode because the
 * provided buffer had insufficient capacity. Message encoding is otherwise
 * infallible.
 */
class EncodeError internal constructor(
    private val required: Int,
    private val remaining: Int,
) : Exception() {

    /** Returns the required buffer capacity to encode the message. */
    fun requiredCapacity(): Int = required

    /** Returns the remaining length in the provided buffer at the time of encoding. */
    fun remaining(): Int = remaining

    override val message: String
        get() = "failed to encode Protobuf message; insufficient buffer capacity " +
            "(required: $required, remaining: $remaining)"

    override fun toString(): String = "EncodeError(required=$required, remaining=$remaining)"

    override fun equals(other: Any?): Boolean =
        other is EncodeError && required == other.required && remaining == other.remaining

    override fun hashCode(): Int = 31 * required + remaining
}

/**
 * An error indicating that an unknown enumeration value was encountered.
 *
 * The Protobuf spec mandates that enumeration value sets are 'open', so this
 * error's value represents an integer value unrecognized by the
 * presently used enum definition.
 */
data class UnknownEnumValue(val value: Int) : Exception() {

    override val message: String
        get() = "unknown enumeration value $value"
}
