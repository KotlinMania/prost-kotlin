// port-lint: tests error.rs
package io.github.kotlinmania.prost

import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorTest {
    @Test
    fun testPush() {
        val decodeError = DecodeError(DecodeErrorKind.InvalidVarint)
        decodeError.push("Foo bad", "bar.foo")
        decodeError.push("Baz bad", "bar.baz")

        assertEquals(
            "failed to decode Protobuf message: Foo bad.bar.foo: Baz bad.bar.baz: invalid varint",
            decodeError.message,
        )
    }

    @Test
    fun testFactories() {
        @Suppress("DEPRECATION")
        val decodeErr = DecodeError.new("something went wrong")
        assertEquals("failed to decode Protobuf message: something went wrong", decodeErr.message)

        val encodeErr = EncodeError.new(10, 5)
        assertEquals(10, encodeErr.requiredCapacity())
        assertEquals(5, encodeErr.remaining())
        assertEquals(
            "failed to encode Protobuf message; insufficient buffer capacity (required: 10, remaining: 5)",
            encodeErr.message,
        )
    }

    // testIntoStdIoError unported: std::io::Error conversion is specific to Rust std library and does not apply to Kotlin Multiplatform common errors.
}
