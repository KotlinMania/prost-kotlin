// port-lint: source src/error.rs
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
}
